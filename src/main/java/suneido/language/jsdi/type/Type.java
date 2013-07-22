package suneido.language.jsdi.type;

import suneido.SuValue;
import suneido.language.jsdi.DllInterface;
import suneido.language.jsdi.MarshallPlan;
import suneido.language.jsdi.Marshaller;
import suneido.language.jsdi.StorageType;

/**
 * TODO: Docs
 * @author Victor Schappert
 * @since 20130625
 */
@DllInterface
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

	/**
	 * <p>
	 * Indicates whether this type is 'closed'.
	 * </p>
	 * <p>
	 * The marshall plan for a 'closed' type is fixed at compile time. The type
	 * doesn't contain any proxies which need to be resolved at runtime.
	 * </p>
	 * 
	 * @return Whether this type is closed
	 * @see TypeList#isClosed()
	 */
	public boolean isClosed() {
		return true;
	}

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
	 * <p>
	 * Marshalls a value of this type ({@code value}) into native dll format.
	 * </p>
	 * <p>
	 * <strong>NOTE</strong>: For pointer types, the following values are
	 * converted into {@code NULL} pointers:
	 * <ul>
	 * <li>{@code null} references;</li>
	 * <li>{@link Boolean#FALSE}; and</li>
	 * <li>the numeric value 0.</li>
	 * </ul>
	 * </p>
	 *
	 * @param marshaller Storage for the marshalled value
	 * @param value Value to marshall
	 * @see #countVariableIndirect(Object)
	 * @since 20130716
	 */
	public void marshallIn(Marshaller marshaller, Object value) {
		throw new IllegalStateException(getDisplayName()
				+ " cannot be marshalled in");
	}

	/**
	 * <p>
	 * Marshalls a value of this type back out of native dll format.
	 * </p>
	 * <p>
	 * <strong>NOTE</strong>: Because of possible concurrent modification of
	 * {@link SuContainer}s between the time that a {@code dll} call was
	 * initiated and the time that the arguments are ultimately marshalled out,
	 * {@code oldValue} is not guaranteed to hold the actual value of the
	 * variable being marshalled at the time the {@code dll} call was initiated.
	 * Clients must avoid concurrent modification of objects being marshalled in
	 * order to avoid unexpected results.
	 * </p>
	 *
	 * @param marshaller Storage from which to retrieve the marshalled value
	 * @param oldValue Previous value of the marshalled value (see NOTE)
	 * @return Value retrieved from the marshaller, or {@link Boolean#FALSE} in
	 * the case of a {@code NULL} pointer.
	 * @since 20130717
	 */
	public Object marshallOut(Marshaller marshaller, Object oldValue) {
		throw new IllegalStateException(getDisplayName()
				+ " cannot be marshalled out");
	}

	// TODO: docs since 20130717
	public Object marshallOutReturnValue(int returnValue) {
		throw new IllegalStateException(getDisplayName() + 
				" cannot be marshalled out of a 32-bit return value");
	}

	// TODO: docs since 20130717
	public Object marshallOutReturnValue(long returnValue) {
		throw new IllegalStateException(getDisplayName()
				+ " cannot be marshalled out of a 64-bit return value");
	}

	// TODO: docs since 20130717
	public boolean isMarshallableToJSDILong() {
		return false;
	}

	// TODO: docs since 20130717
	public void marshallInToJSDILong(int[] target, int pos, Object value) {
		throw new IllegalStateException(getDisplayName()
				+ " cannot be marshalled into a long");
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
	public abstract String getDisplayName();}
