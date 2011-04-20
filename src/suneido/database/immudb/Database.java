/* Copyright 2011 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido.database.immudb;

import java.nio.ByteBuffer;

import suneido.database.immudb.Bootstrap.TN;
import suneido.database.immudb.schema.Index;

public class Database {
	static final int INT_SIZE = 4;
	public final Storage stor;

	public Database(Storage stor) {
		this.stor = stor;
	}

	public void open() {
		Check check = new Check(stor);
		if (false == check.fastcheck())
			throw new RuntimeException("database open check failed");
		ByteBuffer buf = stor.buffer(-(Tran.TAIL_SIZE + 2 * INT_SIZE));
		int root = buf.getInt();
		int redirs = buf.getInt();
System.out.println("open root " + root + " redirs " + redirs);
		loadSchema(root, redirs);
	}

	private void loadSchema(int root, int redirs) {
		Tran tran = new Tran(stor, redirs);
		Record r = new Record(stor.buffer(root));
System.out.println(r);
		BtreeInfo info = Index.btreeInfo(r);
		Btree indexesIndex = new Btree(tran, info);

		int adr = indexesIndex.get(key(TN.COLUMNS, "table,column"));
		r = new Record(stor.buffer(adr));
System.out.println(r);
		info = Index.btreeInfo(r);
		Btree columnsIndex = new Btree(tran, info);

		adr = indexesIndex.get(key(TN.TABLES, "table"));
		r = new Record(stor.buffer(adr));
System.out.println(r);
		info = Index.btreeInfo(r);
		Btree tablesIndex = new Btree(tran, info);

		Btree.Iter iter;

		iter = columnsIndex.iterator();
		for (iter.next(); ! iter.eof(); iter.next()) {
			Record key = iter.cur();
			adr = Btree.getAddress(key);
			r = new Record(stor.buffer(adr));
System.out.println("column " + r);
		}

		iter = indexesIndex.iterator();
		for (iter.next(); ! iter.eof(); iter.next()) {
			Record key = iter.cur();
			adr = Btree.getAddress(key);
			r = new Record(stor.buffer(adr));
System.out.println("index " + r);
		}

		iter = tablesIndex.iterator();
		for (iter.next(); ! iter.eof(); iter.next()) {
			Record key = iter.cur();
			adr = Btree.getAddress(key);
			r = new Record(stor.buffer(adr));
System.out.println("table " + r);
		}
	}

	private Record key(Object... values) {
		RecordBuilder rb = new RecordBuilder();
		for (Object x : values)
			rb.add(x);
		return rb.build();
	}

}
