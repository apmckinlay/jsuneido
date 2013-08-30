package suneido.language.builtin;

import suneido.language.Ops;
import suneido.language.Params;

/**
 * Built-in implementation of the {@code Name(value)} function.
 *
 * @author Victor Schappert
 * @since 20130815
 * @see Type
 */
public final class Name {

	//
	// Built-in functions
	//

	@Params("value")
	public static String Name(Object a) {
		return Ops.valueName(a);
	}

}
