package suneido.database;

import static suneido.SuException.unreachable;
import static suneido.SuException.verify;

import java.io.File;
import java.io.IOException;
import java.util.*;

import suneido.SuException;
import suneido.database.Database.TN;
import suneido.util.ByteBuf;

import com.google.common.collect.ImmutableList;

public class DbRebuild extends DbCheck {
	private final String filename;
	private Database newdb;
	private final BitSet deletes = new BitSet();
	private final Map<Long,Long> tr = new HashMap<Long,Long>();
	private int max_tblnum = -1;
	private final Map<Integer,String> tblnames = new HashMap<Integer,String>();
	private final Map<Integer,Long> deleted_tbls = new HashMap<Integer,Long>();
	private final Map<Integer,Long> renamed_tbls = new HashMap<Integer,Long>();
	private final Checksum cksum = new Checksum();
	// 8 byte overhead (two int's) plus 8 byte alignment
	// means smallest block is 16 bytes
	final private int GRANULARITY = 16;

	protected DbRebuild(String filename) {
		super(filename);
		this.filename = filename;
	}

	public static void rebuildOrExit(String filename) {
		DbRebuild dbr = new DbRebuild(filename);
		switch (dbr.checkPrint()) {
		case OK:
		case CORRUPTED:
			dbr.rebuild();
			System.out.println("-<:+:>-");
		case UNRECOVERABLE:
			System.exit(-1);
		default:
			throw unreachable();
		}
	}

	void rebuild() {
		System.out.println("Rebuilding...");
		File tmpfile;
//tmpfile = new File("rebuild.db");
		try {
			tmpfile = File.createTempFile("sudb", null, new File("."));
		} catch (IOException e) {
			throw new SuException("rebuild failed", e);
		}
		try {
			newdb = new Database(tmpfile, Mode.CREATE);
			copy();
			mmf.close();
			newdb.close();
			newdb = null;

			File dbfile = new File(filename);
			File bakfile = new File(filename + ".bak");
			bakfile.delete();
			dbfile.renameTo(bakfile);
			tmpfile.renameTo(dbfile);
		} finally {
			if (newdb != null) {
				newdb.close();
				newdb = null;
			}
			tmpfile.delete();
		}
	}

	private boolean copy() {
		Mmfile.MmfileIterator iter = mmf.iterator();
		while (iter.hasNext()) {
			ByteBuf buf = iter.next();
			if (iter.type() == Mmfile.OTHER ||
					(iter.type() == Mmfile.COMMIT && isCommitOther(buf)))
				continue; // skip
			long newoff = copyBlock(buf, iter.length(), iter.type());
			switch (iter.type()) {
			case Mmfile.SESSION:
				handleSession();
				break;
			case Mmfile.COMMIT:
				handleCommit(newoff);
				break;
			case Mmfile.DATA:
				handleData(buf, iter.offset(), newoff);
				break;
			default:
				return false;
			}
		}
		return true;
	}

	private boolean isCommitOther(ByteBuf buf) {
		Commit commit = new Commit(buf);
		if (commit.getNCreates() != 1 || commit.getNDeletes() != 0)
			return false;
		long offset = commit.getCreate(0);
		return mmf.type(offset - 4) == Mmfile.OTHER;
	}

	private void handleSession() {
		cksum.reset();
	}

	private void handleData(ByteBuf buf, long oldoff, long newoff) {
		int tblnum = buf.getInt(0);
		if (tblnum != TN.TABLES && tblnum != TN.INDEXES) {
			Record r = new Record(buf.slice(4));
			cksum.add(buf.getByteBuffer(), r.bufSize() + 4);
		}
		tr.put(oldoff, newoff);
	}

	private long copyBlock(ByteBuf buf, int n, byte type) {
		long offset = newdb.alloc(n, type);
		ByteBuf newbuf = newdb.adr(offset);
		newbuf.put(0, buf.slice(0, n));
		return offset;
	}

	private void handleCommit(long newoff) {
		ByteBuf buf = newdb.adr(newoff);
		Commit commit = new Commit(buf);

		handleCommitEntries(commit);
		cksum.add(buf.getByteBuffer(), commit.sizeWithoutChecksum());
		commit.putChecksum(cksum.getValue());
		cksum.reset();
	}

	private void handleCommitEntries(Commit commit) {
		if (isTableRename(commit)) {
			long oldoff = commit.getDelete(0);
			long newoff = tr.get(oldoff - 4) + 4;
			ByteBuf buf = newdb.adr(newoff - 4);
			int tblnum = buf.getInt(0);
			Record rec = new Record(buf.slice(4), newoff);
			newdb.removeIndexEntriesForRebuild(tblnum, rec);
		}
		for (int i = 0; i < commit.getNCreates(); ++i) {
			long oldoff = commit.getCreate(i);
			long newoff = tr.get(oldoff - 4) + 4;
			commit.putCreate(i, newoff);
			addIndexEntries(oldoff, newoff);
		}
		for (int i = 0; i < commit.getNDeletes(); ++i) {
			long oldoff = commit.getDelete(i);
			long newoff = tr.get(oldoff - 4) + 4;
			commit.putDelete(i, newoff);
		}
	}

