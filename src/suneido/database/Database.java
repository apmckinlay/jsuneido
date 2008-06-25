package suneido.database;

import static java.lang.Math.min;
import java.nio.ByteBuffer;
import java.util.zip.Adler32;
import static suneido.Suneido.verify;

public class Database implements Visibility, Destination {
	private Mmfile mmf;
	private DbHdr dbhdr;
	private boolean loading = false;
//	private long clock = 1;
	private Adler32 cksum = new Adler32();
	private byte output_type = Mmfile.DATA;
	private static class TN {
		final static private int TABLES = 0;
		final static private int COLUMNS = 1;
		final static private int INDEXES = 2;
		final static private int VIEWS = 3;
	}
	public final static int SCHEMA_TRAN = 0;
	private final static int VERSION = 1;
	private Index tables_index;
	private Index tblnum_index;
	private Index columns_index;
	private Index indexes_index;
	private Index fkey_index;
	// used by get_view
	private Index views_index;

	public Database(String filename, Mode mode) {		
		mmf = new Mmfile(filename, mode);
//		if (mode == Mode.OPEN && ! check_shutdown(mmf)) {
//			mmf.close();
//			if (0 != fork_rebuild())
//				fatal("Database not rebuilt, unable to start");
//			mmf = new Mmfile(filename, mode);
//			verify(check_shutdown(mmf));
//		}
		if (mode == Mode.CREATE) {
			output_type = Mmfile.OTHER;
			create();
			output_type = Mmfile.DATA;
		} else {
			open();
		}
	}
	private void create() {
		loading = true;
		
		dbhdr = new DbHdr(adr(alloc(DbHdr.SIZE, Mmfile.OTHER)), TN.INDEXES + 1, VERSION);

		// tables
		tables_index = new Index(this, this, TN.TABLES, "tablename", true, false);
		tblnum_index = new Index(this, this, TN.TABLES, "table", true, false);
		table_record(TN.TABLES, "tables", 3, 5);
		table_record(TN.COLUMNS, "columns", 17, 3);
		table_record(TN.INDEXES, "indexes", 5, 9);
//		hdr->next_table = TN.INDEXES + 1;

		// columns
//		columns_index = new Index(this, TN.COLUMNS, "table,column", true);
//		columns_record(TN.TABLES, "table", 0);
//		columns_record(TN.TABLES, "tablename", 1);
//		columns_record(TN.TABLES, "nextfield", 2);
//		columns_record(TN.TABLES, "nrows", 3);
//		columns_record(TN.TABLES, "totalsize", 4);
//
//		columns_record(TN.COLUMNS, "table", 0);
//		columns_record(TN.COLUMNS, "column", 1);
//		columns_record(TN.COLUMNS, "field", 2);
//
//		columns_record(TN.INDEXES, "table", 0);
//		columns_record(TN.INDEXES, "columns", 1);
//		columns_record(TN.INDEXES, "key", 2);
//		columns_record(TN.INDEXES, "fktable", 3);
//		columns_record(TN.INDEXES, "fkcolumns", 4);
//		columns_record(TN.INDEXES, "fkmode", 5);
//		columns_record(TN.INDEXES, "root", 6);
//		columns_record(TN.INDEXES, "treelevels", 7);
//		columns_record(TN.INDEXES, "nnodes", 8);

		// indexes
//		indexes_index = new Index(this, TN.INDEXES, "table,columns", true);
//		fkey_index = new Index(this, TN.INDEXES, "fktable,fkcolumns", false);
//		indexes_record(tables_index);
//		indexes_record(tblnum_index);
//		indexes_record(columns_index);
//		// output indexes indexes last
//		dbhdr.indexes = indexes_record(indexes_index);
//		indexes_record(fkey_index);

		// views
//		add_table("views");
//		add_column("views", "view_name");
//		add_column("views", "view_definition");
//		add_index("views", "view_name", true);
		
		dbhdr.update();
	}
	void table_record(int tblnum, String tblname, int nrows, int nextfield) {
		Record r = new Record();
		r.add(tblnum).add(tblname).add(nextfield).add(nrows).add(100);
		r.alloc(24); // 24 = 3 fields * max int packsize - min int packsize
		long at = output(TN.TABLES, r);
		verify(tblnum_index.insert(SCHEMA_TRAN, new Slot(new Record().add(tblnum).addMmoffset(at))));
		verify(tables_index.insert(SCHEMA_TRAN, new Slot(new Record().add(tblname).addMmoffset(at))));
		}

