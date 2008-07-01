package suneido.database;

public class TranWrite {
	enum Type { CREATE, DELETE };
	Type type;
	int tblnum;
	long off;
	long time;

	private TranWrite(Type type, int tblnum, long off, long time) {
		this.type = type;
		this.tblnum = tblnum;
		this.off = off;
		this.time = time;
	}

	public static TranWrite create(int tblnum, long off, long time) {
		return new TranWrite(Type.CREATE, tblnum, off, time);
	}

	public static TranWrite delete(int tblnum, long off, long time) {
		return new TranWrite(Type.DELETE, tblnum, off, time);
	}
}
