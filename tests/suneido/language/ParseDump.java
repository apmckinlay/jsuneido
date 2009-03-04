package suneido.language;

import suneido.SuException;
import suneido.database.DumpReader;
import suneido.database.Record;

public class ParseDump implements DumpReader.Processor {

	int name;
	int text;

	public static void main(String[] args) {
		new DumpReader("stdlib.su", new ParseDump()).process();
		new DumpReader("Accountinglib.su", new ParseDump()).process();
		new DumpReader("etalib.su", new ParseDump()).process();
	}

	public void schema(String s) {
		if (s.contains("(name,text"))
			name = 0;
		else if (s.contains("(num,parent,name,text,"))
			name = 2;
		else
			throw new SuException("unhandled schema");
		text = name + 1;
	}

	public void record(Record rec) {
		String source = rec.getString(text);
		if (source.equals(""))
			return;

		System.out.println(rec.getString(name));

		Lexer lexer = new Lexer(source);
		StringGenerator generator = new StringGenerator();
		ParseConstant<String, Generator<String>> pc =
				new ParseConstant<String, Generator<String>>(lexer, generator);
		try {
			pc.constant();
			pc.checkEof();
		} catch (SuException e) {
			if (e.toString().contains("not supported"))
				return;
			System.out.println("!!!!!!!!! " + e);
			System.out.println(source);
			throw e;
		}
	}

	public void end() {
	}

}