	void open() {
		dbhdr = new DbHdr(adr(mmf.first()));
//		if (mmf.length(dbhdr()) < sizeof (Dbhdr) || dbhdr.version != DB_VERSION)
//			fatal("incompatible database\n\nplease dump with the old exe and load with the new one");
//		new (adr(alloc(sizeof (Session), MM_SESSION))) Session(Session::STARTUP);
//		mmf->sync();
//		indexes_index = mkindex(ckroot(Record(input(dbhdr.indexes))));
//		
//		Record r = find(schema_tran, indexes_index, key(TN.INDEXES, "table,columns"));
//		verify(! nil(r) && r.off() == indexes);
//		
//		tables_index = mkindex(ckroot(find(schema_tran, indexes_index, key(TN.TABLES, "tablename"))));
//		tblnum_index = mkindex(ckroot(find(schema_tran, indexes_index, key(TN.TABLES, "table"))));
//		columns_index = mkindex(ckroot(find(schema_tran, indexes_index, key(TN.COLUMNS, "table,column"))));
//		fkey_index = mkindex(ckroot(find(schema_tran, indexes_index, key(TN.INDEXES, "fktable,fkcolumns"))));
		// WARNING: any new indexes added here must also be added in get_table
	}


	public void close() {
//		shutdown();
		mmf.close();
	}

	long output(int tblnum, Record r) {
		int n = r.packSize();
		long offset = alloc(4 + n, output_type);
		ByteBuffer p = adr(offset);
		p.putInt(tblnum);
		r.pack(p);
		// don't checksum tables or indexes records because they get updated
		if (output_type == Mmfile.DATA && tblnum != TN.TABLES && tblnum != TN.INDEXES)
			checksum(adr(offset + 4), 4 + n);
		return offset;
	}
	public long alloc(int n) { 
		return alloc(n, Mmfile.OTHER);
	}
	long alloc(int n, byte type) { 
		return mmf.alloc(n, type);
	}
	public ByteBuffer adr(long offset) {
		return mmf.adr(offset);
	}
	public long size() {
		return mmf.size();
	}
	
	static byte[] bytes = new byte[256];
	void checksum(ByteBuffer buf, int len) {
		for (int i = 0; i < len; i += bytes.length) {
			int n = min(bytes.length, len - i);
			buf.get(bytes, i, n);
			cksum.update(bytes, 0, n);
		}
	}
	
	private static class DbHdr {
		static final int SIZE = 4 + 4 + 4;
		ByteBuffer buf;		
		int next_table;
		long indexes;
		int version;
		
		DbHdr(ByteBuffer buf) {
			this.buf = buf;
			buf.position(0);
			next_table = buf.getInt();
			indexes = Mmfile.intToOffset(buf.getInt());
			version = buf.getInt();
		}
		DbHdr(ByteBuffer buf, int next_table, int version) {
			this.buf = buf;
			this.next_table = next_table;
			this.indexes = 0;
			this.version = version;
		}
		void update() {
			buf.position(0);
			buf.putInt(next_table);
			buf.putInt(Mmfile.offsetToInt(indexes));
			buf.putInt(version);
		}
	}

	public TranRead read_act(int tran, int tblnum, String index) {
		// TODO Auto-generated method stub
		return new TranRead(tblnum, index);
	}
	public boolean visible(int tran, long adr) {
		// TODO Auto-generated method stub
		return true;
	}
}
