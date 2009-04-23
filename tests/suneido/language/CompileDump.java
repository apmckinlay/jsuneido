package suneido.language;

import suneido.SuException;
import suneido.database.DumpReader;
import suneido.database.Record;

public class CompileDump implements DumpReader.Processor {

	int iname;
	int itext;

	public static void main(String[] args) {
		new DumpReader("stdlib.su", new CompileDump()).process();
	}

	public void schema(String s) {
		if (s.contains("(name,text"))
			iname = 0;
		else if (s.contains("(num,parent,name,text,"))
			iname = 2;
		else
			throw new SuException("unhandled schema");
		itext = iname + 1;
	}

	public void record(Record rec) {
		String source = rec.getString(itext);
		if (source.equals(""))
			return;

		String name = rec.getString(iname);
		System.out.println(name);

		Lexer lexer = new Lexer(source);
		CompileGenerator generator = new CompileGenerator(name);
		ParseConstant<Object, Generator<Object>> pc =
				new ParseConstant<Object, Generator<Object>>(lexer, generator);
		try {
			pc.parse();
		} catch (SuException e) {
			if (e.toString().contains("not supported"))
				return;
			System.out.println("!!!!!!!!! " + e);
			//System.out.println(source);
			throw e;
		}
	}

	public void end() {
	}

}
