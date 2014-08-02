/* Copyright 2013 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido.jsdi.type;

import static suneido.SuInternalError.unhandledEnum;
import suneido.SuContainer;
import suneido.jsdi.DllInterface;
import suneido.jsdi.StorageType;
import suneido.jsdi.marshall.ElementSkipper;
import suneido.jsdi.marshall.MarshallPlanBuilder;
import suneido.jsdi.marshall.Marshaller;
import suneido.jsdi.marshall.ObjectConversions;
import suneido.jsdi.marshall.PrimitiveSize;
import suneido.language.Context;
import suneido.language.Ops;

/**
 * <p>
 * Indirect reference to a type via a name. Because Suneido is a dynamic
 * language, the underlying type bound to a LateBind's name may change as the
 * program executes.
 * </p>
 * 
 * @author Victor Schappert
 * @since 20130625
 */
@DllInterface
public final class LateBinding extends Type {

	//
	// DATA
	//

	/**
	 * <p>
	 * Naming context for looking up the Suneido language object which this
	 * late binding type refers to (hopefully it is a {@link Structure} or
	 * {@link Callback}, or else the user will get a runtime error).
	 * </p>
	 * <p>
	 * Conceptually, the context variable <em>could</em> be carried around
	 * either by this class or, in the alternative, by {@link TypeList}.
	 * However, because the vast majority of existing Suneido structs (as of
	 * 20130702) are relatively flat (<em>ie</em> they don't contain any
	 * late-binding types at all), the system will be more lightweight in
	 * general if the context belongs to the late-binding type.
	 * </p>
	 */
	private final Context context;
	private final int typeNameSlot;
	private final StorageType storageType;
	private final int numElems;
	private ComplexType lastBoundType;
	private ElementSkipper skipper;

	//
	// CONSTRUCTORS
	//

	public LateBinding(Context context, int typeNameSlot, StorageType storageType,
			int numElems) {
		super(TypeId.LATE_BINDING, storageType);
		this.context = context;
		this.typeNameSlot = typeNameSlot;
		this.storageType = storageType;
		this.numElems = numElems;
		this.lastBoundType = null;
		this.skipper = null;
	}

	//
	// ACCESSORS
	//

	String getUnderlyingTypeName() {
		return context.nameForSlot(typeNameSlot);
	}

	/**
	 * Returns the type last bound to this late-binding type.
	 *
	 * @return Last type bound by {@link #bind(int)}, or <code>null</code> if
	 *         there was no last bind or the last bind threw an exception
	 * @since 20140730
	 */
	public ComplexType getLastBoundType() {
		return lastBoundType;
	}

	//
	// ANCESTOR CLASS: Type
	//

	@Override
	public boolean isClosed() {
		return false;
	}

	@Override
	public String getDisplayName() {
		final String typeName = getUnderlyingTypeName();
		switch (storageType) {
		case VALUE:
			return typeName;
		case POINTER:
			return typeName + '*';
		case ARRAY:
			StringBuilder sb = new StringBuilder(24);
			return sb.append(typeName).append('[').append(numElems).append(']')
					.toString();
		default:
			throw unhandledEnum(StorageType.class);
		}
	}

	@Override
	public int getSizeDirect() {
		switch (storageType) {
		case VALUE:
			return lastBoundType.getSizeDirect();
		case ARRAY:
			return lastBoundType.getSizeDirect() * numElems;
		case POINTER:
			return PrimitiveSize.POINTER;
		default:
			throw unhandledEnum(StorageType.class);
		}
	}

	@Override
	public int getAlignDirect() {
		switch (storageType) {
		case VALUE: // fall through
		case ARRAY:
			return lastBoundType.getAlignDirect();
		case POINTER:
			return PrimitiveSize.POINTER;
		default:
			throw unhandledEnum(StorageType.class);
		}
	}

	@Override
	public int getSizeIndirect() {
		switch (storageType) {
		case VALUE:
			return lastBoundType.getSizeIndirect();
		case ARRAY:
			return lastBoundType.getSizeIndirect() * numElems;
		case POINTER:
			return lastBoundType.getSizeDirect() +
					lastBoundType.getSizeIndirect();
		default:
			throw unhandledEnum(StorageType.class);
		}
	}

	@Override
	public int getVariableIndirectCount() {
		switch (storageType) {
		case VALUE: // fall through
		case POINTER:
			return lastBoundType.getVariableIndirectCount();
		case ARRAY:
			return numElems * lastBoundType.getVariableIndirectCount();
		default:
			throw unhandledEnum(StorageType.class);
		}
	}

