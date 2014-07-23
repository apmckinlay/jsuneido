/* Copyright 2013 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido.jsdi.type;

import javax.annotation.concurrent.Immutable;

import suneido.SuInternalError;
import suneido.jsdi.DllInterface;
import suneido.jsdi.MarshallPlanBuilder;
import suneido.jsdi.Marshaller;
import suneido.jsdi.StorageType;

/**
 * Pseudo-type representing the "type" of the return value of a {@code void}
 * function.
 * 
 * @author Victor Schappert
 * @since 20130707
 */
@DllInterface
@Immutable
public final class VoidType extends Type {

	//
	// CONSTRUCTORS
	//

	private VoidType() {
		super(TypeId.VOID, StorageType.VALUE);
	}

	//
	// STATICS
	//

	/**
	 * Singleton instance of VoidType.
	 */
	public static final VoidType INSTANCE = new VoidType();
	/**
	 * Identifier for VoidType. This is the string which the programmer writes
	 * in order to generate an instance of VoidType.
	 */
	public static final String IDENTIFIER = "void";

	//
	// ANCESTOR CLASS: Type
	//

	@Override
	public String getDisplayName() {
		return IDENTIFIER;
	}

	@Override
	public int getSizeDirectIntrinsic() {
		throw new SuInternalError(getDisplayName() + " has no intrinsic size");
	}

	@Override
	public int getSizeDirectWholeWords() {
		throw new SuInternalError(getDisplayName() + " has no whole word size");
	}

	@Override
	public int getSizeIndirect() {
		throw new SuInternalError(getDisplayName() + " has no indirect size");
	}

	@Override
	public void addToPlan(MarshallPlanBuilder builder, boolean isCallbackPlan) {
		throw new SuInternalError(getDisplayName()
				+ " cannot be added to a marshall plan");
	}

	@Override
	public void marshallInReturnValue(Marshaller marshaller) {
		// Do nothing
	}

	@Override
	public Object marshallOutReturnValue(long returnValue, Marshaller marshaller) {
		return null;
	}
}
