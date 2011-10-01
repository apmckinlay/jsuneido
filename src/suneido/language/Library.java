package suneido.language;

import suneido.SuException;
import suneido.TheDbms;
import suneido.database.server.Dbms.LibGet;

class Library {

	static Object load(String name) {
		if (! TheDbms.isAvailable())
			return null;
		// System.out.println("LOAD " + name);
		Object result = null;
		for (LibGet libget : TheDbms.dbms().libget(name)) {
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

}
