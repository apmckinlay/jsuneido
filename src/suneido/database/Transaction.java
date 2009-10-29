package suneido.database;

import static suneido.Suneido.verify;

import java.nio.ByteBuffer;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

import net.jcip.annotations.NotThreadSafe;
import suneido.SuException;
import suneido.database.server.DbmsTran;
import suneido.util.ByteBuf;
import suneido.util.PersistentMap;

/**
 * Handles a single transaction, either readonly or readwrite.
 * equals and hashCode are the default i.e. identity
 *
 * @author Andrew McKinlay
 * <p><small>Copyright 2008 Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.</small></p>
 */
@NotThreadSafe // should only used by one session i.e. one thread at a time
public class Transaction implements Comparable<Transaction>, DbmsTran {
	private final Database db;
	private final Transactions trans;
	private final boolean readonly;
	protected boolean ended = false;
	private boolean inConflict = false;
	private boolean outConflict = false;
	private String conflict = null;
	private final long asof;
	private long commitTime = 0;
	String sessionId = "session";
	private final Tables tables;
	private final PersistentMap<Integer, TableData> tabledata;
	final Map<Integer, TableData> tabledataUpdates =
			new HashMap<Integer, TableData>();
	public final PersistentMap<String, BtreeIndex> btreeIndexes;
	private final Map<String, BtreeIndex> btreeIndexUpdates =
			new HashMap<String, BtreeIndex>();
	private List<Table> update_tables = null;
	private Table remove_table = null;

	public final int num;
	final Deque<TranWrite> writes = new ArrayDeque<TranWrite>();
	public static final Transaction NULLTRAN = new NullTransaction();

	// needs to be concurrent because complete of other transactions
	// will insert original copies into here so we don't see their changes
	final Map<Long, ByteBuf> shadow = new ConcurrentHashMap<Long, ByteBuf>();

	Transaction(Transactions trans, boolean readonly, Tables tables,
			PersistentMap<Integer, TableData> tabledata,
			PersistentMap<String, BtreeIndex> btreeIndexes) {
		this.db = trans.db;
		this.trans = trans;
		this.readonly = readonly;
		asof = trans.clock();
		num = trans.nextNum();
		this.tables = tables;
		this.tabledata = tabledata;
		this.btreeIndexes = btreeIndexes;
		trans.add(this);
	}

	// used for cursors
	public Transaction(Transactions trans, Tables tables,
			PersistentMap<Integer, TableData> tabledata,
			PersistentMap<String, BtreeIndex> btreeIndexes) {
		this.db = trans.db;
		this.trans = trans;
		this.tables = tables;
		this.tabledata = tabledata;
		this.btreeIndexes = btreeIndexes;
		readonly = true;
		asof = num = 0;
	}

	// used by NullTransaction
	Transaction() {
		this.db = null;
		this.trans = null;
		this.tables = null;
		this.tabledata = null;
		this.btreeIndexes = null;
		readonly = true;
		asof = num = 0;
	}

	@Override
	public String toString() {
		return "Transaction " + (readonly ? "read " : "update ") +
				num + " asof " + asof;
	}

	public boolean isReadonly() {
		return readonly;
	}

	public boolean isReadWrite() {
		return ! readonly;
	}

	public boolean isEnded() {
		return ended;
	}

	public long asof() {
		return asof;
	}

	public String conflict() {
		return conflict;
	}

	public boolean tableExists(String table) {
		return getTable(table) != null;
	}

	public Table ck_getTable(String tablename) {
		Table tbl = getTable(tablename);
		if (tbl == null)
			throw new SuException("nonexistent table: " + tablename);
		return tbl;
	}
	public Table getTable(String tablename) {
		if (tablename == null)
			return null;
		return tables.get(tablename);
	}

	public Table ck_getTable(int tblnum) {
		Table tbl = getTable(tblnum);
		if (tbl == null)
			throw new SuException("nonexistent table: " + tblnum);
		return tbl;
	}

	public Table getTable(int tblnum) {
		return tables.get(tblnum);
	}

	public void updateTable(Table table) {
		assert remove_table == null;
		if (update_tables == null)
			update_tables = new ArrayList<Table>();
		update_tables.add(table);
	}

	public void deleteTable(Table table) {
		assert update_tables == null && remove_table == null;
		remove_table = table;
	}

	public String getView(String name) {
		return db.getView(this, name);
	}

	// table data

	public TableData getTableData(int tblnum) {
		TableData td = tabledataUpdates.get(tblnum);
		return td != null ? td : tabledata.get(tblnum);
	}

	public void updateTableData(TableData td) {
		verify(!readonly);
		tabledataUpdates.put(td.num, td);
	}

	// btree indexes

	public BtreeIndex getBtreeIndex(Index index) {
		return getBtreeIndex(index.tblnum, index.columns);
	}

	public BtreeIndex getBtreeIndex(int tblnum, String columns) {
		String key = tblnum + ":" + columns;
		BtreeIndex bti = btreeIndexUpdates.get(key);
		if (bti == null) {
			bti = btreeIndexes.get(key);
			bti = new BtreeIndex(bti, new TranDest(this, bti.getDest()));
			btreeIndexUpdates.put(key, bti);
assert bti.getDest() instanceof TranDest;
		}
		return bti;
	}

	public void addBtreeIndex(BtreeIndex bti) {
		bti.setDest(new TranDest(this, bti.getDest()));
assert bti.getDest() instanceof TranDest;
		btreeIndexUpdates.put(bti.tblnum + ":" + bti.columns, bti);
	}

