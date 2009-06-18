package suneido.language;

import static suneido.database.server.Command.theDbms;

import java.util.List;

import suneido.SuException;
import suneido.database.server.Dbms.LibGet;

public class Libraries {

	public static Object load(String name) {
		List<LibGet> srcs = theDbms.libget(name);
		if (srcs.isEmpty())
			return null;
		// TODO overloading
		String src = (String) Pack.unpack(srcs.get(0).text);
		try {
			return Compiler.compile(name, src);
		} catch (SuException e) {
			throw new SuException("error loading " + name + ": " + e);
		}
	}

}
