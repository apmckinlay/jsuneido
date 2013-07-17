package suneido.language.jsdi.type;

import suneido.language.jsdi.DllInterface;
import suneido.language.jsdi.MarshallPlan;
import suneido.language.jsdi.Marshaller;
import suneido.language.jsdi.StorageType;

/**
 * TODO: docs
 * @author Victor Schappert
 * @since 20130710
 * @see StringDirect
 */
@DllInterface
public final class StringIndirect extends StringType {

	//
	// DATA
	//

	private final boolean hasInModifier;

	//
	// CONSTRUCTORS
	//

	private StringIndirect(boolean isZeroTerminated, boolean hasInModifier) {
		super(TypeId.STRING_INDIRECT, StorageType.POINTER, MarshallPlan
				.makeVariableIndirectPlan(), isZeroTerminated);
		this.hasInModifier = hasInModifier;
		assert isZeroTerminated || !hasInModifier : "[in] buffer not allowed";
	}

	//
	// STATICS
	//

	/**
	 * "Singleton" instance (I guess it's really a tripleton because of
	 * {@link #INSTANCE_BUFFER} and {@link #INSTANCE_IN_STRING}}) representing a
	 * pointer to a zero-terminated string (<em>ie</em> the type represented by
	 * the Suneido identifier {@code string}).
	 * @see #INSTANCE_IN_STRING
	 * @see #INSTANCE_BUFFER 
	 */
	public static final StringIndirect INSTANCE_STRING = new StringIndirect(
			true, false);
	/**
	 * <p>
	 * "Singleton" instance representing the special type of zero-terminated
	 * string indicated by the Suneido code {@code [in] string}.
	 * </p>
	 * <p>
	 * In CSuneido, the {@code [in]} modifier permitted the DLL marshaller to
	 * make assumptions which increased efficiency by eliminating unnecessary
	 * copying. In jSuneido, this optimization is no longer possible and the
	 * type {@code [in] string} should be considered
	 * <strong>deprecated</strong>.
	 * </p>
	 * @see #INSTANCE_STRING
	 * @see #INSTANCE_BUFFER
	 */
	public static final StringIndirect INSTANCE_IN_STRING = new StringIndirect(
			true, true);

	/**
	 * "Singleton" instance (more accurately a tripleton &mdash; see also
	 * {@link #INSTANCE_STRING} and {@link #INSTANCE_IN_STRING}) representing a
	 * pointer to a string of characters which is not zero-terminated
	 * (<em>ie</em> the type represented by the Suneido identifier
	 * {@code buffer}).
	 * @see #INSTANCE_STRING
	 * @see #INSTANCE_IN_STRING
	 */
	public static final StringIndirect INSTANCE_BUFFER = new StringIndirect(
			false, false);

	//
	// ANCESTOR CLASS: Type
	//

	@Override
	public String getDisplayName() {
		return isZeroTerminated
			? (hasInModifier ? "[in] " + IDENTIFIER_STRING : IDENTIFIER_STRING)
			: IDENTIFIER_BUFFER
			;
	}
	
	@Override
	public int countVariableIndirect(Object value) {
		if (null != value) {
			int extra = isZeroTerminated ? SizeDirect.CHAR : 0;
			return value.toString().length() + extra;
		}
		return 0;
	}

	@Override
	public void marshallIn(Marshaller marshaller, Object value) {
		if (null != value) {
			String str = value.toString();
			if (isZeroTerminated) {
				marshaller.putZeroTerminatedStringIndirect(str);
			} else {
				marshaller.putNonZeroTerminatedStringIndirect(str);
			}
		} else {
			marshaller.putNullPtr();
			// Don't need to skip because there isn't an element in the
			// marshaller's posArray.
		}
	}
}
