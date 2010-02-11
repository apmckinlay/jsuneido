package suneido.database;

import suneido.database.Database.TN;
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
		Status status = dbck.check();
		System.out.println("database " + status + " " + dbck.details);
		System.exit(status == Status.OK ? 0 : -1);
	}

	private DbCheck(String filename) {
		mmf = new Mmfile(filename, Mode.OPEN);
	}

	private Status check() {
		if (mmf.first() == 0)
			return Status.UNRECOVERABLE;
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
				Record r = new Record(buf.slice(4), iter.offset() + 4); // skip tblnum
				if (tblnum != TN.TABLES && tblnum != TN.INDEXES)
					cksum.add(buf.getByteBuffer(), r.bufSize() + 4);
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
