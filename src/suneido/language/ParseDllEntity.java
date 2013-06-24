package suneido.language;

import suneido.SuException;
import suneido.language.jsdi.DllInterface;

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

	protected void checkCount(int count, String object) {
		if (MAX_ITEMS < count)
			throw new SuException("too many " + object + 's');
	}
}
