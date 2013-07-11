package suneido.language.jsdi.type;

import suneido.language.jsdi.DllInterface;
import suneido.language.jsdi.MarshallPlan;
import suneido.language.jsdi.StorageType;

/**
 * <p>
 * Type implementing the Suneido constructs {@code string[x]} and
 * {@code buffer[x]} for some positive integer {@code x}.
 * </p>
 * Suneido treats these entities like a special case. Where {@code string} or
 * {@code buffer} behave like <em>pointers</em> to zero-terminated or
 * non-zero- terminated strings, respectively, {@code string[x]} and
 * {@code buffer[x]} behave like {@code char[x]} (<em>ie</em> an in-place array)
 * <em>except</em> that they are marshalled to/from Suneido string or
 * {@code Buffer} objects instead of to/from Suneido {@code Object}-based arrays
 * of one-character strings.
 * </p>
 * @author Victor Schappert
 * @since 20130710
 * @see StringIndirect
 */
@DllInterface
public final class StringDirect extends StringType {

	//
	// DATA
	//

	private final int numChars;

	//
	// CONSTRUCTORS
	//

	StringDirect(int numChars, boolean isZeroTerminated) {
		super(TypeId.STRING_DIRECT, StorageType.ARRAY, new MarshallPlan(
				numChars * SizeDirect.CHAR), isZeroTerminated);
		// NOTE: If we ever introduce wide-character strings and buffers, this
		//       class can probably handle it just by parameterizing the basic
		//       type.
		assert 0 < numChars : "String or buffer must have at least one character";
		this.numChars = numChars;
	}

	//
	// ANCESTOR CLASS: Type
	//

	@Override
	public String getDisplayName() {
		return new StringBuilder(12)
				.append(isZeroTerminated ? IDENTIFIER_STRING
						: IDENTIFIER_BUFFER).append('[').append(numChars)
				.append(']').toString();
	}
}
