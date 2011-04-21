/* Copyright 2011 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido.database.immudb;

import java.nio.ByteBuffer;
import java.util.List;
import java.util.Map;

import suneido.database.immudb.schema.*;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

public class Transaction {
	private final Storage stor;
	private final Tran tran;
	private final Tables schema;
	private final Map<String,Btree> indexes = Maps.newHashMap();
	private final List<NewBtree> newBtrees = Lists.newArrayList();

	public Transaction(Storage stor, Tables schema) {
		this.stor = stor;
		tran = new Tran(stor);
		this.schema = schema;
	}

	public Btree getIndex(String table) {
		Btree btree = indexes.get(table);
		if (btree != null)
			return btree;
		Table tbl = schema.get(table);
		Index index = tbl.firstIndex();
		btree = new Btree(tran, index.btreeInfo);
		indexes.put(table, btree);
		return btree;
	}

	public int addTable(String name) {
		Btree btree = getIndex("tables");
		IndexedData id = new IndexedData().index(btree, 0);
		int tblnum = 4; // TODO next table num
		Record r = Table.toRecord(tblnum, name, 0, 0, 0);
		id.add(tran, r);
		return tblnum;
	}

	public void addColumn(int tblnum, String column) {
		Btree btree = getIndex("columns");
		IndexedData id = new IndexedData().index(btree, 0, 2);
		int colnum = 0; // TODO next column num
		Record r = Column.toRecord(tblnum, column, colnum);
		id.add(tran, r);
	}

	private static ForeignKey noFkey = new ForeignKey("", "", 0);

	public void addIndex(int tblnum, String columns, boolean key,
			boolean unique, String fktable, String fkcolumns, int fkmode) {
		Btree btree = getIndex("indexes");
		IndexedData id = new IndexedData().index(btree, 0, 1);
		Btree index = new Btree(tran);
		Record r = Index.toRecord(tblnum, columns, key, unique, noFkey,
				index.info());
		int intref = id.add(tran, r);
		newBtrees.add(new NewBtree(index, intref));
	}

	private static class NewBtree {
		Btree btree;
		int intref;

		NewBtree(Btree btree, int intref) {
			this.btree = btree;
			this.intref = intref;
		}
	}

	// TODO synchronize
	public void commit() {
		tran.startStore();
		storeData();
		Btree.store(tran);
		updateNewBtrees();
		// TODO update nrows, totalsize
		updateBtreeInfo();
		int redirs = tran.storeRedirs();
		store(indexesAdr(), redirs);
		tran.endStore();
	}

	// TODO remove duplication with Bootstrap
	private void storeData() {
		IntRefs intrefs = tran.context.intrefs;
		int i = -1;
		for (Object x : intrefs) {
			++i;
			if (x instanceof Record) {
				Record r = (Record) x;
				int adr = r.store(tran.context.stor);
				int intref = i | IntRefs.MASK;
				tran.setAdr(intref, adr);
			}
		}
	}

	private void updateNewBtrees() {
		for (NewBtree nb : newBtrees) {
			Record oldrec = (Record) tran.intToRef(nb.intref);
			Record newrec = Index.updateRecord(oldrec, nb.btree.info());
			int newadr = newrec.store(tran.context.stor);
			int oldadr = tran.getAdr(nb.intref);
			tran.redirs().put(oldadr, newadr);
		}
	}

	private void updateBtreeInfo() {
		for (Map.Entry<String, Btree> entry : indexes.entrySet())
			updateBtreeInfo(entry.getKey(), entry.getValue());
	}

	private void updateBtreeInfo(String table, Btree btree) {
		Table tbl = schema.get(table);
		Index index = tbl.firstIndex();
		if (! btree.info().equals(index.btreeInfo)) {
			int oldadr = indexesLookup(tbl.num, index.columns);
			Record oldrec = tran.getrec(oldadr);
			Record newrec = Index.updateRecord(oldrec, btree.info());
System.out.println("oldrec " + oldrec);
System.out.println("newrec " + newrec);
			int newadr = newrec.store(tran.context.stor);
System.out.println("redirect " + oldadr + " to " + newadr);
			tran.redirs().put(oldadr, newadr);
		}
	}

	public int indexesLookup(int tblnum, String indexColumns) {
		Btree indexes = getIndex("indexes");
		Record key = new RecordBuilder().add(tblnum).add(indexColumns).build();
		return indexes.get(key);
	}

	private int indexesAdr() {
		return indexesLookup(Bootstrap.TN.INDEXES, "table,columns");
	}

	static final int INT_SIZE = 4;

	// TODO remove duplication with Bootstrap
	private void store(int indexes, int redirs) {
		indexes = tran.redir(indexes);
System.out.println("indexes " + indexes);
		ByteBuffer buf = stor.buffer(stor.alloc(2 * INT_SIZE));
		buf.putInt(indexes);
		buf.putInt(redirs);
	}

}
