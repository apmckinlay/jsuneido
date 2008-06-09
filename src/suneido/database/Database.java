package suneido.database;

import static java.lang.Math.min;
import java.nio.ByteBuffer;
import java.util.zip.Adler32;
import java.io.File;

public class Database {
	private Mmfile mmf;
	private DbHdr dbhdr;
	private boolean loading = false;
//	private long clock = 1;
	private Adler32 cksum = new Adler32();
	private byte output_type = Mmfile.DATA;
	static class TN {
		final static private int TABLES = 0;
		final static private int COLUMNS = 1;
		final static private int INDEXES = 2;
		final static private int VIEWS = 3;
	}
	private final static int VERSION = 1;

	public Database(String filename, Mode mode) {		
		mmf = new Mmfile(filename, mode);
//		if (mode == Mode.OPEN && ! check_shutdown(mmf)) {
//			mmf.close();
//			if (0 != fork_rebuild())
//				fatal("Database not rebuilt, unable to start");
//			mmf = new Mmfile(filename, mode);
//			verify(check_shutdown(mmf));
//		}
//		dest = new IndexDest(mmf);
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
//		tables_index = new Index(this, TN_TABLES, "tablename", true);
//		tblnum_index = new Index(this, TN_TABLES, "table", true);
//		table_record(TN_TABLES, "tables", 3, 5);
//		table_record(TN_COLUMNS, "columns", 17, 3);
//		table_record(TN_INDEXES, "indexes", 5, 9);
//		hdr->next_table = TN_INDEXES + 1;

		// columns
//		columns_index = new Index(this, TN_COLUMNS, "table,column", true);
//		columns_record(TN_TABLES, "table", 0);
//		columns_record(TN_TABLES, "tablename", 1);
//		columns_record(TN_TABLES, "nextfield", 2);
//		columns_record(TN_TABLES, "nrows", 3);
//		columns_record(TN_TABLES, "totalsize", 4);
//
//		columns_record(TN_COLUMNS, "table", 0);
//		columns_record(TN_COLUMNS, "column", 1);
//		columns_record(TN_COLUMNS, "field", 2);
//
//		columns_record(TN_INDEXES, "table", 0);
//		columns_record(TN_INDEXES, "columns", 1);
//		columns_record(TN_INDEXES, "key", 2);
//		columns_record(TN_INDEXES, "fktable", 3);
//		columns_record(TN_INDEXES, "fkcolumns", 4);
//		columns_record(TN_INDEXES, "fkmode", 5);
//		columns_record(TN_INDEXES, "root", 6);
//		columns_record(TN_INDEXES, "treelevels", 7);
//		columns_record(TN_INDEXES, "nnodes", 8);

		// indexes
//		indexes_index = new Index(this, TN_INDEXES, "table,columns", true);
//		fkey_index = new Index(this, TN_INDEXES, "fktable,fkcolumns", false);
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
	void open() {
		dbhdr = new DbHdr(adr(mmf.first()));
//		if (mmf.length(dbhdr()) < sizeof (Dbhdr) || dbhdr.version != DB_VERSION)
//			fatal("incompatible database\n\nplease dump with the old exe and load with the new one");
//		new (adr(alloc(sizeof (Session), MM_SESSION))) Session(Session::STARTUP);
//		mmf->sync();
//		indexes_index = mkindex(ckroot(Record(input(dbhdr.indexes))));
//		
//		Record r = find(schema_tran, indexes_index, key(TN_INDEXES, "table,columns"));
//		verify(! nil(r) && r.off() == indexes);
//		
//		tables_index = mkindex(ckroot(find(schema_tran, indexes_index, key(TN_TABLES, "tablename"))));
//		tblnum_index = mkindex(ckroot(find(schema_tran, indexes_index, key(TN_TABLES, "table"))));
//		columns_index = mkindex(ckroot(find(schema_tran, indexes_index, key(TN_COLUMNS, "table,column"))));
//		fkey_index = mkindex(ckroot(find(schema_tran, indexes_index, key(TN_INDEXES, "fktable,fkcolumns"))));
		// WARNING: any new indexes added here must also be added in get_table
	}


	public void close() {
//		shutdown();
		mmf.close();
	}

	long output(int tblnum, MemRecord r) {
		int n = r.bufsize();
		long offset = alloc(4 + n, output_type);
		ByteBuffer p = adr(offset);
		p.putInt(tblnum);
		p = adr(offset + 4);
		r.store(adr(offset + 4));
		// don't checksum tables or indexes records because they get updated
		if (output_type == Mmfile.DATA && tblnum != TN.TABLES && tblnum != TN.INDEXES)
			checksum(p, 4 + n);
		return offset;
	}
	long alloc(int n) { 
		return alloc(n, Mmfile.OTHER);
	}
	long alloc(int n, byte type) { 
		return mmf.alloc(n, type);
	}
	ByteBuffer adr(long offset) {
		return mmf.adr(offset);
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
}
