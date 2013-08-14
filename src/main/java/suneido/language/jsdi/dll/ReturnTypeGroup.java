package suneido.language.jsdi.dll;

import suneido.language.jsdi.DllInterface;
import suneido.language.jsdi.JSDIException;
import suneido.language.jsdi.type.BasicValue;
import suneido.language.jsdi.type.InOutString;
import suneido.language.jsdi.type.Type;

/**
 * TODO: docs
 * @author Victor Schappert
 * @since 20130717
 */
@DllInterface
enum ReturnTypeGroup {

	/**
	 * Indicates that the return value of the function is an integer value
	 * of 64 bits or less in length.
	 */
	INTEGER,
	/**
	 * Indicates that the return value of the function is an IEEE
	 * double-precision floating point value. Both {@code float} and
	 * {@code double} return types belong to this return type group because
	 * under the {@code stdcall} calling convention, these values are returned
	 * on the top of the floating point register stack. Since x86 floating
	 * point registers have 80 bits of precision, we might as well always
	 * return {@code double} values.
	 */
	DOUBLE,
	/**
	 * Indicates that the return value of the function is a pointer to a
	 * string of unpredictable length (<em>ie</em> a variable indirect type).
	 */
	VARIABLE_INDIRECT;

	public static ReturnTypeGroup fromType(Type type) {
		switch (type.getTypeId()) {
		case VOID:
			return INTEGER;
		case BASIC:
			switch (((BasicValue)type).getBasicType()) {
			case FLOAT:  // intentional fall through
			case DOUBLE:
				return DOUBLE;
			default:
				return INTEGER;
			}
		default:
			if (type == InOutString.INSTANCE) {
				return VARIABLE_INDIRECT;
			}
			throw new JSDIException("type " + type.getDisplayName()
					+ " may not be used as a dll return type");
		}
	}
}