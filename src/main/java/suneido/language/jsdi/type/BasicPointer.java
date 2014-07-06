/* Copyright 2013 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido.language.jsdi.type;

import suneido.language.jsdi.DllInterface;
import suneido.language.jsdi.MarshallPlanBuilder;
import suneido.language.jsdi.Marshaller;
import suneido.language.jsdi.StorageType;

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
		return getBasicType().toIdentifier() + '*';
	}

	@Override
	public int getSizeDirectIntrinsic() {
		return PrimitiveSize.POINTER;
	}

	@Override
	public int getSizeDirectWholeWords() {
		return PrimitiveSize.pointerWholeWordBytes();
	}

	@Override
	public int getSizeIndirect() {
		return underlying.getSizeDirectIntrinsic();
	}

	@Override
	public void addToPlan(MarshallPlanBuilder builder, boolean isCallbackPlan) {
		builder.ptrBasic(underlying.getSizeDirectIntrinsic());
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
				throw new IllegalStateException("unhandled BasicType in switch");
			}
		}
	}

	@Override
	public Object marshallOut(Marshaller marshaller, Object oldValue) {
		throw new IllegalStateException("BasicPointer is deprecated");
	}
}
