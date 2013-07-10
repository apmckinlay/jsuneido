package suneido.language.jsdi.type;

import suneido.language.jsdi.MarshallPlan;
import suneido.language.jsdi.StorageType;

/**
 * Pseudo-type representing the "type" of the return value of a {@code void}
 * function.
 * @author Victor Schappert
 * @since 20130707
 */
public final class VoidType extends Type {

	//
	// CONSTRUCTORS
	//

	private VoidType() {
		super(TypeId.VOID, StorageType.VALUE, (MarshallPlan)null);
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
}
