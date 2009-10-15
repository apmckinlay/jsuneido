package suneido.database;

import static suneido.Suneido.verify;

import java.util.HashMap;
import java.util.Map;

/**
 * NOT thread safe, but only updated by schema transactions, during which
 * nothing else should be running
 * 
 * @author Andrew McKinlay
 */
public class Tables {
	private final Map<Integer, Table> bynum = new HashMap<Integer, Table>();
	private final Map<String, Table> byname = new HashMap<String, Table>();

	public void add(Table tbl) {
		bynum.put(tbl.num, tbl);
		byname.put(tbl.name, tbl);
	}

	public Table get(int tblnum) {
		return bynum.get(tblnum);
	}

	public Table get(String tblname) {
		return byname.get(tblname);
	}

	public void remove(String tblname) {
		Table table = byname.remove(tblname);
		if (table != null)
			verify(null != bynum.remove(table.num));
	}

	public void remove(int tblnum) {
		Table table = bynum.remove(tblnum);
		if (table != null)
			verify(null != byname.remove(table.name));
	}
}
