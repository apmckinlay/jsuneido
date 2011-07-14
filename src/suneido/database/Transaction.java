/* Copyright 2008 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido.database;

import static suneido.SuException.verify;

import java.nio.ByteBuffer;
import java.util.*;

import javax.annotation.concurrent.ThreadSafe;

import suneido.SuException;
import suneido.database.server.ServerData;
import suneido.util.ByteBuf;
import suneido.util.PersistentMap;

/**
 * Handles a single readonly or readwrite transaction.
 * Mostly thread-contained, but needs to be threadsafe
 * so that inactive or excessive transactions can be aborted by other threads.
 * Uses {@link Transactions} and {@link Shadows}
 * equals and hashCode are the default i.e. identity
 */
@ThreadSafe
class Transaction implements suneido.Transaction, Comparable<Transaction> {
	final Database db;
	private final Transactions trans;
	private final boolean readonly;
	protected volatile boolean ended = false;
	private volatile boolean inConflict = false;
	private volatile boolean outConflict = false;
	private String conflict = null;
	final long start = new Date().getTime();
	private final long asof;
	final int num;
	private volatile long commitTime = Long.MAX_VALUE;
	final String sessionId = ServerData.forThread().getSessionId();
	// these are final except for being cleared when transaction ends
	private /*final*/ Tables tables;
	private /*final*/  PersistentMap<Integer, TableData> tabledata;
	private /*final*/  Map<Integer, TableData> tabledataUpdates =
			new HashMap<Integer, TableData>();
	/*final*/  PersistentMap<String, BtreeIndex> btreeIndexes;
	private /*final*/  Map<String, BtreeIndex> btreeIndexCopies =
			new HashMap<String, BtreeIndex>();
	private List<Table> update_tables = null;
	private Table remove_table = null;
	private /*final*/  Deque<TranWrite> writes = new ArrayDeque<TranWrite>();
	static final Transaction NULLTRAN = new NullTransaction();
	private static final int MAX_WRITES_PER_TRANSACTION = 5000;
	private /*final*/  Shadows shadows = new Shadows();
	private volatile int shadowSizeAtLastActivity = 0;

	Transaction(Database db, Transactions trans, boolean readonly,
			Tables tables,
			PersistentMap<Integer, TableData> tabledata, PersistentMap<String, BtreeIndex> btreeIndexes) {
		this.db = db;
		this.trans = trans;
		this.readonly = readonly;
		asof = trans.clock();
		num = trans.nextNum(readonly);
		this.tables = tables;
		this.tabledata = tabledata;
		this.btreeIndexes = btreeIndexes;
		trans.add(this);
	}

	// used by NullTransaction
	Transaction() {
		this.db = null;
		this.trans = null;
		this.tables = null;
		this.tabledata = null;
		this.btreeIndexes = null;
		readonly = false;
		asof = num = -1;
	}

	@Override
	public String toString() {
		String sid = "";
		if (! "127.0.0.1".equals(sessionId))
			sid = "(" + sessionId + ")";
		return "T" + (readonly ? "r" : "w") + num + sid;
	}

	@Override
	public boolean isReadonly() {
		return readonly;
	}

	@Override
	public boolean isReadWrite() {
		return !readonly;
	}

	@Override
	public boolean isEnded() {
		return ended;
	}

	@Override
	public long asof() {
		return asof;
	}

	@Override
	public synchronized String conflict() {
		return conflict;
	}

	@Override
	public boolean tableExists(String table) {
		return getTable(table) != null;
	}

	@Override
	public Table ck_getTable(String tablename) {
		Table tbl = getTable(tablename);
		if (tbl == null)
			throw new SuException("nonexistent table: " + tablename);
		return tbl;
	}

	@Override
	public Table getTable(String tablename) {
		if (tablename == null)
			return null;
		notEnded();
		return tables.get(tablename);
	}

	@Override
	public Table ck_getTable(int tblnum) {
		Table tbl = getTable(tblnum);
		if (tbl == null)
			throw new SuException("nonexistent table: " + tblnum);
		return tbl;
	}

