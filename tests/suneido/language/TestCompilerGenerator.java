package suneido.language;

import java.io.*;

import suneido.*;

public class TestCompilerGenerator {

	private static final StringWriter sw = new StringWriter();

	public static void main(String[] args) throws Exception {
		// byte[] b = compile("function (x) { return }");
		// dump(b);

		TestClassLoader loader = new TestClassLoader();
		Class<?> c = loader.findClass("suneido.language.SampleFunction");
		System.out.println(sw);
		SuFunction sf = (SuFunction) c.newInstance();
		SuValue[] locals = new SuValue[] { SuInteger.valueOf(12), null };
		SuValue result = sf.invoke(locals);
		System.out.println("result: " + result);
	}

	static class TestClassLoader extends ClassLoader {
		@Override
		public Class<?> findClass(String name) throws ClassNotFoundException {
			assert "suneido.language.SampleFunction".equals(name);
			byte[] b = compile("function (x) { " +
					"f = function () { 123 }; f() }");
			dump(b);
			Class<?> c = defineClass(name, b, 0, b.length);
			return c;
		}
	}
    private static byte[] compile(String s) {
		Lexer lexer = new Lexer(s);
		CompileGenerator generator = new CompileGenerator(new PrintWriter(sw));
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
