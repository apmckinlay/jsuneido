package suneido.language.jsdi.type;

import suneido.SuValue;
import suneido.language.jsdi.MarshallPlan;
import suneido.language.jsdi.Marshaller;
import suneido.language.jsdi.StorageType;

public abstract class Type extends SuValue {

	//
	// DATA
	//

	private final TypeId typeId;
	private final StorageType storageType;
	protected MarshallPlan marshallPlan;

	//
	// CONSTRUCTORS
	//

	protected Type(TypeId typeId, StorageType storageType,
			MarshallPlan marshallPlan) {
		if (null == typeId)
			throw new IllegalArgumentException("typeId may not be null");
		if (null == storageType)
			throw new IllegalArgumentException("storageType may not be null");
		this.typeId = typeId;
		this.storageType = storageType;
		this.marshallPlan = marshallPlan;
	}

	//
	// ACCESSORS
	//

	public final TypeId getTypeId() {
		return typeId;
	}

	public final StorageType getStorageType() {
		return storageType;
	}

	public final MarshallPlan getMarshallPlan() {
		return marshallPlan;
	}

	/**
	 * Return a human-readable name for the type. This is the value which will
	 * be printed for a user of the built-in Suneido function {@code Display()}.
	 * The {@link #toString()} method is deliberately not overridden because it
	 * can be useful in debugging to have more information.
	 * 
	 * @return Human readable display name.
	 * @author Victor Schappert
	 * @since 20130628
	 */
	public abstract String getDisplayName();

	/**
	 * Marshalls a value of this type ({@code value}) into direct storage.
	 *
	 * @param marshaller Storage for the marshalled value
	 * @param value Value to marshall
	 * @see #countVariableIndirect(Object)
	 * @since 20130716
	 */
	public abstract void marshallIn(Marshaller marshaller, Object value);
}
