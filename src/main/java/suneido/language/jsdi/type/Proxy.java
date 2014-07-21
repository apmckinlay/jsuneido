/* Copyright 2013 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido.language.jsdi.type;

import static suneido.SuInternalError.unhandledEnum;
import suneido.SuContainer;
import suneido.language.Context;
import suneido.language.Ops;
import suneido.language.jsdi.DllInterface;
import suneido.language.jsdi.ElementSkipper;
import suneido.language.jsdi.MarshallPlanBuilder;
import suneido.language.jsdi.Marshaller;
import suneido.language.jsdi.ObjectConversions;
import suneido.language.jsdi.PrimitiveSize;
import suneido.language.jsdi.StorageType;

/**
 * TODO: docs
 * @author Victor Schappert
 * @since 20130625
 */
@DllInterface
public final class Proxy extends Type {

	//
	// DATA
	//

	/**
	 * <p>
	 * Naming context for looking up the Suneido language object which this
	 * proxy refers to (hopefully it is a {@link Structure} or {@link Callback},
	 * or else the user will get a runtime error).
	 * </p>
	 * <p>
	 * Conceptually, the context variable <em>could</em> be carried around
	 * either by this class or, in the alternative, by {@link TypeList}.
	 * However, because the vast majority of existing Suneido structs (as of
	 * 20130702) are relatively flat (<em>ie</em> they don't contain any proxies
	 * at all), the system will be more lightweight in general if the context
	 * belongs to the proxy.
	 * </p>
	 */
	private final Context context;
	private final int typeNameSlot;
	private final StorageType storageType;
	private final int numElems;
	private ComplexType lastResolvedType;
	private ElementSkipper skipper;

	//
	// CONSTRUCTORS
	//

	public Proxy(Context context, int typeNameSlot, StorageType storageType,
			int numElems) {
		super(TypeId.PROXY, storageType);
		this.context = context;
		this.typeNameSlot = typeNameSlot;
		this.storageType = storageType;
		this.numElems = numElems;
		this.lastResolvedType = null;
		this.skipper = null;
	}

	//
	// ACCESSORS
	//

	final boolean resolve(int level) throws ProxyResolveException {
		final Object maybeType = context.tryget(typeNameSlot);
		if (null != maybeType) {
			if (maybeType == lastResolvedType) {
				return lastResolvedType.resolve(level);
			} else if (maybeType instanceof ComplexType) {
				// TODO: [CONCURRENCY]. At the point at which we detect this
				//                      change for the first time, we'll need
				//                      to lock some kind of global lock (might
				//                      be best to pass it as a parameter) so
				//                      that concurrent threads don't mess up
				//                      the same types
				lastResolvedType = (ComplexType) maybeType;
				lastResolvedType.resolve(level);
				return true;
			}
		}
		final Class<?> clazz = null == maybeType ? null : maybeType.getClass();
		throw new ProxyResolveException(this, clazz);
	}

	final String getUnderlyingTypeName() {
		return context.nameForSlot(typeNameSlot);
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
	public int getSizeDirectIntrinsic() {
		switch (storageType) {
		case VALUE:
			return lastResolvedType.getSizeDirectIntrinsic();
		case ARRAY:
			return lastResolvedType.getSizeDirectIntrinsic() * numElems;
		case POINTER:
			return PrimitiveSize.POINTER;
		default:
			throw unhandledEnum(StorageType.class);
		}
	}

	@Override
	public int getSizeDirectWholeWords() {
		switch (storageType) {
		case VALUE:
			return lastResolvedType.getSizeDirectWholeWords();
		case ARRAY:
			return PrimitiveSize.sizeWholeWords(lastResolvedType
					.getSizeDirectIntrinsic() * numElems);
		case POINTER:
			return PrimitiveSize.pointerWholeWordBytes();
		default:
			throw unhandledEnum(StorageType.class);
		}
	}

	@Override
	public int getSizeIndirect() {
		switch (storageType) {
		case VALUE:
			return lastResolvedType.getSizeIndirect();
		case ARRAY:
			return lastResolvedType.getSizeIndirect() * numElems;
		case POINTER:
			return lastResolvedType.getSizeDirectIntrinsic() +
					lastResolvedType.getSizeIndirect();
		default:
			throw unhandledEnum(StorageType.class);
		}
	}

	@Override
	public int getVariableIndirectCount() {
		switch (storageType) {
		case VALUE: // fall through
		case POINTER:
			return lastResolvedType.getVariableIndirectCount();
		case ARRAY:
			return numElems * lastResolvedType.getVariableIndirectCount();
		default:
			throw unhandledEnum(StorageType.class);
		}
	}

	@Override
	public void addToPlan(MarshallPlanBuilder builder, boolean isCallbackPlan) {
		switch (storageType) {
		case VALUE:
			lastResolvedType.addToPlan(builder, isCallbackPlan);
			skipper = lastResolvedType.skipper;
			break;
		case ARRAY:
			builder.arrayBegin();
			for (int k = 0; k < numElems; ++k) {
				// NOTE: This is doing a lot of extra work that could as easily
				//       be done by multiplication... Not ideal.
				lastResolvedType.addToPlan(builder, isCallbackPlan);
			}
			skipper = builder.arrayEnd();
			break;
		case POINTER:
			builder.ptrBegin(lastResolvedType.getSizeDirectIntrinsic());
			lastResolvedType.addToPlan(builder, isCallbackPlan);
			builder.ptrEnd();
			skipper = lastResolvedType.skipper;
			break;
		}
	}

	@Override
	public void marshallIn(Marshaller marshaller, Object value) {
		switch (storageType) {
		case VALUE:
			if (null != value) {
				lastResolvedType.marshallIn(marshaller, value);
			} else {
				marshaller.skipComplexElement(skipper);
			}
			break;
		case POINTER:
			if (! isNullPointerEquivalent(value)) {
				marshaller.putPtr();
				lastResolvedType.marshallIn(marshaller, value);
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
					lastResolvedType.marshallIn(marshaller, value);
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
			return lastResolvedType.marshallOut(marshaller, oldValue);
		case POINTER:
			if (marshaller.isPtrNull()) {
				marshaller.skipComplexElement(skipper);
				return Boolean.FALSE;
			} else {
				if (! (oldValue instanceof SuContainer) &&
						isNullPointerEquivalent(oldValue))
					oldValue = null;
				return lastResolvedType.marshallOut(marshaller, oldValue);
			}
		case ARRAY:
			final SuContainer c = ObjectConversions.containerOrThrow(oldValue,
					numElems);
			if (c == oldValue) {
				for (int k = 0; k < numElems; ++k) {
					oldValue = c.getIfPresent(k);
					Object newValue = lastResolvedType.marshallOut(marshaller,
							oldValue);
					if (!newValue.equals(oldValue)) {
						c.insert(k, newValue);
					}
				}
			} else {
				for (int k = 0; k < numElems; ++k) {
					Object newValue = lastResolvedType.marshallOut(marshaller,
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
	public void putMarshallOutInstruction(Marshaller marshaller) {
		lastResolvedType.putMarshallOutInstruction(marshaller);
	}
}
