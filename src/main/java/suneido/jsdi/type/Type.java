/* Copyright 2013 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido.jsdi.type;

import suneido.SuContainer;
import suneido.SuInternalError;
import suneido.jsdi.DllInterface;
import suneido.jsdi.JSDIException;
import suneido.jsdi.JSDIValue;
import suneido.jsdi.StorageType;
import suneido.jsdi.marshall.MarshallPlanBuilder;
import suneido.jsdi.marshall.Marshaller;
import suneido.language.Numbers;

/**
 * <p>
 * Encapsulates a type in the JSDI type hierarchy. A type maybe either a
 * "basic" type, such as {@code int16}, or a proxied type. Types are used in
 * {@code dll} and {@code callback} parameter lists and {@code struct} member
 * lists.
 * </p>
 * <p>
 * Both the {@code callback} and {@code struct} JSDI keywords create
 * types&mdash;{@link Callback} and {@link Structure}, respectively&mdash;but
 * the {@code dll} keyword does not. 
 * </p>
 *
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
			throw new SuInternalError("typeId may not be null");
		if (null == storageType)
			throw new SuInternalError("storageType may not be null");
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
	 * doesn't contain any late-binding types which need to be bound to concrete
	 * types at runtime.
	 * </p>
	 * 
	 * @return Whether this type is closed
	 * @see TypeList#isClosed()
	 */
	public boolean isClosed() {
		return true;
	}

	/**
	 * <p>
	 * Indicates whether this type is marshallable to a Java {@code long}
	 * </p>
	 *
	 * @return Whether the type can be marshalled into and out of a {@code long}
	 * @see #marshallInToLong(Object)
	 * @see #marshallOutFromLong(long, Object)
	 */
	public boolean isMarshallableToLong() {
		return false;
	}

	/**
	 * <p>
	 * Indicates the broad behaviour category of this type. For example, a Type
	 * which behaves like a basic type (<em>eg</em> {@link BasicType} or
	 * {@link BasicArray}) will return {@link TypeId#BASIC}. 
	 * </p>
	 *
	 * @return Type identifier of this type
	 * @see suneido.jsdi.marshall.ReturnTypeGroup
	 */
	public final TypeId getTypeId() {
		return typeId;
	}

	/**
	 * <p>
	 * Indicates whether this type describes a single value, an array of values,
	 * or a pointer to a value.
	 * </p>
	 *
	 * @return Storage type of this type
	 */
	public final StorageType getStorageType() {
		return storageType;
	}

	/**
	 * <p>
	 * Returns the direct size of the type. This is the number of bytes
	 * required to store the type on the current operating platform, or what
	 * the C-language {@code sizeof()} operator would return if this type were
	 * defined in C.
	 * </p>
	 *
	 * @return Intrinsic size in bytes
	 * @since 20130724
	 * @see #getSizeIndirect()
	 */
	public abstract int getSizeDirect();

	/**
	 * <p>
	 * Returns the required alignment of the type. This is the alignment a
	 * C compiler would assign this type on the current operating platform.
	 * </p>
	 *
	 * @return Required alignment, a positive power of 2: 1, 2, 4, 8...
	 * @since 20140722
	 */
	public int getAlignDirect() {
		return getSizeDirect();
	}

	/**
	 * <p>
	 * Returns the indirect size of the type, in bytes.
	 * </p>
	 * <p>
	 * The indirect size is the aggregate of:
	 * <ul>
	 * <li>
	 * The direct size of anything this type contains a pointer to.
	 * </li>
	 * <li>
	 * The indirect size of anything this type contains a pointer to.
	 * </li>
	 * </ul>
	 * </p>
	 *
	 * @return Indirect size in bytes
	 * @see #getSizeDirect()
	 * @see #getVariableIndirectCount()
	 * @since 20130724
	 */
	public int getSizeIndirect() {
		return 0;
	}

	/**
	 * <p>
	 * Returns the variable indirect count of this type.
	 * </p>
	 * <p>
	 * Unlike the indirect <em>size</em>, the <strong>variable</strong> indirect
	 * <em>count</em> is not expressed in bytes. Rather, it is the total number
	 * of variable indirect entities contained by the type. A variable indirect
	 * entity consists of a pointer to storage of variable size&mdash;for
	 * example, a pointer to a C-style zero-terminated string.
	 * </p>
	 * @return Variable indirect count of this type
	 * @since 20130724
	 * @see #getSizeIndirect()
	 */
	public int getVariableIndirectCount() {
		return 0;
	}

	/**
	 * Adds the type to a marshall plan.
	 *
	 * @param builder Builder that is building the marshall plan
	 * @param isCallbackPlan Whether the marshall plan is being built for a
	 * callback argument list (required because not all parameter types are
	 * valid for callbacks and this can't be fully policed at compile time due
	 * to proxied types)
	 */
	public void addToPlan(MarshallPlanBuilder builder, boolean isCallbackPlan) {
		// RE: isCallbackPlan, see throwNotValidForCallback()
		builder.basic(getSizeDirect(), getAlignDirect());
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
		throw new SuInternalError(getDisplayName() + " cannot be marshalled in");
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
	 * @see #marshallIn(Marshaller, Object)
	 * @see #skipMarshalling(Marshaller)
	 */
	public Object marshallOut(Marshaller marshaller, Object oldValue) {
		throw new SuInternalError(getDisplayName()
				+ " cannot be marshalled out");
	}

	/**
	 * <p>
	 * Skips over the marshalled representation of this type.
	 * </p>
	 *
	 * <p>
	 * The use case for this method is marshalling out {@code dll} parameters
	 * that were passed by value. Since the function that was called could not
	 * have changed these values, there is no need to physically read them out:
	 * they can simply be skipped over.
	 * </p>
	 *
	 * @param marshaller Storage in which to skip the marshalled value
	 * @since 20140730
	 * @see #marshallOut(Marshaller, Object)
	 */
	public void skipMarshalling(Marshaller marshaller) {
		throw new SuInternalError("can't skip marshalling " + getDisplayName());
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

	/**
	 * <p>
	 * Marshalls the type "in" to a return value.
	 * </p>
	 * <p>
	 * This method is used for return types, such as variable indirect return
	 * types, that need to give the native side information about how to store a
	 * variable indirect return value.
	 * </p>
	 * 
	 * @param marshaller
	 *            Marshaller to marshall this return type into
	 * @since 20130808
	 * @see #marshallOutReturnValue(long, Marshaller)
	 */
	public void marshallInReturnValue(Marshaller marshaller) {
		throw new SuInternalError(getDisplayName()
				+ " cannot be marshalled into a return value");
	}

	/**
	 * Marshalls the type out of a 64-bit integer return value.
	 *
	 * @param returnValue
	 *            64-bit integer returned by native side
	 * @param marshaller
	 *            Marshaller to use to marshall out the return value
	 * @return Marshalled-out return value
	 * @since 20130717
	 */
	public Object marshallOutReturnValue(long returnValue, Marshaller marshaller) {
		throw new SuInternalError(getDisplayName()
				+ " cannot be marshalled out of a return value");
	}

	/**
	 * <p>
	 * Marshalls a value of this type ({@code value}) into a Java {@code long}.
	 * </p>
	 * 
	 * @param value Value to marshall
	 * @return Marshalled representation of {@code value}
	 * @since 20140730
	 * @see #isMarshallableToLong()
	 * @see #marshallOutFromLong(long, Object)
	 * @see #marshallIn(Marshaller, Object)
	 * @see #marshallInReturnValue(Marshaller)
	 */
	public long marshallInToLong(Object value) {
		assert !isMarshallableToLong();
		throw new SuInternalError(getDisplayName()
				+ "cannot be marshalled into a Java long");
	}

	/**
	 * <p>
	 * Marshalls a value of this type back out of a Java {@code long}.
	 * </p>
	 *
	 * @param marshaller
	 *            Storage from which to retrieve the marshalled value
	 * @param oldValue
	 *            Previous value of the marshalled value (see NOTE)
	 * @return Value retrieved from the marshaller, or {@link Boolean#FALSE} in
	 *         the case of a {@code NULL} pointer.
	 * @since 20140730
	 * @see #isMarshallableToLong()
	 * @see #marshallInToLong(Object)
	 * @see #marshallOut(Marshaller, Object)
	 * @see #marshallOutReturnValue(long, Marshaller)
	 */
	public Object marshallOutFromLong(long marshalledData, Object oldValue) {
		assert !isMarshallableToLong();
		throw new SuInternalError(getDisplayName()
				+ "cannot be marshalled into a Java long");
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
	 * Binds any late-binding types participating in the type tree rooted at
	 * this type to their underlying types and returns a flag indicating whether
	 * the type tree has changed since the last bind operation.
	 * </p>
	 * 
	 * @param level
	 *            Level of the tree at which the bind operation was requested (0
	 *            being the root)&mdash;necessary for detecting cycles in the
	 *            type hierarchy
	 * @return If the type tree has changed since the last bind, {@code true};
	 *         otherwise, {@code false}
	 * @throws BindException
	 *             If any part of the type tree could not be bound to its
	 *             underlying type
	 * @see LateBinding
	 * @see LateBinding#bind(int)
	 */
	public boolean bind(int level) throws BindException {
		// Subclasses have to explicitly implement bind.
		throw new SuInternalError(getDisplayName() + " cannot be bound");
	}

	/**
	 * <p>
	 * Throws an exception where there is an attempt to receive a type as a
	 * callback parameter (either directly as a parameter, or in indirect
	 * storage referred to by a parameter) that isn't valid for callbacks.
	 * This method is called by callback-unfriendly subclasses on an as-needs
	 * basis.
	 * </p>
	 *
	 * <p>
	 * There are two principal reasons this method might be called.
	 * <ol>
	 * <li>
	 * The type makes no sense as a {@code callback} parameter. For example, an
	 * <code>[<b>in</b>] string</code> is pointless in {@code callbacks}.
	 * </li>
	 * <li>
	 * Support for using the type as a {@code callback} parameter is simply not
	 * yet implemented (for example, for <code><b>resource</b></code>,
	 * {@link ResourceType} simply doesn't yet support being used as a callback
	 * parameter as of 20140730). 
	 * </li>
	 * </ol>
	 * </p>
	 *
	 * @throws JSDIException Unconditionally
	 */
	protected final void throwNotValidForCallback() throws JSDIException {
		// The reason this is JSDIException and not SuInternalError is that it
		// can happen for indirect reasons (e.g. parameter to callback is a
		// struct, but struct contains a member whose type is invalid for
		// callbacks). Such indirect invalid uses can't be prohibited by the
		// compiler because of the dynamic nature of the language, so this is
		// not an internal error, it's a normal runtime exception.
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
	 * In cSuneido, the values 0 ({@code SuZero}) and a {@code Value}
	 * containing a {@code NULL} pointer to an SuValue are treated as
	 * representing {@code NULL} when values are being marshalled from Suneido
	 * <em>in</em> to a {@code dll} call. However, cSuneido substitutes the
	 * value {@code false} ({@code SuFalse}) when a {@code NULL} pointer is
	 * marshalled <em>out</em> of the {@code dll} back to Suneido. On the way
	 * <em>in</em>, {@code false} is not treated as standing for a {@code NULL}
	 * pointer, which is internally inconsistent behaviour.
	 * </p>
	 * <p>
	 * In jSuneido, the values {@code null}, 0, and {@code false}
	 * (Boolean#FALSE) stand in for {@code NULL} on the way <em>in</em> while
	 * {@code NULL} pointers are marshalled <em>out</em> into the value
	 * {@code false}. This is both mostly consistent with cSuneido and
	 * more internally consistent than cSuneido at the same time.
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
