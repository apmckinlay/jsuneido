package suneido.language.jsdi.type;

import suneido.language.jsdi.Allocates;
import suneido.language.jsdi.DllInterface;
import suneido.language.jsdi.StorageType;

@DllInterface
@Allocates
public final class BasicArray extends Type {

	//
	// DATA
	//

	private final BasicValue underlying;
	private final int numElems;

	//
	// CONSTRUCTORS
	//

	BasicArray(BasicValue underlying, int numElems, long jsdiHandle) {
		super(TypeId.BASIC, StorageType.ARRAY, jsdiHandle);
		this.underlying = underlying;
		this.numElems = numElems;
		assert jsdiHandle != 0 : "BasicArray may not have a null handle";
	}

	//
	// ACCESSORS
	//

	public BasicValue getUnderlying() {
		return underlying;
	}

	public BasicType getBasicType() {
		return underlying.getBasicType();
	}

	public int getNumElems() {
		return numElems;
	}

	//
	// ANCESTOR CLASS: Type
	//

	@Override
	public String getDisplayName() {
		StringBuilder result = new StringBuilder(16);
		result.append(getBasicType().toIdentifier()).append('[')
				.append(numElems).append(']');
		return result.toString();
	}

	//
	// ANCESTOR CLASS: Object
	//

	@Override
	protected void finalize() throws Throwable {
		try {
			releaseHandle();
		} finally {
			super.finalize();
		}
	}
}
