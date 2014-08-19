/* Copyright 2014 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido.jsdi.abi.amd64;

import suneido.SuInternalError;
import suneido.jsdi.DllInterface;
import suneido.jsdi.StorageType;
import suneido.jsdi.marshall.MarshallPlanBuilder;
import suneido.jsdi.marshall.Marshaller;
import suneido.jsdi.type.BindException;
import suneido.jsdi.type.ComplexType;
import suneido.jsdi.type.LateBinding;
import suneido.jsdi.type.Proxy;
import suneido.jsdi.type.TypeId;

/**
 * <p>
 * Shim that wraps a pass-by-value structure type in order to change the
 * marshalling behavior to pass-by-pointer where required by the Windows x64
 * ABI.
 * </p>
 *
 * <p>
 * Note that since the actual concrete type underlying a late-binding type is
 * not known until it is bound, it can't be known whether a late-binding type is
 * a {@code struct} or not. Thus this shim doesn't directly wrap a structure
 * type and instead wraps a late-binding type.
 * </p>
 *
 * @author Victor Schappert
 * @since 20140730
 */
@DllInterface
final class ByValShim extends Proxy {

	//
	// DATA
	//

	private final LateBinding underlying;
	private       boolean     needsPassByPointer;

	//
	// CONSTRUCTORS
	//

	ByValShim(LateBinding underlying) {
		super(checkIsByVal(underlying));
		this.underlying = underlying;
		this.needsPassByPointer = false;
	}

	//
	// INTERNALS
	//

	private static LateBinding checkIsByVal(LateBinding underlying) {
		if (null == underlying) {
			throw new SuInternalError("underlying type can't be null");
		} else if (StorageType.VALUE != underlying.getStorageType()) {
			throw new SuInternalError("underlying type must be pass-by-value");
		} else {
			return underlying;
		}
	}

	private static boolean isPassByReferenceSize(int sizeDirect) {
		switch (sizeDirect) {
		case 1:
		case 2:
		case 4:
		case 8:
			return false;
		default:
			return true;
		}
	}

	//
	// ANCESTOR CLASS: Type
	//

	@Override
	public void addToPlan(MarshallPlanBuilder builder, boolean isCallbackPlan) {
		if (! needsPassByPointer) {
			super.addToPlan(builder, isCallbackPlan);
		} else {
			builder.ptrBegin(underlying.getSizeDirect(), underlying.getAlignDirect());
			underlying.addToPlan(builder, isCallbackPlan);
			builder.ptrEnd();
		}
	}

	@Override
	public void marshallIn(Marshaller marshaller, Object value) {
		if (needsPassByPointer) {
			marshaller.putPtr();
		}
		underlying.marshallIn(marshaller, value);
	}

	@Override
	public Object marshallOut(Marshaller marshaller, Object oldValue) {
		if (needsPassByPointer) {
			marshaller.isPtrNull();
		}
		return underlying.marshallOut(marshaller, oldValue);
	}

	@Override
	public void skipMarshalling(Marshaller marshaller) {
		if (needsPassByPointer) {
			marshaller.isPtrNull();
		}
		underlying.skipMarshalling(marshaller);
	}


	@Override
	public void putMarshallOutInstruction(Marshaller marshaller) {
		throw new SuInternalError("not implemented"); // TODO: implement
	}

	@Override
	public boolean bind(int level) throws BindException {
		final boolean changed = underlying.bind(level);
		final ComplexType lastBoundType = underlying.getLastBoundType();
		needsPassByPointer = TypeId.STRUCT == lastBoundType.getTypeId() &&
				isPassByReferenceSize(lastBoundType.getSizeDirect());
		return changed;
	}
}
