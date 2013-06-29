package suneido.language;

import static suneido.language.Token.*;
import suneido.language.jsdi.DllInterface;

/**
 * Parser for Suneido 'struct' type (part of the DLL interface).
 * @author Victor Schappert
 * @since 20130621
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
		T members = null;
		for (int n = 1; Token.IDENTIFIER == token; ++n) {
			checkCount(n, "structure member");
			String baseType = lexer.getValue();
			match();
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
				match(NUMBER);
				match(R_BRACKET);
			}
			String name = lexer.getValue();
			match(IDENTIFIER);
			matchIf(SEMICOLON);
			members = generator.structMembers(members, name, baseType, storageType, numElems);
		}
		match(R_CURLY);
		return generator.struct(members);
	}
}