	@Override
	public Table getTable(int tblnum) {
		notEnded();
		return tables.get(tblnum);
	}

	// used by Schema
	void updateTable(int tblnum) {
		notEnded();
		updateTable(tblnum, null);
	}

	synchronized void updateTable(int tblnum, List<BtreeIndex> btis) {
		Table table = getUpdatedTable(tblnum, btis);
		assert remove_table == null;
		if (update_tables == null)
			update_tables = new ArrayList<Table>();
		update_tables.add(table);
	}

	private Table getUpdatedTable(int tblnum, List<BtreeIndex> btis) {
		Record table_rec = db.getTableRecord(this, tblnum);
		tabledataUpdates.put(tblnum, new TableData(table_rec));
		return db.loadTable(this, table_rec, btis);
	}

	@Override
	public synchronized void deleteTable(Table table) {
		notEnded();
		assert update_tables == null && remove_table == null;
		remove_table = table;
	}

	// table data

	@Override
	public synchronized TableData getTableData(int tblnum) {
		notEnded();
		TableData td = tabledataUpdates.get(tblnum);
		return td != null ? td : tabledata.get(tblnum);
	}

	public synchronized void updateTableData(TableData td) {
		notEnded();
		verify(!readonly);
		tabledataUpdates.put(td.tblnum, td);
	}

	// btree indexes

	@Override
	public BtreeIndex getBtreeIndex(Index index) {
		return getBtreeIndex(index.tblnum, index.columns);
	}

	@Override
	public synchronized BtreeIndex getBtreeIndex(int tblnum, String columns) {
		notEnded();
		String key = tblnum + ":" + columns;
		BtreeIndex bti = btreeIndexCopies.get(key);
		if (bti == null) {
			bti = btreeIndexes.get(key);
			if (bti == null)
				return null;
			bti = new BtreeIndex(bti, new DestTran(this, bti.getDest()));
			btreeIndexCopies.put(key, bti);
		}
		return bti;
	}

	// used by Schema
	synchronized void addBtreeIndex(BtreeIndex bti) {
		notEnded();
		bti.setDest(new DestTran(this, bti.getDest()));
		btreeIndexCopies.put(bti.tblnum + ":" + bti.columns, bti);
	}

	// actions

	synchronized void create_act(int tblnum, long adr) {
		verify(!readonly);
		if (isAborted())
			return;
		notEnded();
		addWrite(TranWrite.create(tblnum, adr));
	}

	synchronized void delete_act(int tblnum, long adr) {
		verify(!readonly);
		if (isAborted())
			return;
		notEnded();
		addWrite(TranWrite.delete(tblnum, adr));
	}

	private void addWrite(TranWrite tw) {
		writes.add(tw);
		if (writes.size() > MAX_WRITES_PER_TRANSACTION)
			abortThrow("too many writes in one transaction");
	}

	synchronized boolean isAborted() {
		return isEnded() && !isCommitted();
	}

	// Used by Data.update_record
	synchronized void undo_delete_act(int tblnum, long adr) {
		verify(!readonly);
		TranWrite tw = writes.removeLast();
		verify(tw.type == TranWrite.Type.DELETE && tw.tblnum == tblnum
				&& tw.off == adr);
	}

	private void notEnded() {
		if (asof == -1)
			throw new SuException("cannot use null transaction");
		if (ended)
			throw new SuException("cannot use ended transaction");
	}

	// abort -------------------------------------------------------------------

	@Override
	public synchronized void abortIfNotComplete() {
		if (!ended)
			abort();
	}

	synchronized void abortIfNotComplete(String conflict) {
		if (!ended)
			abort(conflict);
	}

	void abortThrow(String conflict) {
		abort(conflict);
		throw new SuException("transaction " + conflict);
	}

	void abort(String conflict) {
		this.conflict = conflict;
		abort();
	}

	@Override
	public synchronized void abort() {
		if (isAborted())
			return;
		if (conflict == null)
			this.conflict = "aborted";
		verify(isActive());
		end();
	}

	// complete ----------------------------------------------------------------

	@Override
	public synchronized void ck_complete() {
		String s = complete();
		if (s != null)
			throw new SuException("transaction commit failed: " + s);
	}

