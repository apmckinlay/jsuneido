package suneido.language.jsdi;

import java.util.Arrays;

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
			int[] posArray, int countVariableIndirect) {
		this.sizeDirect            = sizeDirect;
		this.sizeIndirect          = sizeIndirect;
		this.ptrArray              = ptrArray;
		this.posArray              = posArray;
		this.variableIndirectCount = countVariableIndirect;
	}

	//
	// ACCESSORS
	//

	/**
	 * <p>
	 * Get the direct storage required to marshall the data.
	 * </p>
	 * <p>
	 * This function does not presently deal with alignment issues, so if the
	 * Suneido programmer wants to align particular data members, he must do so
	 * by inserting suitable padding members if necessary.
	 * </p>
	 *
	 * @return Amount of direct storage required to marshall the data
	 * @see #getSizeDirectWholeWords() 
	 * @see #getSizeIndirect()
	 */
	public int getSizeDirect() {
		return sizeDirect;
	}

	/**
	 * Get the indirect storage required to marshall the data. Indirect storage
	 * is the storage occupied by the target of a pointer (or, within a
	 * structure or array, the targets of all the pointers which are directly or
	 * indirectly part of the structure or array).
	 * @return Amount of indirect storage required to marshall the data, in
	 * bytes.
	 * @see #getSizeDirectWholeWords()
	 */
	public int getSizeIndirect() {
		return sizeIndirect;
	}

	/**
	 * <p>
	 * Returns a non-negative integer indicating the number of members of this
	 * plan which require variable indirect storage.
	 * </p>
	 * <p>
	 * <strong>NOTE</strong>: This is not the amount of storage required &mdash;
	 * it is only a tally of all the variables that are requesting it.
	 * </p> 
	 * @return Number of members of the plan requiring variable indirect
	 * storage.
	 * @see Marshaller#UNKNOWN_LOCATION
	 */
	public int getVariableIndirectCount() {
		return this.variableIndirectCount;
	}

	// TODO: docs
	// since 20130717
	public int getPtrArraySize() {
		return ptrArray.length;
	}

	// TODO: docs
	// since 20130717
	public int getPosArraySize() {
		return posArray.length;
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
	 * Tests whether two plans are equal.
	 *
	 * @param mp The other plan to compare.
	 * @return Whether {@code this} plan is equal to {@code mp}.
	 */
	public boolean equals(MarshallPlan mp) {
		// FIXME: This has to be updated!!
		if (null == mp)
			return false;
		else if (this == mp)
			return true;
		else {
			return
					sizeDirect == mp.sizeDirect &&
					sizeIndirect == mp.sizeIndirect &&
					variableIndirectCount == mp.variableIndirectCount &&
					Arrays.equals(ptrArray, mp.ptrArray) &&
					Arrays.equals(posArray, mp.posArray);
		}
	}

	//
	// ANCESTOR CLASS: Object
	//

	@Override
	public boolean equals(Object o) {
		return o instanceof MarshallPlan
			? equals((MarshallPlan)o)
			: false
			;
	}

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
