package suneido.database;

public class TranRead {
	int tblnum;
	String index;
	Record org = Record.MINREC;
	Record end = Record.MAXREC;
	
	TranRead(int tblnum, String index) {
		this.tblnum = tblnum;
		this.index = index;
	}
}
