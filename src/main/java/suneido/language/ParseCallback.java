package suneido.language;

import static suneido.language.Token.*;
import suneido.language.jsdi.DllInterface;

/**
 * Parser for Suneido <code>callback</code> type (part of the DLL interface).
 * @author Victor Schappert
 * @since 20130710
 * @see ParseStruct
 * @see ParseDll
 *
 * @param <T> Parse result type (<em>ie</em> {@link AstNode}).
 * @param <G> Result type generator class (<em>ie</em> {@link AstGenerator}).
 */
@DllInterface
public final class ParseCallback<T, G extends Generator<T>> extends ParseDllEntity<T, G> {

	//
	// CONSTRUCTORS
	//

	public ParseCallback(Lexer lexer, G generator) {
		super(lexer, generator);
		expectingCompound = false;
	}

	ParseCallback(Parse<T, G> parse) {
		super(parse);
		expectingCompound = false;
	}

	//
	// RECURSIVE DESCENT PARSING OF 'callback' ENTITY
	//

	public T parse() {
		return matchReturn(EOF, callback());
	}

	public T callback() {
		matchSkipNewlines(CALLBACK);
		match(L_PAREN);
		T params = typeList(CALLBACK);
		return generator.callback(params);
	}

}
