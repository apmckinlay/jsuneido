package suneido.language.jsdi.type;

import javax.annotation.concurrent.Immutable;

import suneido.language.Ops;
import suneido.language.jsdi.DllInterface;
import suneido.language.jsdi.Marshaller;
import suneido.language.jsdi.NumberConversions;
import suneido.language.jsdi.StorageType;

// TODO doc
@DllInterface
@Immutable
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

	@Override
	public Object marshallOut(Marshaller marshaller, Object oldValue) {
		switch (basicType) {
		case BOOL:
			return marshaller.getBool();
		case CHAR:
			return marshaller.getChar();
		case SHORT:
			return marshaller.getShort();
		case GDIOBJ:
			// intentional fall-through
		case HANDLE:
			// intentional fall-through
		case LONG:
			return marshaller.getLong();
		case INT64:
			return marshaller.getInt64();
		case FLOAT:
			return marshaller.getFloat(); // TODO: this should return a BigDecimal, not a float
		case DOUBLE:
			return marshaller.getDouble(); // TODO: this should return a BigDecimal, not a double
		default:
			throw new IllegalStateException("unhandled BasicType in switch");
		}
	}

	@Override
	public Object marshallOutReturnValue(int returnValue) {
		switch (basicType) {
		case BOOL:
			return 0 == returnValue ? Boolean.FALSE : Boolean.TRUE;
		case CHAR:
			// Cast once to truncate, twice to box the result into an Integer.
			return (int)(byte)returnValue;
		case SHORT:
			// Cast once to truncate, twice to box the result into an Integer.
			return (int)(short)returnValue;
		case GDIOBJ:
			// intentional fall-through
		case HANDLE:
			// intentional fall-through
		case LONG:
			return returnValue;
		case FLOAT:
			return Float.intBitsToFloat(returnValue);
		default:
			return super.marshallOutReturnValue(returnValue);
		}
	}

	@Override
	public Object marshallOutReturnValue(long returnValue) {
		switch (basicType) {
		case BOOL:
			return 0L == returnValue ? Boolean.FALSE : Boolean.TRUE;
		case CHAR:
			// Cast once to truncate, twice to box the result into an Integer.
			return (int)(byte)returnValue;
		case SHORT:
			// Cast once to truncate, twice to box the result into an Integer.
			return (int)(short)returnValue;
		case GDIOBJ:
			// intentional fall-through
		case HANDLE:
			// intentional fall-through
		case LONG:
			return (int)returnValue;
		case INT64:
			return returnValue;
		case FLOAT:
			return Float.intBitsToFloat((int)returnValue);
		case DOUBLE:
			return Double.longBitsToDouble(returnValue);
		default:
			throw new IllegalStateException("unhandled BasicType in switch");
		}
	}

	@Override
	public boolean isMarshallableToJSDILong() {
		return basicType.getMarshallPlan().getSizeDirect() == SizeDirect.LONG;
	}

	@Override
	public void marshallInToJSDILong(int[] target, int pos, Object value) {
		if (null != value) {
			switch (basicType) {
			case BOOL:
				target[pos] = Ops.toIntBool(value);
				break;
			case GDIOBJ:
				// intentional fall-through
			case HANDLE:
				// intentional fall-through
			case LONG:
				target[pos] = Ops.toInt(value);
				break;
			case FLOAT:
				target[pos] = Float.floatToRawIntBits(NumberConversions.toFloat(value));
				break;
			default:
				assert !isMarshallableToJSDILong();
				super.marshallInToJSDILong(target, pos, value);
			}
		}
	}
}