	private int tblnum(long offset) {
		ByteBuf buf = mmf.adr(offset - 4);
		return buf.getInt(0);
	}

	private void addIndexEntries(long oldoff, long newoff) {
		if (isDeleted(oldoff))
			return;
		ByteBuf buf = newdb.adr(newoff - 4);
		int tblnum = buf.getInt(0);
		Record rec = new Record(buf.slice(4), newoff);
		if (tblnum <= TN.INDEXES)
			handleSchemaRecord(tblnum, rec, newoff);
		else {
			String tablename = tblnames.get(tblnum);
			if (tablename == null)
				return;
			newdb.addIndexEntriesForRebuild(tblnum, rec);
		}
	}

	private boolean isDeleted(long oldoff) {
		oldoff -= 4;
		verify(oldoff % 8 == 4);
		verify(oldoff / GRANULARITY < Integer.MAX_VALUE);
		return deletes.get((int) (oldoff / GRANULARITY));
	}

	private void handleSchemaRecord(int tblnum, Record rec, long newoff) {
		int tn = rec.getInt(0);
		if (tn <= TN.INDEXES)
			return; // handled by Database create
		newoff += 4;
		switch (tblnum) {
		case TN.TABLES:
			handleTablesRecord(rec);
			break;
		case TN.COLUMNS:
			handleColumnsRecord(rec);
			break;
		case TN.INDEXES:
			handleIndexesRecord(rec);
			break;
		default:
			throw unreachable();
		}
	}

	private void handleTablesRecord(Record rec) {
		int tn = rec.getInt(Table.TBLNUM);
		if (tn > max_tblnum)
			max_tblnum = tn;
		String tablename = rec.getString(Table.TABLE);
		tblnames.put(tn, tablename);
		newdb.addIndexEntriesForRebuild(TN.TABLES, rec);
		reloadTable(rec);
	}

	private void reloadTable(Record rec) {
		Transaction tran = newdb.readonlyTran();
		reloadTable(tran, rec);
		tran.complete();
	}

	private void handleColumnsRecord(Record rec) {
		newdb.addIndexEntriesForRebuild(TN.COLUMNS, rec);
		reloadTable(rec.getInt(Column.TBLNUM));
	}

	private void handleIndexesRecord(Record rec) {
		newdb.addIndexEntriesForRebuild(TN.INDEXES, rec);
		BtreeIndex.rebuild(newdb.dest, rec);
		reloadTable(rec.getInt(Index.TBLNUM));

		Transaction tran = newdb.readwriteTran();
		insertExistingRecords(tran, rec);
		tran.complete();
	}

	void insertExistingRecords(Transaction tran, Record rec) {
		int tblnum = rec.getInt(Index.TBLNUM);
		Table table = tran.getTable(tblnum);
		if (table.indexes.size() == 1)
			return; // first index
		String columns = rec.getString(Index.COLUMNS);
		BtreeIndex btreeIndex = tran.getBtreeIndex(tblnum, columns);
		ImmutableList<Integer> colnums = table.getIndex(columns).colnums;
		Index index = table.firstIndex();
		BtreeIndex.Iter iter = tran.getBtreeIndex(index).iter(tran).next();
		for (; !iter.eof(); iter.next()) {
			Record r = newdb.input(iter.keyadr());
			Record key = r.project(colnums, iter.cur().keyadr());
			verify(btreeIndex.insert(tran, new Slot(key)));
		}
	}

	private void reloadTable(int tblnum) {
		Transaction tran = newdb.readonlyTran();
		Record table_rec = newdb.getTableRecord(tran, tblnum);
		reloadTable(tran, table_rec);
		tran.complete();
	}

	private void reloadTable(Transaction tran, Record table_rec) {
		List<BtreeIndex> btis = new ArrayList<BtreeIndex>();
		Table table = newdb.loadTable(tran, table_rec, btis);
		newdb.updateTable(table, new TableData(table_rec));
		for (BtreeIndex bti : btis)
			newdb.updateBtreeIndex(bti);
	}

	@Override
	// called by DbCheck
	protected void process_deletes(Commit commit) {
		if (isTableRename(commit))
			return;

		for (int i = 0; i < commit.getNDeletes(); ++i) {
			long del = commit.getDelete(i);
			verify(del % 8 == 0);
			// - 4 because offsets are to data, which is preceded by table number
			del = (del - 4) / GRANULARITY;
			verify(del < Integer.MAX_VALUE);
			deletes.set((int) del);
		}
	}

	private boolean isTableRename(Commit commit) {
		return commit.getNCreates() == 1
				&& commit.getNDeletes() == 1
				&& isTableRecord(commit.getDelete(0))
				&& isTableRecord(commit.getCreate(0));
	}

	private boolean isTableRecord(long offset) {
		return mmf.type(offset - 4) == Mmfile.DATA
				&& tblnum(offset) == TN.TABLES;
	}

	public static void main(String[] args) {
		rebuildOrExit("suneido.db");
	}

}
