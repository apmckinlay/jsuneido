package suneido.language.jsdi.type;

import suneido.SuValue;
import suneido.language.jsdi.JSDIException;
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
	 * <p>
	 * Returns the amount of variable indirect storage occupied by a certain
	 * value if it is treated as having the same type as this. In practice, this
	 * method will only be validly called on strings and buffers.
	 * </p>
	 * <p>
	 * FIXME: There is a problem with how countVariableIndirect is conceived.
	 * It is possible some kind of concurrent execution causes the length of
	 * strings to increase between the time that countVariableIndirect is called
	 * for a particular value, and the time that the data is actually
	 * marshalled. If/when this happens, you'll overrun the buffer allocated
	 * based on countVariableIndirect. Indirect storage should probably just be
	 * dumped into a separate growable buffer which starts out quite big.
	 * </p>
	 * 
	 * @param value
	 *            Non-{@code null} value whose variable indirect storage needs
	 *            are to be counted.
	 * @return Amount of variable indirect storage required by {@code value}
	 * @see TypeList#countVariableIndirectMembers(suneido.SuContainer)
	 * @see TypeList#countVariableIndirectParams(Object[])
	 * @see #marshallIn(Marshaller, Object)
	 * @since 20130711
	 */
	public int countVariableIndirect(Object value) {
		throw new JSDIException(getDisplayName()
				+ " does not support variable indirect storage");
	}

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
