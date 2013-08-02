package suneido.language.jsdi.type;

import javax.annotation.concurrent.Immutable;

import suneido.language.Ops;
import suneido.language.jsdi.DllInterface;
import suneido.language.jsdi.Marshaller;
import suneido.language.jsdi.VariableIndirectInstruction;

/**
 * <p>
 * Special type representing a multi-use string pointer, a given value of which
 * is treated by the Windows API in the following way:
 * <ul>
 * <li>
 * when the high-order bits are zero, the value is treated as a 16-bit integer
 * whose value is given by the low-order 16 bits of the pointer;</li>
 * <li>
 * when the high-order bits are non-zero, the value is treated as pointer to a
 * string.</li>
 * </ul>
 * </p>
 * 
 * <p>
 * This type therefore has some characteristics of TypeString
 * </p>
 * 
 * @author Victor Schappert
 * @since 20130731
 */
@DllInterface
@Immutable
public final class ResourceType extends StringIndirect {

	//
	// CONSTRUCTORS
	//

	private ResourceType() {
	}

	//
	// SINGLETON INSTANCE
	//

	/**
	 * Singleton instance of ResourceType.
	 * 
	 * @see #IDENTIFIER
	 */
	public static final ResourceType INSTANCE = new ResourceType();

	//
	// PUBLIC CONSTANTS
	//

	/**
	 * String identifier for ResourceType.
	 * 
	 * @see #INSTANCE
	 */
	public static final String IDENTIFIER = "resource";

	//
	// STATICS
	//

	/**
	 * Method for determining whether a value is a Win32 {@code INTRESOURCE}
	 * value, similar to the {@code IS_INTRESOURCE} macro.
	 * 
	 * @param value
	 *            Value to test
	 * @return If the value is not an {@code INTRESOURCE} value, the return
	 *         value is {@code null}; otherwise, the return value is an instance
	 *         of Short containing the signed 16-bit integer that is bitwise
	 *         equivalent to the unsigned 16-bit {@code INTRESOURCE} value
	 */
	public static Short AS_INTRESOURCE(Object value) {
		try
		{
			int intValue = Ops.toIntIfNum(value);
			return 0 == (0xffff0000 & intValue)
				? new Short((short)intValue)
				: null;
		}
		catch (RuntimeException e)
		{ return null; }
	}

	//
	// ANCESTOR CLASS: Type
	//

	@Override
	public String getDisplayName() {
		return IDENTIFIER;
	}

	@Override
	public void marshallIn(Marshaller marshaller, Object value) {
		if (null == value) {
			marshaller.putINTRESOURCE((short)0);
		} else {
			Short intResource = AS_INTRESOURCE(value);
			if (null != intResource)
				marshaller.putINTRESOURCE(intResource.shortValue());
			else
				marshaller.putStringPtr(
					value.toString(),
					VariableIndirectInstruction.RETURN_RESOURCE
				);
		}
	}

	@Override
	public Object marshallOut(Marshaller marshaller, Object oldValue) {
		return marshaller.getResource();
	}
}
