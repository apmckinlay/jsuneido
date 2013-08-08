package suneido.language.jsdi.type;

import javax.annotation.concurrent.Immutable;

import suneido.language.jsdi.*;

/**
 * Pseudo-type representing the "type" of the return value of a {@code void}
 * function.
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
		throw new IllegalStateException(getDisplayName()
				+ " has no intrinsic size");
	}

	@Override
	public int getSizeDirectWholeWords() {
		throw new IllegalStateException(getDisplayName()
				+ " has no whole word size");
	}

	@Override
	public int getSizeIndirect() {
		throw new IllegalStateException(getDisplayName()
				+ " has no indirect size");
	}

	@Override
	public void addToPlan(MarshallPlanBuilder builder, boolean isCallbackPlan) {
		throw new IllegalStateException(getDisplayName()
				+ " cannot be added to a marshall plan");
	}
}
