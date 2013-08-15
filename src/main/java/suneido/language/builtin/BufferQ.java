package suneido.language.builtin;

import suneido.language.Params;
import suneido.language.jsdi.Buffer;
import suneido.language.jsdi.DllInterface;

/**
 * Built-in function {@code Buffer?(value)}. Returns {@code true} if-and-only-if
 * the given value is a {@code Buffer} instance.
 * @author Victor Schappert
 * @since 201301214
 * @see StructQ
 * @see DllQ
 */
@DllInterface
public final class BufferQ {

	@Params("value")
	public static Boolean BufferQ(Object a) {
		return a instanceof Buffer;
	}

}
