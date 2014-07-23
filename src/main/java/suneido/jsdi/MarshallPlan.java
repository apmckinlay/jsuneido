/* Copyright 2013 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido.jsdi;

/**
 * <p>
 * Encapsulates a template for creating a marshaller.
 * </p>
 * <p>
 * The division of marshalling labour is as follows:
 * <ul>
 * <li>
 * A marshall plan describes <strong>how to create</strong> a
 * {@link Marshaller}. A plan is in principle stable and permanent: it needs to
 * be recomputed if, and only if, a type hierarchy changes <em>ie</em> if the
 * concrete type referred to by a proxied type changes. A marshall plan is
 * analogous to a Java class.
 * </li>
 * <li>
 * A {@link Marshaller} gets and/or puts data for one particular {@code dll}
 * call, {@code callback} invocation, or {@code struct} copy out. It is not
 * reusable. Once used it must be discarded and a new marshaller created from
 * the relevant plan for each subsequent marshalling problem. A
 * {@link Marshaller} is analogous to a Java class instance.
 * </ul>
 * </p>
 * <p>
 * The reason for the division of responsibility between the plan and the actual
 * marshaller is that a considerable portion of the marshalling problem is
 * susceptible of being pre-computed (the "plan"). This means that each
 * individual marshalling sequence can be driven by pre-calculated tables and
 * constant values, considerably speeding up the process of converting Suneido
 * language values to native values.
 * </p>
 *
 * @author Victor Schappert
 * @since 20130702
 */
@DllInterface
public abstract class MarshallPlan {
	
//	 * FIXME: Need to insert padding 0-7 padding bytes btwn sizeDirect and
//	 *        sizeIndirect ... (first check w Java abt array alignment). We might
//	 *        be screwed if Java doesn't 8-byte-align byte array beginnings i.e.
//	 *        might need to use a long array

	//
	// DATA
	//

	protected final int   sizeDirect;
	protected final int   sizeIndirect;
	protected final int   variableIndirectCount;
	protected final int[] ptrArray; // TODO: This should go to word indices
	protected final int[] posArray; // TODO: This can stay byte indices

	//
	// CONSTRUCTORS
	//

	protected MarshallPlan(int sizeDirect, int sizeIndirect, int[] ptrArray,
			int[] posArray, int variableIndirectCount) {
		this.sizeDirect            = sizeDirect;
		this.sizeIndirect          = sizeIndirect;
		this.ptrArray              = ptrArray;
		this.posArray              = posArray;
		this.variableIndirectCount = variableIndirectCount;
	}

	//
	// ACCESSORS
	//

	/**
	 * Indicates this plan describes direct-only (<em>ie</em> pointerless)
	 * storage.
	 *
	 * @return True if the plan contains no pointers, false otherwise
	 * @since 20130814
	 * @see #getSizeDirect()
	 * @see #getSizeIndirect()
	 * @see #getVariableIndirectCount()
	 */
	public final boolean isDirectOnly() {
		return 0 == sizeIndirect && 0 == variableIndirectCount;
	}

	/**
	 * <p>
	 * Get the amount of direct storage, in bytes, required to marshall the
	 * data.
	 * </p>
	 * <p>
	 * This function does not presently deal with alignment issues, so if the
	 * Suneido programmer wants to align particular data members, he must do so
	 * by inserting suitable padding members if necessary.
	 *
	 * TODO: Is this paragraph still accurate after jsdi64?
	 * </p>
	 *
	 * @return Amount of direct storage required to marshall the data
	 * @see #isDirectOnly
	 * @see #getSizeIndirect()
	 * @see #getPtrArray()
	 * @see #getVariableIndirectCount()
	 */
	public final int getSizeDirect() {
		return sizeDirect;
	}

	/**
	 * <p>
	 * Get the amount of indirect storage, in bytes, required to marshall the
	 * data.
	 * </p>
	 *
	 * @return Amount of indirect storage required to marshall the data
	 * @see #getSizeDirect()
	 * @see #getPtrArray()
	 * @see #getVariableIndirectCount()
	 * @since 20130806
	 */
	public final int getSizeIndirect() {
		return sizeIndirect;
	}

	/**
	 * <p>
	 * Get a reference to the plan's internal pointer array.
	 * </p>
	 * <p>
	 * Because the return value refers directly to the plan's internal pointer
	 * array, the <em>caveat</em> from {@link Marshaller#getPtrArray()} applies
	 * equally here: <em>the contents of the array returned are
	 * <strong>not</strong> to be modified under any circumstances!</em>.
	 * 
	 * @return Pointer array
	 * @see Marshaller#getPtrArray()
	 * @see #getSizeDirect()
	 * @see #getSizeIndirect()
	 * @see #getVariableIndirectCount()
	 * @since 20130806
	 */
	public final int[] getPtrArray() {
		return ptrArray;
	} // TODO: Does this need to be public? Who uses this?

	/**
	 * <p>
	 * Get the number of variable indirect values this plan requires to be
	 * marshalled.
	 * </p>
	 * @return Variable indirect count
	 * @since 20130806
	 * @see #getSizeDirect()
	 * @see #getSizeIndirect()i
	 */
	public final int getVariableIndirectCount() {
		return variableIndirectCount;
	}

	/**
	 * This method is purely for testing purposes.
	 *
	 * @return Marshaller on this plan
	 * @since 20140719
	 */
	public abstract Marshaller makeMarshaller();

	//
	// ANCESTOR CLASS: Object
	//

	@Override
	public final String toString() {
		StringBuilder result = new StringBuilder(128);
		result.append("MarshallPlan[ ").append(sizeDirect).append(", ")
				.append(sizeIndirect).append(", {");
		final int N = ptrArray.length;
		if (0 < N) {
			result.append(' ').append(ptrArray[0]).append(':')
					.append(ptrArray[1]);
			int k = 2;
			while (k < N) {
				result.append(", ").append(ptrArray[k++]).append(':')
						.append(ptrArray[k++]);
			}
		}
		result.append(" }, {");
		final int P = posArray.length;
		if (0 < P) {
			result.append(' ').append(posArray[0]);
			for (int k = 1; k < P; ++k) {
				result.append(", ").append(posArray[k]);
			}
		}
		result.append(" }, #vi:").append(variableIndirectCount).append(" ]");
		return result.toString();
	}
}
