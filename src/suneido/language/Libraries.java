package suneido.language;

import static suneido.database.server.Command.theDbms;

import java.util.List;

import suneido.database.server.Dbms.LibGet;

public class Libraries {

	public static Object load(String name) {
		List<LibGet> srcs = theDbms.libget(name);
		if (srcs.isEmpty())
			return null;
		// TODO overloading
		String src = (String) Pack.unpack(srcs.get(0).text);
		return Compiler.compile(name, src);
	}

}
