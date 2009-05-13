package suneido.language;

import java.io.FileReader;
import java.io.IOException;

import suneido.database.*;

public class CompileFile {
	public static void main(String[] args) throws IOException {
		FileReader f = new FileReader("compilefile.src");
		char buf[] = new char[100000];
		int n = f.read(buf);
		f.close();
		String src = new String(buf, 0, n);
		Object c = TestCompiler.compile(src);

		Globals.put("T", c);

		Mmfile mmf = new Mmfile("suneido.db", Mode.OPEN);
		Database.theDB = new Database(mmf, Mode.OPEN);

		Compiler.eval("T.Test_params()");
	}
}
