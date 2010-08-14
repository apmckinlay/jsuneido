package suneido.language;

import suneido.SuException;
import suneido.database.DumpReader;
import suneido.database.Record;

public class CompileDump implements DumpReader.Processor {

	int iname;
	int itext;

	public static void main(String[] args) {
//		new DumpReader("stdlib.su", new CompileDump()).process();
//		new DumpReader("Accountinglib.su", new CompileDump()).process();
		new DumpReader("etalib.su", new CompileDump()).process();
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

		compile(source, name);
	}

	public static void compile(String source, String name) {
//		if (! name.equals("CLucene"))
//			return;
		Lexer lexer = new Lexer(source);
		CompileGenerator generator = new CompileGenerator(name);
		ParseConstant<Object, Generator<Object>> pc =
				new ParseConstant<Object, Generator<Object>>(lexer, generator);
		try {
			pc.parse();
		} catch (SuException e) {
			if (e.toString().matches(".*(not support|can't find _).*"))
				return;
			//System.out.println(source);
			System.out.println("line " + lexer.getLineNumber());
			throw e;
		} catch (Throwable e) {
			//System.out.println(source);
			System.out.println("line " + lexer.getLineNumber());
			throw new SuException("", e);
		}
	}

	public void end() {
	}

}
