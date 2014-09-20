/* Copyright 2013 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido.jsdi.marshall;

import suneido.jsdi.DllInterface;

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

	//
	// TYPES
	//

	/**
	 * Enumerates categories of marshall plan according to what kind(s) of
	 * storage they require.
	 *
	 * @author Victor Schappert
	 * @since 20140722
	 */
	public static enum StorageCategory {

		/**
		 * The plan uses only direct storage.
		 */
		DIRECT,
		/**
		 * The plan uses some indirect storage, but no variable indirect
		 * storage.
		 */
		INDIRECT,
		/**
		 * The plan uses variable indirect storage.
		 */
		VARIABLE_INDIRECT;
	}

	//
	// DATA
	//

	private final int   sizeDirect;
	private final int   alignDirect;
	private final int   sizeIndirect;
	private final int   sizeTotal;
	private final int   variableIndirectCount;
	private final int[] ptrArray; // Pairs<x, y> => x: ptr idx, y: byte idx
	private final int[] posArray; // Byte indices

	//
	// CONSTRUCTORS
	//

	protected MarshallPlan(int sizeDirect, int alignDirect, int sizeIndirect,
			int sizeTotal, int[] ptrArray, int[] posArray,
			int variableIndirectCount) {
		assert 0 <= alignDirect && Integer.bitCount(alignDirect) <= 1 : "alignDirect must be a non-negative power of 2";
		assert 0 <= sizeIndirect;
		assert sizeDirect + sizeIndirect <= sizeTotal;
		assert 0 <= variableIndirectCount;
		assert 0 == ptrArray.length % 2;
		this.sizeDirect   = sizeDirect;
		this.alignDirect  = alignDirect;
		this.sizeIndirect = sizeIndirect;
		this.sizeTotal    = sizeTotal;
		this.ptrArray     = ptrArray;
		this.posArray     = posArray;
		this.variableIndirectCount = variableIndirectCount;
	}

	//
	// INTERNALS
	//

	protected abstract Marshaller makeMarshallerInternal(int sizeTotal,
			int variableIndirectCount, int[] ptrArray, int[] posArray);

	protected abstract Marshaller makeUnMarshallerInternal(long[] data,
			int[] ptrArray, int[] posArray);

	protected abstract Marshaller makeUnMarshallerInternal(long[] data,
			int[] ptrArray, int[] posArray, Object[] viArray, int[] viInstArray);

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
	 * @see #getSizeTotal()
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
	 *
	 * <p>
	 * The number returned includes both the intrinsic size of the direct data
	 * and the size of any padding is inserted at appropriate places in order
	 * to align the data members as appropriate for the target platform.
	 * </p>
	 *
	 * @return Amount of direct storage required to marshall the data
	 * @see #getSizeDirectLongAligned()
	 * @see #isDirectOnly()
	 * @see #getAlignDirect()
	 * @see #getSizeTotal()
	 * @see #getVariableIndirectCount()
	 */
	public final int getSizeDirect() {
		return sizeDirect;
	}

	/**
	 * <p>
	 * Returns the long-aligned direct storage size.
	 * </p>
	 *
	 * <p>
	 * <code>0 &lt; {@link #getSizeDirect()} &le;
	 * {@link #getSizeDirectLongAligned()}</code> and the number returned is
	 * the size, in bytes, of the storage required by the minimum number of
	 * contiguous Java {@code long} values that will hold
	 * {@link #getSizeDirect()}. 
	 * </p>
	 *
	 * @return Long-aligned direct storage size
	 * @see #getSizeDirect()
	 * @see #getAlignDirect()
	 * @see #isDirectOnly()
	 */
	public final int getSizeDirectLongAligned() {
		return PrimitiveSize.sizeLongs(sizeDirect);
	}

	/**
	 * <p>
	 * Get the required alignment, in bytes, of the plan's direct data.
	 * </p>
	 *
	 * @return Positive power of two indicating alignment of direct data: 1, 2,
	 *         4, 8, ...
	 * @since 20140722
	 * @see #getSizeDirect()
	 */
	public final int getAlignDirect() {
		return alignDirect;
	}

	/**
	 * <p>
	 * Get the amount of indirect storage, in bytes, required to marshall the
	 * data.
	 * </p>
	 *
	 * @return Amount of indirect storage required to marshall the data
	 * @since 20130806
	 * @see #getSizeDirect()
	 * @see #getSizeTotal()
	 * @see #getVariableIndirectCount()
	 */
	public final int getSizeIndirect() {
		return sizeIndirect;
	}

	/**
	 * <p>
	 * Get the total amount of storage required to marshall the data. This
	 * includes the direct size, the indirect size, and any padding inserted
	 * between the direct and indirect data in order to align the indirect data.
	 * </p>
	 *
	 * <p>
	 * The number returned cannot in general be expected to equal
	 * {@link #getSizeDirect()} <code>+</code> {@link #getSizeIndirect()}.
	 * </p>
	 *
	 * @return Amount of indirect storage required to marshall the data
	 * @see #getSizeDirect()
	 * @see #getPtrArray()
	 * @see #getVariableIndirectCount()
	 * @since 20140721
	 */
	public final int getSizeTotal() {
		return sizeTotal;
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
	 * @see #getSizeTotal()
	 * @see #getVariableIndirectCount()
	 * @since 20130806
	 */
	public final int[] getPtrArray() {
		// Public because it is used by ABI-specific thunk managers in other
		// packages.
		return ptrArray;
	}

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
	 * <p>
	 * Get this plan's storage use category.
	 * </p>
	 *
	 * @return Plan group this plan belongs to
	 * @since 20140722
	 */
	public final StorageCategory getStorageCategory() {
		if (0 < variableIndirectCount)
			return StorageCategory.VARIABLE_INDIRECT;
		else if (0 < sizeIndirect)
			return StorageCategory.INDIRECT;
		else
			return StorageCategory.DIRECT;
	}

	/**
	 * Creates a marshaller instance for the ordinary use case of putting data
	 * into marshalled representation (<em>put</em> operations). The data
	 * marshalled in is valid for <em>get</em> operations after a rewind.
	 *
	 * @return Marshaller on this plan
	 * @since 20140719
	 */
	public final Marshaller makeMarshaller() {
		return makeMarshallerInternal(sizeTotal, variableIndirectCount,
				ptrArray, posArray);
	}

	/**
	 * Creates a marshaller instance which is only valid for <em>get</em>
	 * operations out of existing data.
	 * 
	 * @param data
	 *            An existing data array of the correct length
	 * @return Get-only marshaller based on this plan
	 * @since 20130806
	 * @see #makeMarshaller()
	 * @see #makeUnMarshaller(long[], Object[], int[])
	 */
	public final Marshaller makeUnMarshaller(long[] data) {
		assert 0 == variableIndirectCount;
		assert data.length == sizeTotal / Long.BYTES;
		return makeUnMarshallerInternal(data, ptrArray, posArray);
	}

	/**
	 * Creates a marshaller instance which is only valid for <em>get</em>
	 * operations out of existing data.
	 *
	 * @param data
	 *            An existing data array of the correct length
	 * @param viArray
	 *            An existing, valid, variable indirect output array
	 * @param viInstArray
	 *            An existing, valid, variable indirect instruction array
	 * @return Get-only marshaller based on this plan
	 * @since 20130806
	 * @see #makeMarshaller()
	 * @see #makeUnMarshaller(long[])
	 */
	public final Marshaller makeUnMarshaller(long[] data, Object[] viArray,
			int[] viInstArray) {
		assert data.length == sizeTotal / Long.BYTES
				&& viArray.length == variableIndirectCount
				&& viInstArray.length == variableIndirectCount;
		return makeUnMarshallerInternal(data, ptrArray, posArray, viArray,
				viInstArray);
	}

	//
	// ANCESTOR CLASS: Object
	//

	@Override
	public final String toString() {
		StringBuilder result = new StringBuilder(128);
		result.append(getClass().getSimpleName()).append("[ ");
		result.append(sizeDirect).append(", ").append(sizeTotal).append(", {");
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
