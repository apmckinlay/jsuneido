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
		return compile(name, src);
	}

	public static Object compile(String name, String src) {
		Lexer lexer = new Lexer(src);
		CompileGenerator generator = new CompileGenerator(name);
		ParseConstant<Object, Generator<Object>> pc =
				new ParseConstant<Object, Generator<Object>>(lexer, generator);
		return pc.parse();
	}
}
