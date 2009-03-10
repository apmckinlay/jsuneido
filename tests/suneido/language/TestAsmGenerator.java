package suneido.language;

import java.io.FileOutputStream;
import java.io.IOException;

import suneido.SuException;

public class TestAsmGenerator {

	public static void main(String[] args) {
		// byte[] b = compile("function (x) { return }");
		// dump(b);

		TestClassLoader loader = new TestClassLoader();
		try {
			Class<?> c = loader.findClass("suneido.language.SampleFunction");
			SuFunction sf = (SuFunction) c.newInstance();
			sf.invoke();
		} catch (Exception e) {
			throw new SuException("class not found");
		}
	}

	static class TestClassLoader extends ClassLoader {
		@Override
		public Class<?> findClass(String name) throws ClassNotFoundException {
			assert "suneido.language.SampleFunction".equals(name);
			byte[] b = compile("function (x) { return }");
			dump(b);
			Class<?> c = defineClass(name, b, 0, b.length);
			return c;
		}
	}
    private static byte[] compile(String s) {
		Lexer lexer = new Lexer(s);
		CompileGenerator generator = new CompileGenerator();
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
