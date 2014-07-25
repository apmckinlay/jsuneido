/* Copyright 2013 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido.jsdi.type;

import static suneido.SuInternalError.unhandledEnum;

import javax.annotation.concurrent.Immutable;

import suneido.jsdi.DllInterface;
import suneido.jsdi.Marshaller;
import suneido.jsdi.NumberConversions;
import suneido.jsdi.StorageType;
import suneido.language.Numbers;
import suneido.language.Ops;

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

	public BasicValue(BasicType basicType) {
		super(TypeId.BASIC, StorageType.VALUE);
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
		return basicType.getName();
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
		case GDIOBJ:
			// intentional fall-through
		case HANDLE:
			// intentional fall-through
		case OPAQUE_POINTER:
			// intentional fall-through
		case INT32:
			return (int)returnValue;
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
}
