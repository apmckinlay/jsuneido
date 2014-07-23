/* Copyright 2013 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido.language;

import static suneido.SuInternalError.unreachable;
import static suneido.language.Token.ARRAYTYPE;
import static suneido.language.Token.CALLBACK;
import static suneido.language.Token.COMMA;
import static suneido.language.Token.DLL;
import static suneido.language.Token.IDENTIFIER;
import static suneido.language.Token.IN;
import static suneido.language.Token.L_BRACKET;
import static suneido.language.Token.MUL;
import static suneido.language.Token.NEWLINE;
import static suneido.language.Token.POINTERTYPE;
import static suneido.language.Token.R_BRACKET;
import static suneido.language.Token.R_CURLY;
import static suneido.language.Token.R_PAREN;
import static suneido.language.Token.SEMICOLON;
import static suneido.language.Token.STRUCT;
import static suneido.language.Token.VALUETYPE;
import suneido.SuException;
import suneido.jsdi.DllInterface;

/**
 * Shared members and functionality for matching entities that form part of the
 * DLL interface.
 * @author Victor Schappert
 * @since 20130621
 */
@DllInterface
public abstract class ParseDllEntity<T, G extends Generator<T>> extends Parse<T, G> {

	//
	// STATIC FIELDS
	//

	private static final int MAX_ITEMS = 100; // Carry-over from CSuneido

	//
	// CONSTRUCTORS
	//

	protected ParseDllEntity(Lexer lexer, G generator) {
		super(lexer, generator);
	}

	protected ParseDllEntity(Parse<T, G> parse) {
		super(parse);
	}

	//
	// METHODS
	//

	private static void checkCount(int count, Token listType) {
		if (MAX_ITEMS < count) {
			switch (listType) {
			case DLL:
				throw new SuException("too many dll parameters");
			case STRUCT:
				throw new SuException("too many struct members");
			case CALLBACK:
				throw new SuException("too many callback parameters");
			default:
				throw unreachable();
			}
		}
	}

	private boolean hasAnItem(final Token listType) {
		switch (listType) {
		case STRUCT:
			// Intentional fall-through
		case CALLBACK:
			return IDENTIFIER == token;
		case DLL:
			return IDENTIFIER == token ||
				   L_BRACKET == token /* for [in] parameters */;
		default:
			throw unreachable();
		}
	}

	private boolean hasAnotherItem(final Token listType) {
		switch (listType) {
		case STRUCT:
			// TODO: should really allow an arbitrary number of consecutive
			//       semicolons...
			if (SEMICOLON == token || NEWLINE == token) {
				match();
				return hasAnItem(STRUCT);
			} else {
				return false;
			}
		case DLL:
			// Intentional fall-through
		case CALLBACK:
			if (COMMA == token) {
				match();
				return true;
			} else {
				return false;
			}
		default:
			throw unreachable();
		}
	}

	private String getName(final Token listType) {
		final String name = lexer.getValue();
		switch (listType) {
		case STRUCT:
			matchKeepNewline(IDENTIFIER);
			break;
		case DLL:
			// Intentional fall-through
		case CALLBACK:
			match(IDENTIFIER);
			break;
		default:
			throw unreachable();
		}
		return name;
	}

	private void matchEnd(final Token listType) {
		switch (listType) {
		case STRUCT:
			match(R_CURLY);
			break;
		case DLL:
			// Intentional fall-through
		case CALLBACK:
			match(R_PAREN);
			break;
		default:
			throw unreachable();
		}
	}

	/**
	 * <p>
	 * Parses out a type list (either a member list for a <code>struct</code> or
	 * a parameter list for a <code>dll</code> or <code>callback</code>.
	 * </p>
	 * <p>
	 * <strong>NOTE</strong> that this parsing is slightly less permissive than
	 * CSuneido's. For example, CSuneido permitted the following constructs:
	 * <ul>
	 * <li><code>struct { Type1 member1 Type2 member2 }</code> &mdash; note that
	 * lack of semicolon or newline between the members; and</li>
	 * <li><code>dll ReturnType Library:FuncName(Type1 param1 Type2 param2)</code>
	 * &mdash; note the lack of comma between the members;
	 * </ul>
	 * but JSuneido strictly requires the correct separator, which is
	 * <em>either</em> a semicolon or a newline for <code>struct</code> members,
	 * and <em>always</em> a comma for parameters.
	 * </p>
	 * @param listType Must be either {@link STRUCT}, {@link DLL}, or
	 * {@link CALLBACK}.
	 * @return The parsed list
	 * @since 20130705
	 */
	protected final T typeList(final Token listType) {
		assert STRUCT == listType || DLL == listType || CALLBACK == listType;
		T list = null;
		if (hasAnItem(listType))
		{
			int n = 1;
			do
			{
				checkCount(n++, listType);
				boolean in = false;
				// DLL only: check for '[in]' parameter
				if (DLL == listType && L_BRACKET == token) {
					match();
					match(IN);
					match(R_BRACKET);
					in = true;
				}
				// Pull out the identifier
				String baseType = lexer.getValue();
				match(IDENTIFIER);
				// NOTE: At the moment, don't allow pointer + array in same
				//       structure member. We allow only the following syntax:
				//           type name
				//           type * name
				//           type[N] name
				// TODO: Introduce another syntax:
				//           type[] * name
				//       This would allow a pointer to an array containing an
				//       arbitrary number of elements. Marshalling code can infer
				//       the size from the given parameters. For example, if you
				//       have
				//           x = struct { string a; long[] * b }
				//           f = dll void lib:f(x * p)
				//       then
				//           f(Object(a: "abc", b: Object(1, 2, 3))
				//       would cause the marshalling code to send a 3-element long
				//       array to the DLL function. For OUT-only parameters, we
				//       would need some kind of placeholder, say a built-in type
				//       called ArraySize(). Then you could say:
				//           f(Object(a: "abc", b: ArraySize(3))
				//       and the presence of the placeholder would tell the
				//       marshaller to allocate an array of size 3.
				Token storageType = VALUETYPE;
				String numElems = "1";
				if (MUL == token) { // pointer
					storageType = POINTERTYPE;
					match();
				}
				else if (L_BRACKET == token) { // array
					match();
					storageType = ARRAYTYPE;
					numElems = lexer.getValue();
					matchNonNegativeInteger();
					match(R_BRACKET);
				}
				String name = getName(listType);
				list = generator.typeList(list, name, in, baseType,
						storageType, numElems);
			}
			while (hasAnotherItem(listType));
		}
		matchEnd(listType);
		return list;
	}
}
