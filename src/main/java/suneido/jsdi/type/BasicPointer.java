/* Copyright 2013 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido.jsdi.type;

import static suneido.SuInternalError.unhandledEnum;
import suneido.SuInternalError;
import suneido.jsdi.DllInterface;
import suneido.jsdi.MarshallPlanBuilder;
import suneido.jsdi.Marshaller;
import suneido.jsdi.PrimitiveSize;
import suneido.jsdi.StorageType;

@Deprecated // pointers to basic value types are ... pointless (wrap in a struct if you desperately need one)
@DllInterface
public final class BasicPointer extends Type {

	//
	// DATA
	//

	private final BasicValue underlying;

	//
	// CONSTRUCTORS
	//

	BasicPointer(BasicValue underlying) {
		super(TypeId.BASIC, StorageType.POINTER);
		assert null != underlying : "Underlying BasicValue may not be null";
		this.underlying = underlying;
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
		return getBasicType().getName() + '*';
	}

	@Override
	public int getSizeDirect() {
		return PrimitiveSize.POINTER;
	}

	@Override
	public int getSizeIndirect() {
		return underlying.getSizeDirect();
	}

	@Override
	public void addToPlan(MarshallPlanBuilder builder, boolean isCallbackPlan) {
		final int size = underlying.getSizeDirect();
		builder.ptrBasic(size, size);
	}

	@Override
	public void marshallIn(Marshaller marshaller, Object value) {
		if (null == value) {
			marshaller.putNullPtr();
			marshaller.skipBasicArrayElements(1);
		} else {
			switch (getBasicType()) {
//			case BOOL:
//				marshaller.putBoolPtr(Ops.toBoolean_(value));
//				break;
//			case INT8:
//				marshaller.putCharPtr((byte)Ops.toInt(value));
//				break;
//			case INT16:
//				marshaller.putShortPtr((short)Ops.toInt(value));
//				break;
//			case GDIOBJ:
//				// intentional fall-through
//			case HANDLE:
//				// intentional fall-through
//			case INT32:
//				marshaller.putLongPtr(Ops.toInt(value));
//				break;
//			case INT64:
//				marshaller.putInt64Ptr(NumberConversions.toLong(value));
//				break;
//			case FLOAT:
//				marshaller.putFloatPtr(NumberConversions.toFloat(value));
//				break;
//			case DOUBLE:
//				marshaller.putDoublePtr(NumberConversions.toDouble(value));
//				break;
			default:
				throw unhandledEnum(BasicType.class);
			}
		}
	}

	@Override
	public Object marshallOut(Marshaller marshaller, Object oldValue) {
		throw new SuInternalError("BasicPointer is deprecated");
	}
}
