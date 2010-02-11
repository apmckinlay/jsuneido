package suneido.database;

import static suneido.database.Database.theDB;

import java.nio.ByteBuffer;

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
	Mmfile mmf;
	long last_good_commit = 0; // offset
	String details = "";

	public static void checkPrintExit(String filename) {
		DbCheck dbck = new DbCheck(filename);
		System.out.println("Checking commits and shutdowns...");
		Status status = dbck.check_commits_and_shutdowns();
		if (status == Status.OK) {
			System.out.println("Checking data and indexes...");
			if (!dbck.check_data_and_indexes())
				status = Status.CORRUPTED;
		}
		System.out.println("database " + status + " " + dbck.details);
		System.exit(status == Status.OK ? 0 : -1);
	}

	private DbCheck(String filename) {
		mmf = new Mmfile(filename, Mode.OPEN);
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
					// + 4 to skip tblnum
					Record r = new Record(buf.slice(4), iter.offset() + 4);
					cksum.add(buf.getByteBuffer(), r.bufSize() + 4);
				}
				break;
			case Mmfile.COMMIT:
				int ncreates = buf.getInt(12);
				int ndeletes = buf.getInt(16);
				int pos = 20 + (ncreates + ndeletes) * 4;

				cksum.add(buf.getByteBuffer(), pos);

				int commit_cksum = buf.getInt(pos);
				if (commit_cksum != (int) cksum.getValue()) {
					details += "checksum mismatch. ";
					break loop;
					}
				last_good_commit = iter.offset();
				cksum.reset();
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

	private final static int BAD_LIMIT = 10;

	private boolean check_data_and_indexes() {
		Database.open_theDB();
		Transaction t = theDB.readonlyTran();
		try {
			BtreeIndex bti = t.getBtreeIndex(Database.TN.TABLES, "tablename");
			BtreeIndex.Iter iter = bti.iter(t).next();
			int nbad = 0;
			for (; !iter.eof(); iter.next()) {
				Record r = t.input(iter.keyadr());
				String tablename = r.getString(Table.TABLE);
				if (! check_data_and_indexes(t, tablename)) {
					if (++nbad > BAD_LIMIT)
						break;
				}
			}
			return nbad == 0;
		} finally {
			t.complete();
		}
	}

	private boolean check_data_and_indexes(Transaction t, String tablename) {
		int nbad = 0;
		boolean first_index = true;
		Table table = t.getTable(tablename);
		for (Index index : table.indexes) {
			BtreeIndex bti = t.getBtreeIndex(index);
			BtreeIndex.Iter iter = bti.iter(t);
			for (iter.next(); !iter.eof(); iter.next()) {
				Record key = iter.cur().key;
				Record rec = theDB.input(iter.keyadr());
				if (first_index)
					if (!check_record(tablename, rec))
						if (++nbad > BAD_LIMIT)
							return false;
				Record reckey = rec.project(index.colnums, iter.keyadr());
				if (!key.equals(reckey))
					if (++nbad > BAD_LIMIT)
						return false;
					else
						details += tablename + ": index mismatch. ";

				for (Index index2 : table.indexes) {
					Record key2 = rec.project(index2.colnums, iter.keyadr());
					BtreeIndex bti2 = t.getBtreeIndex(index2);
					Slot slot = bti2.find(t, key2);
					if (slot == null)
						if (++nbad > BAD_LIMIT)
							return false;
						else
							details += tablename + ": incomplete index. ";
				}
			}
		}
		return nbad == 0;
	}

	private boolean check_record(String tablename, Record rec) {
		for (ByteBuffer buf : rec)
			try {
				Pack.unpack(buf);
			} catch (Throwable e) {
				details += tablename + ": " + e + ". ";
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
