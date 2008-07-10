package suneido.database.query;

import org.antlr.runtime.ANTLRStringStream;
import org.antlr.runtime.CommonTokenStream;
import org.antlr.runtime.RecognitionException;

import suneido.SuException;

public class ParseQuery {
	public static Query parse(String s) {
		ANTLRStringStream input = new ANTLRStringStream(s);
		QueryLexer lexer = new QueryLexer(input);
		CommonTokenStream tokens = new CommonTokenStream(lexer);
		QueryParser parser = new QueryParser(tokens);
		try {
			return parser.query();
		} catch (RecognitionException e) {
			throw new SuException("syntax error", e);
		}
	}

}
