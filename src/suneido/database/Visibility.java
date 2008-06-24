package suneido.database;

public interface Visibility {
	TranRead read_act(int tran, int tblnum, String index);
	boolean visible(int tran, long adr);
}
