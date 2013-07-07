package suneido.language.jsdi.type;

import suneido.SuValue;
import suneido.language.jsdi.JSDIException;
import suneido.language.jsdi.StorageType;

public abstract class ComplexType extends Type {

	//
	// DATA
	//

	protected final String   suTypeName;
	protected final TypeList typeList;

	//
	// CONSTRUCTORS
	//

	protected ComplexType(TypeId typeId, String suTypeName, TypeList typeList) {
		super(typeId, StorageType.VALUE, typeList.getMarshallPlan());
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
	 */
	public String getSuTypeName()
	{
		return suTypeName;
	}

	//
	// MUTATORS
	//

	final boolean resolve(int level) {
		try {
			final boolean changed = typeList.resolve(level);
			if (null == marshallPlan || changed) {
				marshallPlan = typeList.getMarshallPlan();
				return true;
			} else {
				return false;
			}
		} catch (ProxyResolveException e) {
			if (TypeId.STRUCT == getTypeId()) {
				e.setMemberType("member");
			} else {
				assert TypeId.CALLBACK == getTypeId();
				e.setMemberType("parameter");
			}
			e.setParentName(suTypeName);
			throw new JSDIException(e);
		}
	}
}