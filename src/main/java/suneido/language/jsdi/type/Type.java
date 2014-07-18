/* Copyright 2013 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido.language.jsdi.type;

import suneido.SuContainer;
import suneido.language.Numbers;
import suneido.language.jsdi.DllInterface;
import suneido.language.jsdi.JSDIException;
import suneido.language.jsdi.JSDIValue;
import suneido.language.jsdi.MarshallPlanBuilder;
import suneido.language.jsdi.Marshaller;
import suneido.language.jsdi.StorageType;

/**
 * TODO: Docs
 * @author Victor Schappert
 * @since 20130625
 */
@DllInterface
public abstract class Type extends JSDIValue {

	//
	// DATA
	//

	private final TypeId typeId;
	private final StorageType storageType;

	//
	// CONSTRUCTORS
	//

	protected Type(TypeId typeId, StorageType storageType) {
		if (null == typeId)
			throw new IllegalArgumentException("typeId may not be null");
		if (null == storageType)
			throw new IllegalArgumentException("storageType may not be null");
		this.typeId = typeId;
		this.storageType = storageType;
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

	// TODO: docs since 20130724
	public abstract int getSizeDirectIntrinsic();

	// TODO: docs since 20130724
	public abstract int getSizeDirectWholeWords();

	// TODO: docs since 20130724
	public int getSizeIndirect() {
		return 0;
	}

	// TODO: Docs since 20130724
	public int getVariableIndirectCount() {
		return 0;
	}

	// TODO: Docs since 20130724
	public void addToPlan(MarshallPlanBuilder builder, boolean isCallbackPlan) {
		builder.pos(getSizeDirectIntrinsic());
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
		throw new InternalError(getDisplayName() + " cannot be marshalled in");
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
		throw new InternalError(getDisplayName() + " cannot be marshalled out");
	}

	/**
	 * <p>
	 * In a situation in which a value is being marshalled out of the native
	 * side without being marshalled in first, ensures that the marshaller
	 * contains enough information to marshall the type out.
	 * </p>
	 * <p>
	 * At the moment, this method is used to implement
	 * {@link Structure#call1(Object)}.
	 * </p>
	 * @param marshaller
	 * @since 20130813
	 */
	public void putMarshallOutInstruction(Marshaller marshaller) {
		// Do nothing.
	}

	// TODO: docs since 20130808
	public void marshallInReturnValue(Marshaller marshaller) {
		throw new InternalError(getDisplayName()
				+ " cannot be marshalled into a return value");
	}

	// TODO: docs since 20130717
	public Object marshallOutReturnValue(long returnValue, Marshaller marshaller) {
		throw new InternalError(getDisplayName()
				+ " cannot be marshalled out of a return value");
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

	// TODO: docs since 20130808
	protected final void throwNotValidForCallback() throws JSDIException {
		throw new JSDIException(getDisplayName()
				+ " may not directly or indirectly be passed to a callback");
	}

	/**
	 * <p>
	 * Where a pointer type needs to be marshalled, this function tests whether
	 * the Suneido programmer provided a value that is "equivalent" to a
	 * {@code NULL} pointer such that the system should send a {@code NULL}
	 * pointer to the {@code dll} call in question.
	 * </p>
	 * <p>
	 * In CSuneido, the values 0 ({@code SuZero}) and a {@code Value}
	 * containing a {@code NULL} pointer to an SuValue are treated as
	 * representing {@code NULL} when values are being marshalled from Suneido
	 * <em>in</em> to a {@code dll} call. However, CSuneido substitutes the
	 * value {@code false} ({@code SuFalse}) when a {@code NULL} pointer is
	 * marshalled <em>out</em> of the {@code dll} back to Suneido. On the way
	 * <em>in</em>, {@code false} is not treated as standing for a {@code NULL}
	 * pointer, which is internally inconsistent behaviour.
	 * </p>
	 * <p>
	 * In jSuneido, the values {@code null}, 0, and {@code false}
	 * (Boolean#FALSE) stand in for {@code NULL} on the way <em>in</em> while
	 * {@code NULL} pointers are marshalled <em>out</em> into the value
	 * {@code false}. This is both mostly consistent with CSuneido and
	 * more internally consistent than CSuneido at the same time.
	 * </p>
	 * @param object A reference to an Object, which may be {@code null}
	 * @return Whether {@code object} stands in for a {@code NULL} pointer
	 * @since 20130808
	 */
	protected static boolean isNullPointerEquivalent(Object object) {
		return null == object || Boolean.FALSE == object
				|| Numbers.isZero(object);
	}
}
