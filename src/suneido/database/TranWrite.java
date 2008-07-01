package suneido.database;

public class TranWrite {
	enum Type { CREATE, DELETE };
	Type type;
	int tblnum;
	long off;
	long time;
	
	TranWrite(Type type, int tblnum, long off, long time) {
		this.type = type;
		this.tblnum = tblnum;
		this.off = off;
		this.time = time;
	}
}
