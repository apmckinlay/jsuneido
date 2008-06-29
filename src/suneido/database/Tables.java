package suneido.database;

import static suneido.Suneido.verify;

import java.util.HashMap;

public class Tables {
	private HashMap<Integer, Table> bynum;
	private HashMap<String, Table> byname;

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
		if (null == byname.get(tblname))
			return;
		verify(null != bynum.remove(byname.get(tblname).num));
		verify(null != byname.remove(tblname));
	}

	public void remove(int tblnum) {
		if (null == bynum.get(tblnum))
			return;
		String tblname = bynum.get(tblnum).name;
		verify(null != byname.remove(tblname));
		verify(null != bynum.remove(tblnum));
	}
}