	@Override
	public void addToPlan(MarshallPlanBuilder builder, boolean isCallbackPlan) {
		switch (storageType) {
		case VALUE:
			lastBoundType.addToPlan(builder, isCallbackPlan);
			skipper = lastBoundType.skipper;
			break;
		case ARRAY:
			builder.arrayBegin();
			for (int k = 0; k < numElems; ++k) {
				// NOTE: This is doing a lot of extra work that could as easily
				//       be done by multiplication... Not ideal.
				lastBoundType.addToPlan(builder, isCallbackPlan);
			}
			skipper = builder.arrayEnd();
			break;
		case POINTER:
			builder.ptrBegin(lastBoundType.getSizeDirect(),
					lastBoundType.getAlignDirect());
			lastBoundType.addToPlan(builder, isCallbackPlan);
			builder.ptrEnd();
			skipper = lastBoundType.skipper;
			break;
		}
	}

	@Override
	public void marshallIn(Marshaller marshaller, Object value) {
		switch (storageType) {
		case VALUE:
			if (null != value) {
				lastBoundType.marshallIn(marshaller, value);
			} else {
				marshaller.skipComplexElement(skipper);
			}
			break;
		case POINTER:
			if (! isNullPointerEquivalent(value)) {
				marshaller.putPtr();
				lastBoundType.marshallIn(marshaller, value);
			} else {
				marshaller.putNullPtr();
				marshaller.skipComplexElement(skipper);
			}
			break;
		case ARRAY:
			final SuContainer c = Ops.toContainer(value);
			if (null != c) {
				for (int k = 0; k < numElems; ++k) {
					value = c.get(k);
					lastBoundType.marshallIn(marshaller, value);
				}
			} else {
				marshaller.skipComplexElement(skipper);
			}
			break;
		default:
			throw unhandledEnum(StorageType.class);
		}
	}

	@Override
	public Object marshallOut(Marshaller marshaller, Object oldValue) {
		switch (storageType) {
		case VALUE:
			return lastBoundType.marshallOut(marshaller, oldValue);
		case POINTER:
			if (marshaller.isPtrNull()) {
				marshaller.skipComplexElement(skipper);
				return Boolean.FALSE;
			} else {
				if (! (oldValue instanceof SuContainer) &&
						isNullPointerEquivalent(oldValue))
					oldValue = null;
				return lastBoundType.marshallOut(marshaller, oldValue);
			}
		case ARRAY:
			final SuContainer c = ObjectConversions.containerOrThrow(oldValue,
					numElems);
			if (c == oldValue) {
				for (int k = 0; k < numElems; ++k) {
					oldValue = c.getIfPresent(k);
					Object newValue = lastBoundType.marshallOut(marshaller,
							oldValue);
					if (!newValue.equals(oldValue)) {
						c.insert(k, newValue);
					}
				}
			} else {
				for (int k = 0; k < numElems; ++k) {
					Object newValue = lastBoundType.marshallOut(marshaller,
							null);
					c.insert(k, newValue);
				}
			}
			return c;
		default:
			throw unhandledEnum(StorageType.class);
		}
	}

	@Override
	public void skipMarshalling(Marshaller marshaller) {
		switch (storageType) {
		case VALUE:
		case ARRAY:
			marshaller.skipComplexElement(skipper);
			break;
		case POINTER:
			super.skipMarshalling(marshaller); // Will throw
		}
	}

	@Override
	public void putMarshallOutInstruction(Marshaller marshaller) {
		lastBoundType.putMarshallOutInstruction(marshaller);
	}

	@Override
	public final boolean bind(int level) throws BindException {
		final Object maybeType = context.tryget(typeNameSlot);
		if (null != maybeType) {
			if (maybeType == lastBoundType) {
				return lastBoundType.bind(level);
			} else if (maybeType instanceof ComplexType) {
				// TODO: [CONCURRENCY]. At the point at which we detect this
				//                      change for the first time, we'll need
				//                      to lock some kind of global lock (might
				//                      be best to pass it as a parameter) so
				//                      that concurrent threads don't mess up
				//                      the same types
				lastBoundType = (ComplexType) maybeType;
				lastBoundType.bind(level);
				return true;
			}
		}
		final Class<?> clazz = null == maybeType ? null : maybeType.getClass();
		throw new BindException(this, clazz);
	}
}
