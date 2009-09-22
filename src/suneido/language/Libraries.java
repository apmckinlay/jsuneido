package suneido.language;

import static suneido.database.Database.theDB;
import static suneido.database.server.Command.theDbms;

import java.util.ArrayList;
import java.util.List;

import suneido.SuException;
import suneido.database.*;
import suneido.database.server.Dbms.LibGet;

public class Libraries {

	private static final List<String> libraries = new ArrayList<String>();
	static {
		libraries.add("stdlib");
	}

	public static List<String> libraries() {
		return libraries;
	}

	public static Object load(String name) {
		List<LibGet> srcs = theDbms.libget(name);
		if (srcs.isEmpty())
			return null;
		//System.out.println("LOAD " + name);
		String src = (String) Pack.unpack(srcs.get(0).text);
		try {
			return Compiler.compile(name, src);
		} catch (SuException e) {
			throw new SuException("error loading " + name + ": " + e);
		}
	}

	public static List<LibGet> libget(String name) {
		List<LibGet> srcs = new ArrayList<LibGet>();
		if (theDB == null)
			return srcs;
		Record key = new Record();
		key.add(name);
		key.add(-1);
		Transaction tran = theDB.readonlyTran();
		try {
			for (String lib : libraries) {
				Table table = theDB.getTable(lib);
				if (table == null)
					continue;
				List<String> flds = table.getFields();
				int group_fld = flds.indexOf("group");
				int text_fld = flds.indexOf("text");
				Index index = theDB.getIndex(table, "name,group");
				if (group_fld < 0 || text_fld < 0 || index == null)
					continue; // library is invalid, ignore it
				BtreeIndex.Iter iter = index.btreeIndex.iter(tran, key).next();
				if (!iter.eof()) {
					Record rec = theDB.input(iter.keyadr());
					srcs.add(new LibGet(lib, rec.getraw(text_fld)));
				}
			}
		} finally {
			tran.complete();
		}
		return srcs;
	}

	public static boolean use(String library) {
		if (libraries.contains(library))
			return false;
		try {
			theDbms.cursor(null, library + " project group, name, text");
			theDbms.admin(null, "ensure " + library + " key(name,group)");
		} catch (RuntimeException e) {
			return false;
		}
		libraries.add(library);
		Globals.clear();
		return true;
	}

	public static boolean unuse(String library) {
		if ("stdlib".equals(library) || !libraries.contains(library))
			return false;
		libraries.remove(library);
		Globals.clear();
		return true;
	}

}
