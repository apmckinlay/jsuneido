package suneido.database;

import java.io.FileNotFoundException;
import java.io.PrintStream;

import suneido.database.Database.TN;
import suneido.util.ByteBuf;

public class DbDebug {
	Mmfile mmf;

	protected DbDebug(String filename) {
		mmf = new Mmfile(filename, Mode.OPEN);
	}

	public static void dump(String filename) {
		new DbDebug(filename).dump();
	}

	private void dump() {
		Mmfile.MmfileIterator iter = mmf.iterator();
		while (iter.hasNext()) {
			ByteBuf buf = iter.next();
			System.out.print(iter.offset() + " " + iter.length() + " ");
			if (iter.offset() == mmf.first()) {
				int next_table = buf.getInt(0);
				long indexes = Mmfile.intToOffset(buf.getInt(4));
				int version = buf.getInt(8);
				System.out.println("DBHDR next table " + next_table
						+ ", indexes " + indexes + ", version " + version);
				continue;
			}
			switch (iter.type()) {
			case Mmfile.DATA:
				int tblnum = buf.getInt(0);
				System.out.print("data");
				if (tblnum > TN.INDEXES)
					System.out.println(" for table " + tblnum);
				else {
					Record rec = new Record(buf.slice(4));
					int tn = rec.getInt(0);
					switch (tblnum) {
					case TN.TABLES:
						System.out.println(" TABLE " + tn + " " + rec.getString(Table.TABLE));
						break;
					case TN.COLUMNS:
						System.out.println(" COLUMN " + tn + " " + rec.getString(Column.COLUMN));
						break;
					case TN.INDEXES:
						System.out.println(" INDEX " + tn + " " + rec.getString(Index.COLUMNS));
						break;
					}
				}
				break;
			case Mmfile.COMMIT:
				System.out.println(new Commit(buf).toStringVerbose());
				break;
			case Mmfile.SESSION:
				System.out.println(new Session(buf));
				break;
			case Mmfile.OTHER:
				System.out.println("other");
				break;
			default:
				System.out.println("unknown type: " + iter.type());
			}
		}
		if (iter.corrupt())
			System.out.println("CORRUPT");
	}

	public static void main(String[] args) throws FileNotFoundException {
		System.setOut(new PrintStream("dbdump.txt"));
		dump("rebuild.db");
	}

}
