/* Copyright 2013 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido.jsdi.type;

import suneido.jsdi.DllInterface;

/**
 * <p>
 * Exception thrown when a late-binding type cannot bind to an underlying
 * user-defined {@link ComplexType}.
 * </p>
 *
 * @author Victor Schappert
 * @since 20130702
 */
@DllInterface
public final class BindException extends Exception {

	//
	// SERIALIZATION
	//

	private static final long serialVersionUID = 7310351403974056739L;

	//
	// DATA
	//

	private final LateBinding lateBinding;
	private final Class<?> invalidBoundUnderlying;
	private String memberName;
	private String memberType; // 'parameter' or 'member'
	private String parentName;

	//
	// CONSTRUCTORS
	//

	BindException(LateBinding lateBinding, Class<?> invalidBoundUnderlying) {
		super("can't bind " + lateBinding.getDisplayName());
		this.lateBinding = lateBinding;
		this.invalidBoundUnderlying = invalidBoundUnderlying;
		this.memberName = null; // to be filled in higher up the call stack
		this.memberType = null; // likewise
		this.parentName = null; // likewise
	}

	//
	// ACCESSORS
	//

	/**
	 * Returns the late-binding type whose failure to bind caused this
	 * exception.
	 *
	 * @return LateBinding that failed to bind
	 * @see #getInvalidBoundUnderlying()
	 */
	public final LateBinding getLateBinding() {
		return lateBinding;
	}

	/**
	 * Returns the class of the underlying value the late-binding type would
	 * have bound to if it wasn't invalid. If the late-binding type could not
	 * find any value to bind to at all, this value will be {@code null}.
	 * However, if the late-binding object tried to bind to an invalid value
	 * (<em>ie</em> not a user-defined {@link ComplexType}), the return value
	 * is a reference to that invalid value's class.
	 *
	 * @return Class of the invalid value or <code>null</code> if none
	 */
	public final Class<?> getInvalidBoundUnderlying() {
		return invalidBoundUnderlying;
	}

	//
	// MUTATORS
	//

	/**
	 * <p>
	 * Sets the member name of the late-binding type within its parent type.
	 * </p>
	 * 
	 * <p>
	 * For example, imagine a <code>S &rarr; <b>struct</b> { T t }</code> where
	 * <code>t</code> is the name of a member within the structure type
	 * <code>S</code> that is of the late binding type <code>T</code>. If, in
	 * attempting to bind <code>S</code>, <code>T</code> causes a
	 * {@link BindException}, the member name will be "t".
	 * </p>
	 *
	 * @param memberName
	 *            Name of the member whose type is the late-binding type that
	 *            failed to bind
	 * @see #setMemberType(String)
	 * @see #setParentName(String)
	 */
	public final void setMemberName(String memberName) {
		this.memberName = memberName;
	}

	/**
	 * <p>
	 * Sets the member name of the late-binding type within its parent type.
	 * This will be either "parameter"&mdash;for example, if a callback
	 * parameter failed to bind&mdash; or "member"&mdash;if a structure member
	 * failed to bind.
	 * </p>
	 *
	 * @param memberType
	 *            Either "parameter" or "member"
	 * @see #setMemberName(String)
	 * @see #setParentName(String)
	 */
	public final void setMemberType(String memberType) {
		this.memberType = memberType;
	}

	/**
	 * <p>
	 * Sets the name of the parent type within which late-binding member type
	 * failed to bind.
	 * </p>
	 * 
	 * <p>
	 * For example, imagine a <code>S &rarr; <b>struct</b> { T t }</code> where
	 * <code>t</code> is the name of a member within the structure type
	 * <code>S</code> that is of the late binding type <code>T</code>. If, in
	 * attempting to bind <code>S</code>, <code>T</code> causes a
	 * {@link BindException}, the parent name will be "S".
	 * </p>
	 *
	 * @param parentName
	 *            Name of the parent type whose member type failed to bind
	 * @see #setMemberName(String)
	 * @see #setMemberType(String)
	 */
	 public final void setParentName(String parentName) {
		this.parentName = parentName;
	}

	//
	// ANCESTOR CLASS: Exception
	//

	private StringBuilder appendUnderlyingMessage(StringBuilder sb) {
		sb.append("underlying type ")
				.append(lateBinding.getUnderlyingTypeName()).append(" of ");
		if (0 < parentName.length()) {
			sb.append('\'').append(parentName).append("' ");
		}
		return sb.append(memberType).append(" '")
				.append(lateBinding.getDisplayName()).append(' ')
				.append(memberName).append('\'');
	}

	@Override
	public final String getMessage() {
		if (null == memberName || null == memberType || null == parentName)
			return super.getMessage();
		else {
			StringBuilder result = new StringBuilder(128);
			if (null == invalidBoundUnderlying) {
				result.append("not found: ");
				appendUnderlyingMessage(result);
			} else {
				result.append("expected ");
				appendUnderlyingMessage(result).append(" to be ")
						.append(Structure.class.getSimpleName()).append(" or ")
						.append(Callback.class.getSimpleName())
						.append(" but it is ")
						.append(invalidBoundUnderlying.getSimpleName());
			}
			return result.toString();
		}
	}
}
