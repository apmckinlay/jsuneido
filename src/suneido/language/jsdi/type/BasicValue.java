package suneido.language.jsdi.type;

import suneido.language.Ops;
import suneido.language.jsdi.DllInterface;
import suneido.language.jsdi.Marshaller;
import suneido.language.jsdi.StorageType;

// TODO doc
@DllInterface
public final class BasicValue extends Type {

	//
	// DATA
	//

	private final BasicPointer pointerType;
	private final BasicType basicType;

	//
	// CONSTRUCTORS
	//

	BasicValue(BasicType basicType) {
		super(TypeId.BASIC, StorageType.VALUE, basicType.getMarshallPlan());
		this.basicType = basicType;
		this.pointerType = new BasicPointer(this);
	}

	//
	// ACCESSORS
	//

	public BasicPointer getPointerType() {
		return pointerType;
	}

	public BasicType getBasicType() {
		return basicType;
	}

	//
	// ANCESTOR CLASS: Type
	//

	@Override
	public String getDisplayName() {
		return basicType.toIdentifier();
	}

	@Override
	public void marshallIn(Marshaller marshaller, Object value) {
		if (null == value) {
			marshaller.skipBasicArrayElements(1);
		} else {
			switch (basicType) {
			case BOOL:
				marshaller.putBool(Ops.toBoolean_(value));
				break;
			case CHAR:
				marshaller.putChar((byte)Ops.toInt(value));
				break;
			case SHORT:
				marshaller.putShort((short)Ops.toInt(value));
				break;
			case GDIOBJ:
				// intentional fall-through
			case HANDLE:
				// intentional fall-through
			case LONG:
				marshaller.putLong(Ops.toInt(value));
				break;
			case INT64:
				marshaller.putInt64(NumberConversions.toLong(value));
				break;
			case FLOAT:
				marshaller.putFloat(NumberConversions.toFloat(value));
				break;
			case DOUBLE:
				marshaller.putDouble(NumberConversions.toDouble(value));
				break;
			default:
				throw new IllegalStateException("unhandled BasicType in switch");
			}
		}
	}
}
