/* Copyright 2013 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido.language;

import static suneido.language.Token.EOF;
import static suneido.language.Token.L_CURLY;
import static suneido.language.Token.STRUCT;
import suneido.jsdi.DllInterface;

/**
 * Parser for Suneido <code>struct</code> type (part of the DLL interface).
 * @author Victor Schappert
 * @since 20130621
 * @see ParseDll
 * @see ParseCallback
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
	}

	ParseStruct(Parse<T, G> parse) {
		super(parse);
	}

	//
	// RECURSIVE DESCENT PARSING OF 'struct' ENTITY
	//

	public T parse() {
		return matchReturn(EOF, struct());
	}

	public T struct() {
		int lineNumber = lexer.getLineNumber();
		matchSkipNewlines(STRUCT);
		match(L_CURLY);
		T members = typeList(STRUCT);
		return generator.struct(members, lineNumber);
	}
}
