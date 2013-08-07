package suneido.language.jsdi.type;

import suneido.SuValue;
import suneido.language.jsdi.DllInterface;
import suneido.language.jsdi.ElementSkipper;
import suneido.language.jsdi.StorageType;

/**
 * TODO: docs
 * @author Victor Schappert
 * @since 20130625
 */
@DllInterface
public abstract class ComplexType extends Type {

	//
	// DATA
	//

	protected final String         suTypeName;
	protected final TypeList       typeList;
	protected       ElementSkipper skipper; // only valid to last addToPlan()

	//
	// CONSTRUCTORS
	//

	protected ComplexType(TypeId typeId, String suTypeName, TypeList typeList) {
		super(typeId, StorageType.VALUE);
		if (null == suTypeName)
			throw new IllegalArgumentException("suTypeName cannot be null");
		this.suTypeName = suTypeName;
		this.typeList = typeList;
	}

	//
	// ACCESSORS
	//

	/**
	 * Returns the Suneido type name.
	 * 
	 * <p>
	 * For example, for a global {@code struct} in a library record called 'A',
	 * the returned value is "A". However, the value returned is not necessarily
	 * a global name. For anonymous types, the Suneido type name is an
	 * arbitrary value assigned by the compiler.
	 * </p>
	 *
	 * <p>
	 * This method is called {@code getSuTypeName()} to differentiate it from
	 * {@link SuValue#typeName()}, which relates to the JSuneido universe rather
	 * than the user's type naming universe. 
	 * </p>
	 *
	 * @return Suneido type name
	 * @see suneido.language.jsdi.dll.Dll#getSuTypeName()
	 */
	public final String getSuTypeName() {
		return suTypeName;
	}

	// TODO: Docs since 20130725
	public final ElementSkipper getElementSkipper() {
		return skipper;
	}

	//
	// MUTATORS
	//

	// TODO: docs
	boolean resolve(int level) {
		return false;
	}
}
