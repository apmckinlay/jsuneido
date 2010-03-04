package suneido.database;

import static suneido.database.Database.theDB;

import java.nio.ByteBuffer;
import java.text.SimpleDateFormat;
import java.util.Date;

import suneido.database.Database.TN;
import suneido.language.Pack;
import suneido.util.ByteBuf;

/**
 * check the consistency of a database
 * e.g. after finding it was not shutdown properly
 *
 * @author Andrew McKinlay
 */
public class DbCheck {
	public enum Status {
		OK, CORRUPTED, UNRECOVERABLE
	};
	String filename;
	Mmfile mmf;
	long last_good_commit = 0; // offset
	String details = "";

	public DbCheck(String filename) {
		this.filename = filename;
		mmf = new Mmfile(filename, Mode.READ_ONLY);
	}

	public static void checkPrintExit(String filename) {
		System.out.println("Checking " + filename);
		DbCheck dbck = new DbCheck(filename);
		Status status = dbck.checkPrint();
		System.exit(status == Status.OK ? 0 : -1);
	}

	public Status checkPrint() {
		System.out.println("Checking commits and shutdowns...");
		Status status = check_commits_and_shutdowns();
		if (status == Status.OK) {
			System.out.println("Checking data and indexes...");
			if (!check_data_and_indexes())
				status = Status.CORRUPTED;
		}
		Date d = new Date(last_good_commit);
		if (status != Status.UNRECOVERABLE)
			System.out.println("Last "
					+ (status == Status.CORRUPTED ? "good " : "")
					+ "commit "
					+ new SimpleDateFormat("yyyy-MM-dd HH:mm").format(d));
		System.out.println("Database " + status + " " + details);
		return status;
	}

	private Status check_commits_and_shutdowns() {
		if (mmf.first() == 0) {
			details = "no data";
			return Status.UNRECOVERABLE;
		}
		boolean ok = false;
		boolean has_a_shutdown = false;
		Checksum cksum = new Checksum();
		Mmfile.MmfileIterator iter = mmf.iterator();
		loop: while (iter.hasNext()) {
			ok = false;
			ByteBuf buf = iter.next();
			switch (iter.type()) {
			case Mmfile.DATA:
				int tblnum = buf.getInt(0);
				if (tblnum != TN.TABLES && tblnum != TN.INDEXES) {
					Record r = new Record(buf.slice(4));
					cksum.add(buf.getByteBuffer(), r.bufSize() + 4);
					// + 4 to skip tblnum
				}
				break;
			case Mmfile.COMMIT:
				Commit commit = new Commit(buf);
				cksum.add(buf.getByteBuffer(), commit.sizeWithoutChecksum());
				if (commit.getChecksum() != (int) cksum.getValue()) {
					details += "checksum mismatch. ";
					break loop;
				}
				last_good_commit = commit.getDate();
				cksum.reset();
				process_deletes(commit);
				break;
			case Mmfile.SESSION:
				if (buf.get(0) == Session.SHUTDOWN) {
					has_a_shutdown = true;
					ok = true;
				}
				break;
			case Mmfile.OTHER:
				// ignore
				break;
			default:
				details += "invalid block type. ";
				break loop;
			}
		}
		if (! has_a_shutdown) {
			details += "no valid shutdowns. ";
			return Status.UNRECOVERABLE;
		}
		if (last_good_commit == 0) {
			details += "no valid commits. ";
			return Status.UNRECOVERABLE;
		}
		if (iter.corrupt()) {
			details += "iteration failed. ";
			return Status.CORRUPTED;
		}
		if (!ok) {
			details += "missing last shutdown. ";
			return Status.CORRUPTED;
		}
		return Status.OK;
	}

	protected void process_deletes(Commit commit) {
		// empty stub, overridden by DbRebuild
	}

	private final static int BAD_LIMIT = 10;

	private boolean check_data_and_indexes() {
		Database.theDB = new Database(filename, Mode.READ_ONLY);
		Transaction t = theDB.readonlyTran();
		try {
			BtreeIndex bti = t.getBtreeIndex(Database.TN.TABLES, "tablename");
			BtreeIndex.Iter iter = bti.iter(t).next();
			int i = 0;
			int nbad = 0;
			for (; !iter.eof(); iter.next()) {
				System.out.print(".");
				if (++i % 80 == 0)
					System.out.println();
				Record r = t.input(iter.keyadr());
				String tablename = r.getString(Table.TABLE);
				if (! checkTable(t, tablename)) {
					if (++nbad > BAD_LIMIT)
						break;
				}
			}
			return nbad == 0;
		} finally {
			System.out.println();
			t.complete();
			Database.theDB.close();
			Database.theDB = null;
		}
	}

	private boolean checkTable(Transaction t, String tablename) {
		boolean first_index = true;
		Table table = t.getTable(tablename);
		TableData td = t.getTableData(table.num);
		int maxfields = 0;
		for (Index index : table.indexes) {
			int nrecords = 0;
			long totalsize = 0;
			BtreeIndex bti = t.getBtreeIndex(index);
			BtreeIndex.Iter iter = bti.iter(t);
			for (iter.next(); !iter.eof(); iter.next()) {
				Record key = iter.cur().key;
				Record rec = theDB.input(iter.keyadr());
				if (first_index)
					if (!checkRecord(tablename, rec))
						return false;
				Record reckey = rec.project(index.colnums, iter.keyadr());
				if (!key.equals(reckey)) {
					details += tablename + ": index key mismatch. ";
					return false;
				}
				for (Index index2 : table.indexes) {
					Record key2 = rec.project(index2.colnums, iter.keyadr());
					BtreeIndex bti2 = t.getBtreeIndex(index2);
					Slot slot = bti2.find(t, key2);
					if (slot == null) {
						details += tablename + ": incomplete index. ";
						return false;
					}
				}
				++nrecords;
				totalsize += rec.packSize();
				if (rec.size() > maxfields)
					maxfields = rec.size();
			}
			if (nrecords != td.nrecords) {
				details += tablename + ": record count mismatch: index "
						+ nrecords + " != tables " + td.nrecords + ". ";
				return false;
			}
			if (totalsize != td.totalsize) {
				details += tablename + ": table size mismatch: data "
						+ totalsize + " != tables " + td.totalsize + ". ";
				return false;
			}
		}
		if (td.nextfield <= table.maxColumnNum()) {
			details += tablename + ": nextfield mismatch: nextfield "
					+ td.nextfield + " <= max column# " + table.maxColumnNum() + ". ";
			return false;
		}
		if (maxfields > td.nextfield) {
			details += tablename + ": nextfield mismatch: maxfields "
					+ maxfields + " > nextfield " + td.nextfield + ". ";
			return false;
		}
		nextfield(table.num, Math.max(maxfields, table.maxColumnNum() + 1));
		return true;
	}

	// overridden by DbRebuild
	protected void nextfield(int tblnum, int n) {
	}

	private boolean checkRecord(String tablename, Record rec) {
		for (ByteBuffer buf : rec)
			try {
				Pack.unpack(buf);
			} catch (Throwable e) {
				details += tablename + ": " + e + ". ";
				return false;
			}
		return true;
	}

	public static void main(String[] args) {
//		Database db = new Database("check.db", Mode.CREATE);
//		db.addTable("test");
//		db.addColumn("test", "a");
//		db.addColumn("test", "b");
//		db.addIndex("test", "b", true);
//		Record r = new Record().add(12).add(34);
//		Transaction t = db.readwriteTran();
//		t.addRecord("test", r);
//		t.ck_complete();
//		db.close();
		checkPrintExit("suneido.db");
	}

}
