package suneido.language.jsdi.type;

import suneido.SuValue;
import suneido.language.jsdi.JSDIException;
import suneido.language.jsdi.MarshallPlan;
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
	// INTERNALS
	//

	protected void releaseHandle() {	// TODO: delete this method
// XXX
//		assert 0 != jsdiHandle : "No handle to release";
//		TypeFactory.releaseHandle(jsdiHandle);
//		jsdiHandle = 0;
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
	 * Returns the amount of variable indirect storage occupied by a certain
	 * value if it is treated as having the same type as this. In practice, this
	 * method will only be validly called on strings and buffers.
	 * 
	 * @param value
	 *            Non-{@code null} value whose variable indirect storage needs
	 *            are to be counted.
	 * @return Amount of variable indirect storage required by {@code value}
	 * @see Type#countVariableIndirect(Object)
	 * @since 20130711
	 */
	public int countVariableIndirect(Object value) {
		throw new JSDIException(getDisplayName()
				+ " does not support variable indirect storage");
	}
}
