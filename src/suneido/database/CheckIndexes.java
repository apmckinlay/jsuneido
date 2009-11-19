package suneido.database;

import static suneido.database.Database.theDB;

public class CheckIndexes {

	public static void checkIndexes(String tablename) {
		Transaction t = theDB.readonlyTran();
		try {
			Table table = t.getTable(tablename);
			for (Index index : table.indexes) {
				BtreeIndex bti = t.getBtreeIndex(index);
				BtreeIndex.Iter iter = bti.iter(t);
				for (iter.next(); !iter.eof(); iter.next()) {
					Record key = iter.cur().key;
					Record rec = theDB.input(iter.keyadr());
					Record reckey = rec.project(index.colnums, iter.keyadr());
if (!key.equals(reckey))
System.out.println("ERROR " + key + " != " + reckey);
					//verify(key.equals(reckey));

					for (Index index2 : table.indexes) {
						Record key2 = rec.project(index2.colnums, iter.keyadr());
						BtreeIndex bti2 = t.getBtreeIndex(index2);
						Slot slot = bti2.find(t, key2);
if (slot == null) {
System.out.println("ERROR from " + index + " got " + key + " " + rec);
System.out.println("    failed to find " + key2 + " in " + index2);
}
						//verify(slot != null);
					}
				}
			}
		} finally {
			t.ck_complete();
		}
System.out.println("CheckIndexes finished");
	}

}
