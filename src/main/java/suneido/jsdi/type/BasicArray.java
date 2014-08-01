/* Copyright 2013 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido.jsdi.type;

import static suneido.SuInternalError.unhandledEnum;

import javax.annotation.concurrent.Immutable;

import suneido.SuContainer;
import suneido.jsdi.DllInterface;
import suneido.jsdi.StorageType;
import suneido.jsdi.marshall.MarshallPlanBuilder;
import suneido.jsdi.marshall.Marshaller;
import suneido.jsdi.marshall.NumberConversions;
import suneido.jsdi.marshall.ObjectConversions;
import suneido.language.Ops;

/**
 * Represents an array of an underlying basic type, such as
 * <code><b>int8</b>&#8288;[10]</code>.
 *
 * @author Victor Schappert
 * @since 20130625
 * @see BasicValue
 * @see StringDirect
 */
@DllInterface
@Immutable
public final class BasicArray extends Type {

	//
	// DATA
	//

	private final BasicValue underlying;
	private final int numElems;

	//
	// CONSTRUCTORS
	//

	/**
	 * Constructs a basic array type given an underlying basic value and a
	 * positive element count.
	 *
	 * @param underlying Underlying basic type
	 * @param numElems Element count: <code>0 &lt; numElems</code>
	 */
	public BasicArray(BasicValue underlying, int numElems) {
		super(TypeId.BASIC, StorageType.ARRAY);
		this.underlying = underlying;
		this.numElems = numElems;
	}

	//
	// ACCESSORS
	//

	/**
	 * Returns the underlying basic type.
	 *
	 * @return Underlying basic type
	 */
	public BasicType getBasicType() {
		return underlying.getBasicType();
	}

	//
	// ANCESTOR CLASS: Type
	//

	@Override
	public String getDisplayName() {
		StringBuilder result = new StringBuilder(16);
		result.append(getBasicType().getName()).append('[')
				.append(numElems).append(']');
		return result.toString();
	}

	@Override
	public int getSizeDirect() {
		return underlying.getBasicType().getSize() * numElems;
	}

	@Override
	public int getAlignDirect() {
		return underlying.getSizeDirect();
	}

	@Override
	public void addToPlan(MarshallPlanBuilder builder, boolean isCallbackPlan) {
		builder.arrayBegin();
		for (int k = 0; k < numElems; ++k) {
			underlying.addToPlan(builder, isCallbackPlan);
		}
		builder.arrayEnd();
	}

	@Override
	public void marshallIn(Marshaller marshaller, Object value) {
		final SuContainer c = Ops.toContainer(value);
		if (null == c) {
			marshaller.skipBasicArrayElements(numElems);
		} else {
			final BasicType type = getBasicType();
			// NOTE: We can get a more efficient result if we add the ability to
			//       lock an SuContainer (by locking both the vector and the
			//       map) and then use vecSize() and vecGet()
			for (int k = 0; k < numElems; ++k) {
				Object elem = c.get(k);
				if (null == elem) {
					marshaller.skipBasicArrayElements(1);
					continue;
				}
				switch (type) {
				// With any luck the JIT compiler will optimize this by pulling
				// the switch statement outside the loop...
				case BOOL:
					marshaller.putBool(Ops.toBoolean_(elem));
					break;
				case INT8:
					marshaller.putInt8((byte)Ops.toInt(elem));
					break;
				case INT16:
					marshaller.putInt16((short)Ops.toInt(elem));
					break;
				case GDIOBJ:
					// intentional fall-through
				case HANDLE:
					// intentional fall-through
				case OPAQUE_POINTER:
					// intentional fall-through
				case INT32:
					marshaller.putInt32(Ops.toInt(elem));
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
					throw unhandledEnum(BasicType.class);
				}
			}
		}
	}

	@Override
	public Object marshallOut(Marshaller marshaller, Object oldValue) {
		final SuContainer c = ObjectConversions.containerOrThrow(oldValue, numElems);
		final BasicType type = getBasicType();
		for (int k = 0; k < numElems; ++k) {
			switch (type) {
			// With any luck the JIT compiler will optimize this by pulling
			// the switch statement outside the loop...
			case BOOL:
				c.insert(k, marshaller.getBool());
				break;
			case INT8:
				c.insert(k, (int)marshaller.getInt8());
				break;
			case INT16:
				c.insert(k, (int)marshaller.getInt16());
				break;
			case GDIOBJ:
				// intentional fall-through
			case HANDLE:
				// intentional fall-through
			case OPAQUE_POINTER:
				// intentional fall-through
			case INT32:
				c.insert(k, marshaller.getInt32());
				break;
			case INT64:
				c.insert(k, marshaller.getInt64());
				break;
			case FLOAT:
				c.insert(k, marshaller.getFloat()); // TODO: this should insert a BigDecimal, not a float
				break;
			case DOUBLE:
				c.insert(k, marshaller.getDouble()); // TODO: this should insert a BigDecimal, not a double
				break;
			default:
				throw unhandledEnum(BasicType.class);
			}
		}
		return c;
	}

	@Override
	public void skipMarshalling(Marshaller marshaller) {
		marshaller.skipBasicArrayElements(numElems);
	}
}
