package suneido.database;

public class TranRead {
	int tblnum;
	String index;
	BufRecord org = BufRecord.MINREC;
	BufRecord end = BufRecord.MAXREC;
	
	TranRead(int tblnum, String index) {
		this.tblnum = tblnum;
		this.index = index;
	}
}
