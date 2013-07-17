package suneido.language.jsdi.type;

import suneido.SuContainer;
import suneido.language.Ops;
import suneido.language.jsdi.DllInterface;
import suneido.language.jsdi.MarshallPlan;
import suneido.language.jsdi.Marshaller;
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

	@Override
	public void marshallIn(Marshaller marshaller, Object value) {
		final SuContainer c = Ops.toContainer(value);
		if (null == c) {
			marshaller.skipBasicArrayElements(numElems);
		} else {
			final BasicType type = getBasicType();
			// NOTE: We can get a far more efficient result if we add the
			//       ability to lock an SuContainer (by locking both the
			//       vector and the map) and then use vecSize() and vecGet()
			for (int k = 0; k < numElems; ++k) {
				Object elem = c.get(k);
				if (null == elem) {
					marshaller.skipBasicArrayElements(1);
					continue;
				}
				switch (type) {
				// With any luck the JIT compiler will optimize  this by pulling
				// the switch statement outside the loop...
				case BOOL:
					marshaller.putBool(Ops.toBoolean_(elem));
					break;
				case CHAR:
					marshaller.putChar((byte)Ops.toInt(elem));
					break;
				case SHORT:
					marshaller.putShort((short)Ops.toInt(elem));
					break;
				case GDIOBJ:
					// intentional fall-through
				case HANDLE:
					// intentional fall-through
				case LONG:
					marshaller.putLong(Ops.toInt(value));
					break;
				case INT64:
					marshaller.putInt64(NumberConversions.toLong(elem));
					break;
				case FLOAT:
					marshaller.putFloat(NumberConversions.toFloat(elem));
					break;
				case DOUBLE:
					marshaller.putDouble(NumberConversions.toDouble(elem));
					break;
				default:
					throw new IllegalStateException("unhandled BasicType in switch");
				}
			}
		}
	}
}
