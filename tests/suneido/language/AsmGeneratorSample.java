package suneido.language;

import java.io.FileOutputStream;
import java.io.IOException;

import suneido.SuException;

public class AsmGeneratorSample {

	public static void main(String[] args) {
		byte[] b = compile("function (x) { return }");
		dump(b);
	}

    private static byte[] compile(String s) {
		Lexer lexer = new Lexer(s);
		AsmGenerator generator = new AsmGenerator();
		ParseFunction<Object, Generator<Object>> pc =
				new ParseFunction<Object, Generator<Object>>(lexer, generator);
		return (byte[]) pc.parse();
	}

	private static void dump(byte[] buf) {
		try {
			FileOutputStream f = new FileOutputStream("SampleFunction.class");
			f.write(buf);
			f.close();
		} catch (IOException e) {
			throw new SuException("dump error");
		}
	}

}
