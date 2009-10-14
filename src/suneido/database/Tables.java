package suneido.database;

import static suneido.Suneido.verify;

import java.util.HashMap;

/**
 * @author Andrew McKinlay
 * <p><small>Copyright 2008 Suneido Software Corp. All rights reserved. 
 * Licensed under GPLv2.</small></p>
 */
public class Tables {
	private final HashMap<Integer, Table> bynum = new HashMap<Integer, Table>();
	private final HashMap<String, Table> byname = new HashMap<String, Table>();

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
