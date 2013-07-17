package suneido.language.jsdi.type;

import suneido.language.Ops;
import suneido.language.jsdi.MarshallPlan;
import suneido.language.jsdi.Marshaller;
import suneido.language.jsdi.StorageType;

public final class BasicPointer extends Type {

	//
	// DATA
	//

	private final BasicValue underlying;

	//
	// CONSTRUCTORS
	//

	BasicPointer(BasicValue underlying) {
		super(TypeId.BASIC, StorageType.POINTER, pointerPlan(underlying
				.getBasicType().getMarshallPlan()));
		assert null != underlying : "Underlying BasicValue may not be null";
		this.underlying = underlying;
	}

	private static MarshallPlan pointerPlan(MarshallPlan valuePlan) {
		return MarshallPlan.makePointerPlan(valuePlan);
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

	//
	// ANCESTOR CLASS: Type
	//

	@Override
	public String getDisplayName() {
		return getBasicType().toIdentifier() + '*';
	}

	@Override
	public void marshallIn(Marshaller marshaller, Object value) {
		if (null == value) {
			marshaller.putNullPtr();
			marshaller.skipBasicArrayElements(1);
		} else {
			switch (getBasicType()) {
			case BOOL:
				marshaller.putBoolPtr(Ops.toBoolean_(value));
				break;
			case CHAR:
				marshaller.putCharPtr((byte)Ops.toInt(value));
				break;
			case SHORT:
				marshaller.putShortPtr((short)Ops.toInt(value));
				break;
			case GDIOBJ:
				// intentional fall-through
			case HANDLE:
				// intentional fall-through
			case LONG:
				marshaller.putLongPtr(Ops.toInt(value));
				break;
			case INT64:
				marshaller.putInt64Ptr(NumberConversions.toLong(value));
				break;
			case FLOAT:
				marshaller.putFloatPtr(NumberConversions.toFloat(value));
				break;
			case DOUBLE:
				marshaller.putDoublePtr(NumberConversions.toDouble(value));
				break;
			default:
				throw new IllegalStateException("unhandled BasicType in switch");
			}
		}
	}
}