	@Override
	public synchronized String complete() {
		if (isAborted())
			return conflict;
		notEnded();
		if (readonly)
			end();
		else
			commit();
		return null;
	}

	private void end() {
		ended = true;
		trans.remove(this);
		// release memory
		shadows = null;
		tables = null;
		tabledata = null;
		tabledataUpdates = null;
		btreeIndexes = null;
		btreeIndexCopies = null;
		update_tables = null;
		remove_table = null;
		writes = null;
	}

	private void commit() {
		synchronized (db.commitLock) {
			completeReadwrite();
			trans.addFinal(this);
			commitTime = trans.clock();
			end();
		}
	}

	private void completeReadwrite() {
		updateBtreeIndexes();
		writeBtreeNodes();
		updateTableData();
		updateTables();
		writeCommitRecord();
	}

	private void updateBtreeIndexes() {
		for (Map.Entry<String, BtreeIndex> e : btreeIndexCopies.entrySet()) {
			String key = e.getKey();
			BtreeIndex btiNew = e.getValue();
			BtreeIndex btiOld = btreeIndexes.get(key);
			if (btiOld == null)
				db.addBtreeIndex(key, btiNew);
			else if (btiNew.differsFrom(btiOld))
				db.updateBtreeIndex(key, btiOld, btiNew);
		}
	}

	private void writeBtreeNodes() {
		// TODO copy nodes outside commitLock
		for (Map.Entry<Long, ByteBuf> e : shadows.entrySet()) {
			ByteBuf from = e.getValue();
			if (from.isReadOnly())
				continue;
			Long offset = e.getKey();
			trans.addShadows(this, offset);
			ByteBuf to = db.dest.adr(offset);
			to.put(0, from);
		}
	}

	private void updateTables() {
		if (remove_table != null)
			db.removeTableCommit(remove_table);
		if (update_tables != null)
			for (Table table : update_tables) {
				db.removeTableCommit(table);
				db.updateTable(table, getTableData(table.num));
				for (Index index : table.indexes)
					db.updateBtreeIndex(getBtreeIndex(index));
			}
	}

	private void updateTableData() {
		for (Map.Entry<Integer, TableData> e : tabledataUpdates.entrySet()) {
			TableData tdNew = e.getValue();
			TableData tdOld = tabledata.get(tdNew.tblnum);
			if (tdOld != null)
				db.updateTableData(tdNew.tblnum, tdNew.nextfield,
						tdNew.nrecords - tdOld.nrecords, tdNew.totalsize
								- tdOld.totalsize);
		}
	}

	private void writeCommitRecord() {
		int ncreates = 0;
		int ndeletes = 0;
		for (TranWrite tw : writes)
			switch (tw.type) {
			case CREATE:
				++ncreates;
				break;
			case DELETE:
				++ndeletes;
				break;
			default:
				throw SuException.unreachable();
			}
		if (ndeletes == 0 && ncreates == 0)
			return;
		final int n = 8 + 4 + 4 + 4 + 4 * (ncreates + ndeletes) + 4;
		synchronized (db) {
			ByteBuffer buf = db.adr(db.alloc(n, Mmfile.COMMIT)).getByteBuffer();
			buf.putLong(new Date().getTime());
			buf.putInt(num);
			buf.putInt(ncreates);
			buf.putInt(ndeletes);
			for (TranWrite tw : writes)
				if (tw.type == TranWrite.Type.CREATE)
					buf.putInt(Mmfile.offsetToInt(tw.off));
			for (TranWrite tw : writes)
				if (tw.type == TranWrite.Type.DELETE)
					buf.putInt(Mmfile.offsetToInt(tw.off));
			db.writeCommit(buf);
			verify(buf.position() == n);
		}
	}

	// end of complete ---------------------------------------------------------

	// need for PriorityQueue in Transactions
	@Override
	public int compareTo(Transaction that) {
		return num < that.num ? -1 : num > that.num ? +1 : 0;
	}

	// WARNING: don't define equals based on num
	// it causes errors e.g. in TestConcurrency

	private static class NullTransaction extends Transaction {

