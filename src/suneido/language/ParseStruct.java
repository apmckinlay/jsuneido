package suneido.language;

import static suneido.language.Token.*;
import suneido.language.jsdi.DllInterface;

/**
 * Parser for Suneido <code>struct</code> type (part of the DLL interface).
 * @author Victor Schappert
 * @since 20130621
 * @see ParseDll
 *
 * @param <T> Parse result type (<em>ie</em> {@link AstNode}).
 * @param <G> Result type generator class (<em>ie</em> {@link AstGenerator}).
 */
@DllInterface
public final class ParseStruct<T, G extends Generator<T>> extends ParseDllEntity<T, G> {

	//
	// CONSTRUCTORS
	//

	public ParseStruct(Lexer lexer, G generator) {
		super(lexer, generator);
		expectingCompound = false;
	}

	ParseStruct(Parse<T, G> parse) {
		super(parse);
		expectingCompound = false;
	}

	//
	// RECURSIVE DESCENT PARSING OF 'struct' ENTITY
	//

	public T parse() {
		return matchReturn(EOF, struct());
	}

	public T struct() {
		matchSkipNewlines(STRUCT);
		match(L_CURLY);
		T members = typeList(STRUCT);
		return generator.struct(members);
	}
}