	// actions

	public void create_act(int tblnum, long adr) {
		verify(! readonly);
		if (isAborted())
			return;
		notEnded();
		writes.add(TranWrite.create(tblnum, adr, trans.clock()));
	}

	public void delete_act(int tblnum, long adr) {
		verify(! readonly);
		if (isAborted())
			return;
		notEnded();
		writes.add(TranWrite.delete(tblnum, adr, trans.clock()));
	}

	boolean isAborted() {
		return isEnded() && ! isCommitted();
	}

	public void undo_delete_act(int tblnum, long adr) {
		verify(!readonly);
		TranWrite tw = writes.removeLast();
		verify(tw.type == TranWrite.Type.DELETE && tw.tblnum == tblnum
				&& tw.off == adr);
	}

	private void notEnded() {
		if (ended)
			throw new SuException("cannot use "
					+ (isCommitted() ? "completed" : "aborted")
					+ " transaction");
	}

	// complete & abort

	public void ck_complete() {
		String s = complete();
		if (s != null)
			throw new SuException("transaction commit failed: " + s);
	}

	public String complete() {
		if (isAborted())
			return conflict;
		notEnded();
		if (!readonly && !writes.isEmpty())
			completeReadwrite();
		commitTime  = trans.clock();
		trans.remove(this);
		ended = true;
		return null;
	}

	private void completeReadwrite() {
		updateBtreeIndexes();
		writeBtreeNodes();
		updateTableData();
		updateTables();

		int ncreates = 0;
		int ndeletes = 0;
		commitTime  = trans.clock();
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
		trans.addFinal(this);
		writeCommitRecord(ncreates, ndeletes);
	}

	private void writeBtreeNodes() {
		trans.addShadows(this, shadow);
		for (Map.Entry<Long, ByteBuf> e : shadow.entrySet()) {
			ByteBuf to = db.dest.adr(e.getKey());
			ByteBuf from = e.getValue();
			to.put(0, from.array());
		}
	}

	private void updateTables() {
		if (remove_table != null)
			db.removeTable(remove_table);
		if (update_tables != null)
			for (Table table : update_tables) {
				db.removeTable(table);
				db.updateTable(table, getTableData(table.num));
				for (Index index : table.indexes)
					db.updateBtreeIndex(getBtreeIndex(index));
			}
	}

	private void updateTableData() {
		for (Map.Entry<Integer, TableData> e : tabledataUpdates.entrySet()) {
			TableData tdNew = e.getValue();
			TableData tdOld = tabledata.get(tdNew.num);
			if (tdOld != null)
				db.updateTableData(tdNew.num, tdNew.nextfield,
						tdNew.nrecords - tdOld.nrecords,
						tdNew.totalsize	- tdOld.totalsize);
		}
	}

	private void updateBtreeIndexes() {
		for (Map.Entry<String, BtreeIndex> e : btreeIndexUpdates.entrySet()) {
			String key = e.getKey();
			BtreeIndex btiNew = e.getValue();
			BtreeIndex btiOld = btreeIndexes.get(key);
			if (btiOld == null)
				db.addBtreeIndex(key, btiNew);
			else if (btiNew.differsFrom(btiOld))
				db.updateBtreeIndex(key, btiOld, btiNew);
		}
	}

	private void writeCommitRecord(int ncreates, int ndeletes) {
		if (ndeletes == 0 && ncreates == 0)
			return;
		final int n = 8 + 4 + 4 + 4 + 4 * (ncreates + ndeletes) + 4;
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
		// include commit in checksum, but don't include checksum itself
		db.checksum(buf, buf.position());
		buf.putInt(db.getChecksum());
		verify(buf.position() == n);
		db.resetChecksum();
	}

	public void abort() {
		verify(isActive());
		abort("aborted");
	}

	public void abortIfNotComplete() {
		if (!ended)
			abort();
	}

	public int compareTo(Transaction other) {
		return asof < other.asof ? -1 : asof > other.asof ? +1 : 0;
	}

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

	public void readLock(long offset) {
		Transaction writer = trans.readLock(this, offset);
		if (writer != null) {
			if (this.inConflict || writer.outConflict) {
				abort("read-write conflict");
				return;
			}
			writer.inConflict = true;
			this.outConflict = true;
		}

		Set<Transaction> writes = trans.writes(offset);
		for (Transaction w : writes) {
			if (w == this
					|| (w.isCommitted() && w.commitTime < asof))
				continue;
			if (w.outConflict) {
				abort("read-write conflict");
				return;
			}
			this.outConflict = true;
		}
		for (Transaction w : writes)
			w.inConflict = true;
	}

	public void writeLock(long offset) {
		Set<Transaction> readers = trans.writeLock(this, offset);
		if (readers == null) {
			abort("write-write conflict");
			return;
		}
		for (Transaction reader : readers)
			if (reader.isActive() || reader.commitTime > asof) {
				if (reader.inConflict || this.outConflict) {
					abort("write-read conflict"); //
					return;
				}
				this.inConflict = true;
			}
		for (Transaction reader : readers)
			reader.outConflict = true;
	}

	boolean isCommitted() {
		return commitTime != 0;
	}

	private boolean isActive() {
		return commitTime == 0 && ! ended;
	}

	private void abort(String conflict) {
		if (isAborted())
			return;
		this.conflict = conflict;
		trans.remove(this);
		ended = true;
	}

}
