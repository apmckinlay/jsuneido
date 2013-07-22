package suneido.language.builtin;

import static suneido.language.Token.EOF;

import java.util.Iterator;

import suneido.SuValue;
import suneido.language.*;

public class Scanner extends SuValue implements Iterable<String>, Iterator<String> {
	private final Lexer lexer;
	private Token token;
	private static final BuiltinMethods methods = new BuiltinMethods(Scanner.class);

	public Scanner(String s) {
		lexer = new Lexer(s);
	}

	@Override
	public SuValue lookup(String method) {
		return methods.lookup(method);
	}

	public static Object Position(Object self) {
			return ((Scanner) self).lexer.end();
	}

	public static Object Type(Object self) {
			return ((Scanner) self).token.oldnum;
	}

	public static Object Text(Object self) {
			return ((Scanner) self).lexer.matched();
	}

	public static Object Value(Object self) {
			return ((Scanner) self).lexer.getValue();
	}

	public static Object Keyword(Object self) {
			return ((Scanner) self).lexer.getKeyword().oldnum;
	}

	public static Object Iter(Object self) {
			return self;
	}

	public static Object Next(Object self) {
			String s = ((Scanner) self).next();
			return s == null ? self : s;
	}

	@Override
	public Iterator<String> iterator() {
		return this;
	}

	@Override
	public boolean hasNext() {
		return lexer.hasNext();
	}

	@Override
	public String next() {
		token = lexer.nextAll();
		if (token == EOF)
			return null;
		return lexer.matched();
	}

	@Override
	public void remove() {
		throw new UnsupportedOperationException();
	}

	public static final BuiltinClass clazz = new BuiltinClass() {
		@Override
		public Scanner newInstance(Object... args) {
			args = Args.massage(FunctionSpec.string, args);
			return new Scanner(Ops.toStr(args[0]));
		}
	};

}
