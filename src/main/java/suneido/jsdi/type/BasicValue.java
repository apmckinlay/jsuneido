/* Copyright 2013 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido.jsdi.type;

import static suneido.SuInternalError.unhandledEnum;

import javax.annotation.concurrent.Immutable;

import suneido.SuInternalError;
import suneido.jsdi.DllInterface;
import suneido.jsdi.StorageType;
import suneido.jsdi.marshall.Marshaller;
import suneido.jsdi.marshall.NumberConversions;
import suneido.jsdi.marshall.PrimitiveSize;
import suneido.language.Numbers;
import suneido.language.Ops;

/**
 * Represents a value type based on an underlying basic type, for example
 * <code><b>int32</b></code>.
 *
 * @author Victor Schappert
 * @see BasicArray
 * @see BasicType
 */
@DllInterface
@Immutable
public final class BasicValue extends Type {

	//
	// DATA
	//

	private final BasicType basicType;

	//
	// CONSTRUCTORS
	//

	/**
	 * Constructs a basic value based on an underlying basic type.
	 *
	 * @param basicType Basic type underlying the value type
	 */
	public BasicValue(BasicType basicType) {
		super(TypeId.BASIC, StorageType.VALUE);
		this.basicType = basicType;
	}

	//
	// INTERNALS
	//

	public static long marshallPointerInToLong(Object value) {
		if (Long.BYTES == PrimitiveSize.POINTER) {
			return NumberConversions.toPointer64(value);
		} else if (Integer.BYTES == PrimitiveSize.POINTER) {
			return NumberConversions.toPointer32(value);
		} else {
			throw new SuInternalError("unsupported pointer size: "
					+ PrimitiveSize.POINTER);
		}
	}

	//
	// ACCESSORS
	//

	/**
	 * Returns the basic type of this.
	 *
	 * @return Basic type
	 */
	public BasicType getBasicType() {
		return basicType;
	}

	//
	// ANCESTOR CLASS: Type
	//

	@Override
	public String getDisplayName() {
		return basicType.getName();
	}

	@Override
	public boolean isMarshallableToLong() {
		return true; 
	}

	@Override
	public int getSizeDirect() {
		return basicType.getSize();
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
			case INT8:
				marshaller.putInt8((byte)Ops.toInt(value));
				break;
			case INT16:
				marshaller.putInt16((short)Ops.toInt(value));
				break;
			case INT32:
				marshaller.putInt32(Ops.toInt(value));
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
			case GDIOBJ:
				// intentional fall-through
			case HANDLE:
				// intentional fall-through
			case OPAQUE_POINTER:
				marshaller.putPointerSizedInt(NumberConversions.toLong(value));
				break;
			default:
				throw unhandledEnum(BasicType.class);
			}
		}
	}

	@Override
	public Object marshallOut(Marshaller marshaller, Object oldValue) {
		switch (basicType) {
		case BOOL:
			return marshaller.getBool();
		case INT8:
			return marshaller.getInt8();
		case INT16:
			return marshaller.getInt16();
		case INT32:
			return marshaller.getInt32();
		case INT64:
			return marshaller.getInt64();
		case FLOAT:
			return Numbers.toBigDecimal(marshaller.getFloat());
		case DOUBLE:
			return Numbers.toBigDecimal(marshaller.getDouble());
		case GDIOBJ:
			// intentional fall-through
		case HANDLE:
			// intentional fall-through
		case OPAQUE_POINTER:
			return marshaller.getPointerSizedInt();
		default:
			throw unhandledEnum(BasicType.class);
		}
	}

	@Override
	public void skipMarshalling(Marshaller marshaller) {
		marshaller.skipBasicArrayElements(1);
	}

	@Override
	public void marshallInReturnValue(Marshaller marshaller) {
		// Do nothing
	}

	@Override
	public Object marshallOutReturnValue(long returnValue, Marshaller marshaller) {
		switch (basicType) {
		case BOOL:
			return 0L == returnValue ? Boolean.FALSE : Boolean.TRUE;
		case INT8:
			// Cast once to truncate, twice to box the result into an Integer.
			return (int)(byte)returnValue;
		case INT16:
			// Cast once to truncate, twice to box the result into an Integer.
			return (int)(short)returnValue;
		case INT32:
			return (int)returnValue;
		case GDIOBJ:
			// intentional fall-through
		case HANDLE:
			// intentional fall-through
		case OPAQUE_POINTER:
			// intentional fall-through
		case INT64:
			return returnValue;
		case FLOAT:
			// intentional fall-through
			// Floating-point return values from the native side are always
			// doubles.
		case DOUBLE:
			return Numbers.toBigDecimal(Double.longBitsToDouble(returnValue));
		default:
			throw unhandledEnum(BasicType.class);
		}
	}

	@Override
	public long marshallInToLong(Object value) {
		switch (basicType) {
		case BOOL:
			return Ops.toBoolean_(value) ? 1L : 0L;
		case INT8:
			return (long)Ops.toInt(value) & 0xffL; 
		case INT16:
			return (long)Ops.toInt(value) & 0xffffL;
		case INT32:
			return (long)Ops.toInt(value) & 0xffffffffL;
		case GDIOBJ:
			// intentional fall-through
		case HANDLE:
			// intentional fall-through
		case OPAQUE_POINTER:
			// intentional fall-through
			return marshallPointerInToLong(value);
		case INT64:
			return NumberConversions.toLong(value);
		case FLOAT:
			return 0xffffffffL &
					(long)Float.floatToRawIntBits(NumberConversions.toFloat(value));
		case DOUBLE:
			return Double.doubleToRawLongBits(NumberConversions.toDouble(value));
		default:
			throw unhandledEnum(BasicType.class);
		}
	}

	@Override
	public Object marshallOutFromLong(long marshalledData, Object oldValue) {
		switch (basicType) {
		case BOOL:
			return 0L != (marshalledData & 0xffffffffL);  
		case INT8:
			return (int)(byte)marshalledData; 
		case INT16:
			return (int)(short)marshalledData;
		case INT32:
			return (int)marshalledData;
		case GDIOBJ:
			// intentional fall-through
		case HANDLE:
			// intentional fall-through
		case OPAQUE_POINTER:
			// intentional fall-through
		case INT64:
			return marshalledData;
		case FLOAT:
			return Numbers.toBigDecimal(Float.intBitsToFloat((int)marshalledData));
		case DOUBLE:
			return Numbers.toBigDecimal(Double.longBitsToDouble(marshalledData));
		default:
			throw unhandledEnum(BasicType.class);
		}
	}
}
