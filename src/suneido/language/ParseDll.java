package suneido.language;

import static suneido.language.Token.*;
import suneido.language.jsdi.DllInterface;

/**
 * Parser for Suneido <code>dll</code> entity (part of the DLL interface).
 * @author Victor Schappert
 * @since 20130705
 * @see ParseStruct
 *
 * @param <T> Parse result type (<em>ie</em> {@link AstNode}).
 * @param <G> Result type generator class (<em>ie</em> {@link AstGenerator}).
 */
@DllInterface
public final class ParseDll<T, G extends Generator<T>> extends ParseDllEntity<T, G> {

	//
	// CONSTRUCTORS
	//

	public ParseDll(Lexer lexer, G generator) {
		super(lexer, generator);
		expectingCompound = false;
	}

	ParseDll(Parse<T, G> parse) {
		super(parse);
		expectingCompound = false;
	}

	//
	// RECURSIVE DESCENT PARSING OF 'dll' ENTITY
	//

	public T parse() {
		return matchReturn(EOF, dll());
	}

	public T dll() {
		matchSkipNewlines(DLL);
		// Return Type
		final String returnTypeBase = lexer.getValue();
		final T returnType = null;
			// TODO: do this properly -- will need a generator method to return
			//       a "return type" node...
			// TODO: should support all value types of size 0..8 bytes and all
			//       pointer types, but not array types... ?? or support array
			//       types up to 8 bytes?
		match(IDENTIFIER);
		// Library Name
		final String libraryName = lexer.getValue();
		match(IDENTIFIER);
		match(COLON);
		// Function Name as provided by the user
		final StringBuilder userFunctionName = new StringBuilder(
				lexer.getValue());
		match(IDENTIFIER);
		if (AT == token) {
			match();
			userFunctionName.append('@');
			userFunctionName.append(lexer.getValue());
			matchNonNegativeInteger();
		}
		// Parameters
		match(L_PAREN);
		T params = typeList(DLL);
		return generator.dll(libraryName, userFunctionName.toString(),
				returnType, params);
	}
}
