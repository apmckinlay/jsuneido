package suneido.database;

import java.util.List;

import suneido.SuValue;
import suneido.database.query.Header;
import suneido.database.query.Row;
import suneido.database.query.Query.Dir;

public interface Dbms {
	enum TranType { READONLY, READWRITE };
	int transaction(TranType type, String session_id);
	boolean commit(int tran, String conflict );
	void abort(int tran);

	boolean admin(String s);
	int request(int tran, String s);
	DbmsQuery cursor(String s);
	DbmsQuery query(int tran, String s);
	List<String> libget(String name);
	List<String> libraries();
	List<Integer> tranlist();
	SuValue timestamp();
	void dump(String filename);
	void copy(String filename);
	SuValue run(String s);
	long size();
	SuValue connections();
	void erase(int tran, long recadr);
	long update(int tran, long recadr, Record rec);
	boolean record_ok(int tran, long recadr);
	Row get(Dir dir, String query, boolean one, Header hdr, int tran );
	int tempdest();
	int cursors();
	SuValue sessionid(String s);
	boolean refresh(int tran);
	int finalSize();
	void log(String s);
	int kill(String s);
}