		@Override
		public String complete() {
			ended = true;
			return null;
		}

		@Override
		public void abort() {
			ended = true;
		}
	}

	synchronized void readLock(long offset) {
		notEnded();
		Transaction writer = trans.readLock(this, offset);
		if (writer != null) {
			if (this.inConflict || writer.outConflict)
				abortThrow("conflict (read-write) with " + writer);
			writer.inConflict = true;
			this.outConflict = true;
		}

		Set<Transaction> writes = trans.writes(offset);
		for (Transaction w : writes) {
			if (w == this || w.commitTime < asof)
				continue;
			if (w.outConflict)
				abortThrow("conflict (read-write) with " + w);
			this.outConflict = true;
		}
		for (Transaction w : writes)
			w.inConflict = true;
	}

	synchronized void writeLock(long offset) {
		notEnded();
		Set<Transaction> readers = trans.writeLock(this, offset);
		if (readers == null)
			abortThrow("conflict (write-write)");
		for (Transaction reader : readers)
			if (reader.isActive() || reader.committedAfter(this)) {
				if (reader.inConflict || this.outConflict)
					abortThrow("conflict (write-read) with " + reader);
				this.inConflict = true;
			}
		for (Transaction reader : readers)
			reader.outConflict = true;
	}

	boolean isCommitted() {
		return commitTime != Long.MAX_VALUE;
	}

	boolean committedBefore(Transaction tran) {
		return commitTime < tran.asof;
	}

	private boolean committedAfter(Transaction tran) {
		return isCommitted() && commitTime > tran.asof;
	}

	private boolean isActive() {
		return commitTime == Long.MAX_VALUE && !ended;
	}

	// delegate

	@Override
	public String getView(String viewname) {
		notEnded();
		return db.getView(this, viewname);
	}

	void removeView(String viewname) {
		notEnded();
		Database.removeView(this, viewname);
	}

	@Override
	public void addRecord(String table, Record r) {
		notEnded();
		Data.addRecord(this, table, r);
	}

	@Override
	public long updateRecord(long recadr, Record rec) {
		notEnded();
		return Data.updateRecord(this, recadr, rec);
	}

	void updateRecord(String table, String index, Record key,
			Record record) {
		notEnded();
		Data.updateRecord(this, table, index, key, record);
	}

	@Override
	public void removeRecord(long off) {
		notEnded();
		Data.removeRecord(this, off);
	}

	void removeRecord(String tablename, String index, Record key) {
		notEnded();
		Data.removeRecord(this, tablename, index, key);
	}

	@Override
	public Record input(long adr) {
		return db.input(adr);
	}

	int shadowsSize() {
		return shadows == null ? 0 : shadows.size();
	}

	int shadowSizeAtLastActivity() {
		return shadowSizeAtLastActivity;
	}

	ByteBuf node(Destination dest, long offset) {
		notEnded();
		shadowSizeAtLastActivity = shadows.size();
		return shadows.node(dest, offset);
	}

	ByteBuf nodeForWrite(Destination dest, long offset) {
		notEnded();
		shadowSizeAtLastActivity = shadows.size();
		return shadows.nodeForWrite(dest, offset);
	}

	ByteBuf shadow(Destination dest, Long offset, ByteBuf copy) {
		if (ended)
			return copy;
		return shadows.shadow(dest, offset, copy);
	}

	@Override
	protected void finalize() throws Throwable {
		abortIfNotComplete();
	}

	// used by Library
	@Override
	public Record lookup(int tblnum, String index, Record key) {
		BtreeIndex bti = getBtreeIndex(tblnum, index);
		if (bti == null)
			return null;
		BtreeIndex.Iter iter = bti.iter(key).next();
		return iter.eof() ? null : db.input(iter.keyadr());
	}

	@Override
	public void callTrigger(Table table, Record oldrec, Record newrec) {
		db.callTrigger(this, table, oldrec, newrec);
	}

	@Override
	public int num() {
		return num;
	}

	@Override
	public Record fromRef(Object ref) {
		return ref instanceof Long
				? db.input((Long) ref)
				: new Record((ByteBuf) ref);
	}

}
