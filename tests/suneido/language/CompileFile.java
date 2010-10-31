package suneido.language;

import java.io.*;

import com.google.common.base.Charsets;
import com.google.common.io.Files;

public class CompileFile {
	public static void main(String[] args) throws IOException {
//		Repl.setup();
		String src = Files.toString(new File("compilefile.src"), Charsets.US_ASCII);
		Object result = Compiler.compile("Test", src,
				new PrintWriter(System.out));
		System.out.println(result);
	}

}
