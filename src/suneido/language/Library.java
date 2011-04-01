package suneido.language;

import java.util.ArrayList;
import java.util.List;

import suneido.CommandLineOptions.Action;
import suneido.*;
import suneido.database.*;
import suneido.database.query.Query.Dir;
import suneido.database.server.Dbms.LibGet;

import com.google.common.collect.Lists;

public class Library {
	private static final List<String> libraries = Lists.newArrayList("stdlib");

	public static List<String> libraries() {
		return libraries;
	}

	public static Object load(String name) {
		//System.out.println("LOAD " + name);
		return load2(name, TheDbms.dbms().libget(name));
	}
	synchronized private static Object load2(String name, List<LibGet> libgets) {
		Object result = null;
		for (LibGet libget : libgets) {
			String src = (String) Pack.unpack(libget.text);
			try {
				result = Compiler.compile(name, src);
				Globals.put(name, result); // needed inside loop for overloading
			} catch (SuException e) {
				throw new SuException("error loading " + name, e);
			}
		}
		return result;
	}

	public static List<LibGet> libget(String name) {
		List<LibGet> srcs = new ArrayList<LibGet>();
		if (! TheDb.isOpen())
			return srcs;
		Record key = new Record();
		key.add(name);
		key.add(-1);
		Transaction tran = TheDb.db().readonlyTran();
		try {
			for (String lib : libraries) {
				Table table = tran.getTable(lib);
				if (table == null)
					continue;
				List<String> flds = table.getFields();
				int group_fld = flds.indexOf("group");
				int text_fld = flds.indexOf("text");
				BtreeIndex bti = tran.getBtreeIndex(table.num, "name,group");
				if (group_fld < 0 || text_fld < 0 || bti == null)
					continue; // library is invalid, ignore it
				BtreeIndex.Iter iter = bti.iter(tran, key).next();
				if (!iter.eof()) {
					Record rec = TheDb.db().input(iter.keyadr());
					srcs.add(new LibGet(lib, rec.getraw(text_fld)));
				}
			}
		} finally {
			tran.complete();
		}
		return srcs;
	}

	public static boolean use(String library) {
		if (TheDbms.dbms().libraries().contains(library))
			return false;
		if (Suneido.cmdlineoptions.action == Action.CLIENT)
			throw new SuException("can't Use('" + library + "')\n" +
					"When client-server, only the server can Use");
		try {
			TheDbms.dbms().get(Dir.NEXT, library + " project group, name, text", false);
			TheDbms.dbms().admin("ensure " + library + " key(name,group)");
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
