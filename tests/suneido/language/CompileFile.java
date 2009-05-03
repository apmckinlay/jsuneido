package suneido.language;

import java.io.FileReader;
import java.io.IOException;

public class CompileFile {
	public static void main(String[] args) throws IOException {
		FileReader f = new FileReader("compilefile.src");
		char buf[] = new char[100000];
		int n = f.read(buf);
		f.close();
		String src = new String(buf, 0, n);
		CompileDump.compile(src, "Test");
	}
}
