/* Copyright 2013 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido.language.jsdi;

/**
 * TODO: docs
 * FIXME: Need to insert padding 0-7 padding bytes btwn sizeDirect and
 *        sizeIndirect ... (first check w Java abt array alignment). We might
 *        be screwed if Java doesn't 8-byte-align byte array beginnings i.e.
 *        might need to use a long array
 * @author Victor Schappert
 * @since 20130702
 */
@DllInterface
public final class MarshallPlan {

	//
	// DATA
	//

	private final int   sizeDirect;
	private final int   sizeIndirect;
	private final int   variableIndirectCount;
	private final int[] ptrArray;
	private final int[] posArray;

	//
	// CONSTRUCTORS
	//

	MarshallPlan(int sizeDirect, int sizeIndirect, int[] ptrArray,
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
	public boolean isDirectOnly() {
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
	 * </p>
	 *
	 * @return Amount of direct storage required to marshall the data
	 * @see #isDirectOnly
	 * @see #getSizeIndirect()
	 * @see #getPtrArray()
	 * @see #getVariableIndirectCount()
	 */
	public int getSizeDirect() {
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
	public int getSizeIndirect() {
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
	public int[] getPtrArray() {
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
	public int getVariableIndirectCount() {
		return variableIndirectCount;
	}

	/**
	 * Creates a marshaller instance for marshalling all data described by this
	 * plan, both direct and indirect.
	 *
	 * @return Marshaller based on this plan
	 */
	public Marshaller makeMarshaller() {
		return new Marshaller(sizeDirect, sizeIndirect, variableIndirectCount,
				ptrArray, posArray);
	}

	/**
	 * Creates a marshaller instance which is only valid for <em>get</em>
	 * operations out of existing data.
	 * 
	 * @param data An existing data array of the correct length
	 * @return Get-only marshaller based on this plan
	 * @since 20130806
	 * @see #makeMarshaller()
	 * @see #makeUnMarshaller(byte[], Object[], int[])
	 */
	public Marshaller makeUnMarshaller(byte[] data) {
		assert 0 == variableIndirectCount;
		assert data.length == sizeDirect + sizeIndirect;
		return new Marshaller(data, ptrArray, posArray);
	}

	/**
	 * Creates a marshaller instance which is only valid for <em>get</em>
	 * operations out of existing data.
	 *
	 * @param data An existing data array of the correct length
	 * @param viArray An existing, valid, variable indirect output array
	 * @param viInstArray An existing, valid, variable indirect instruction
	 * array
	 * @return Get-only marshaller based on this plan
	 * @since 20130806
	 * @see #makeMarshaller()
	 * @see #makeUnMarshaller(byte[])
	 */
	public Marshaller makeUnMarshaller(byte[] data, Object[] viArray,
			int[] viInstArray) {
		assert viArray.length == variableIndirectCount &&
			viInstArray.length == variableIndirectCount;
		return new Marshaller(data, ptrArray, posArray, viArray, viInstArray);
	}

	//
	// ANCESTOR CLASS: Object
	//

	@Override
	public String toString() {
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
