package suneido.language;

import org.antlr.runtime.*;

import suneido.SuException;
import suneido.SuValue;

public class ParseLanguage {
	public static SuValue parse(String s) {
		LanguageParser parser = parser(s);
		try {
			return parser.top_constant();
		} catch (RecognitionException e) {
			throw new SuException("syntax error", e);
		}
	}

	private static LanguageParser parser(String s) {
		ANTLRStringStream input = new ANTLRStringStream(s);
		LanguageLexer lexer = new LanguageLexer(input);
		CommonTokenStream tokens = new CommonTokenStream(lexer);
		return new LanguageParser(tokens);
	}

}
