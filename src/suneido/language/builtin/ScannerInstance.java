package suneido.language.builtin;

import static suneido.language.Token.EOF;

import java.util.Iterator;

import suneido.SuException;
import suneido.SuValue;
import suneido.language.*;

public class ScannerInstance extends SuValue implements Iterable<String>, Iterator<String> {
	private final Lexer lexer;
	private Token token;

	private static final FunctionSpec sFS = new FunctionSpec("name");

	public ScannerInstance(Object[] args) {
		args = Args.massage(sFS, args);
		lexer = new Lexer(Ops.toStr(args[0]));
	}

	@Override
	public Object invoke(Object self, String method, Object... args) {
		if (method == "Next")
			return Next(args);
		if (method == "Position")
			return Position(args);
		if (method == "Type")
			return Type(args);
		if (method == "Text")
			return Text(args);
		if (method == "Value")
			return Value(args);
		if (method == "Keyword")
			return Keyword(args);
		if (method == "Iter")
			return Iter(args);
		throw new SuException("unknown method: scanner." + method);
	}

	private Object Position(Object[] args) {
		Args.massage(FunctionSpec.noParams, args);
		return lexer.end();
	}

	private Object Type(Object[] args) {
		Args.massage(FunctionSpec.noParams, args);
		return null;
	}

	private Object Text(Object[] args) {
		Args.massage(FunctionSpec.noParams, args);
		return lexer.matched();
	}

	private Object Value(Object[] args) {
		Args.massage(FunctionSpec.noParams, args);
		return lexer.getValue();
	}

	private Object Keyword(Object[] args) {
		Args.massage(FunctionSpec.noParams, args);
		return lexer.getKeyword().oldnum;
	}

	private Object Iter(Object[] args) {
		return this;
	}

	private Object Next(Object... args) {
		Args.massage(FunctionSpec.noParams, args);
		token = lexer.nextAll();
		if (token == EOF)
			return this;
		return lexer.matched();
	}

	public Iterator<String> iterator() {
		return this;
	}

	public boolean hasNext() {
		return lexer.hasNext();
	}

	public String next() {
		return (String) Next();
	}

	public void remove() {
		throw new UnsupportedOperationException();
	}

}
