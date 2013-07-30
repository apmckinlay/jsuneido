package suneido.language.jsdi.dll;

import suneido.language.jsdi.DllInterface;
import suneido.language.jsdi.JSDIException;
import suneido.language.jsdi.type.InOutString;
import suneido.language.jsdi.type.Type;

/**
 * TODO: docs
 * @author Victor Schappert
 * @since 20130717
 */
@DllInterface
enum ReturnTypeGroup {

	VOID,
	_32_BIT,
	_64_BIT;

	public static ReturnTypeGroup fromType(Type type) {
		switch (type.getTypeId()) {
		case VOID:
			return VOID;
		case BASIC:
			final int size = type.getSizeDirectIntrinsic();
			if (size <= 4) {
				return _32_BIT;
			} else {
				assert size <= 8;
				return _64_BIT;
			}
		default:
			if (type == InOutString.INSTANCE){
				return _32_BIT;
			}
			throw new JSDIException("type " + type.getDisplayName()
					+ " may not be used as a dll return type");
		}
	}
}
