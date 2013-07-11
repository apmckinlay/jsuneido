package suneido.language.jsdi.type;

import suneido.language.jsdi.DllInterface;
import suneido.language.jsdi.MarshallPlan;
import suneido.language.jsdi.StorageType;

/**
 * TODO: docs
 * @author Victor Schappert
 * @since 20130625
 */
@DllInterface
public final class BasicArray extends Type {

	//
	// DATA
	//

	private final BasicValue underlying;
	private final int numElems;

	//
	// CONSTRUCTORS
	//

	BasicArray(BasicValue underlying, int numElems) {
		super(TypeId.BASIC, StorageType.ARRAY, arrayPlan(underlying
				.getBasicType().getMarshallPlan(), numElems));
		this.underlying = underlying;
		this.numElems = numElems;
	}

	private static MarshallPlan arrayPlan(MarshallPlan valuePlan, int numElems) {
		return MarshallPlan.makeArrayPlan(valuePlan, numElems);
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
