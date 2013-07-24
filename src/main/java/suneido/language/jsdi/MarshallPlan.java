package suneido.language.jsdi;

import java.util.Arrays;

import suneido.language.jsdi.type.SizeDirect;

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
	private final int[] ptrArray;
	private final int[] posArray;
	private final int   countVariableIndirect;

	//
	// PUBLIC CONSTANTS
	//

	/**
	 * Represents an index in the marshalled data array which:
	 * <ul>
	 * <li>
	 * if this value is present in a MarshallPlan on the Java side, indicates
	 * that the index to which the pointer points cannot be determined until the
	 * data is marshalled, because a variable amount of indirect storage is
	 * involved; and
	 * </li>
	 * <li>
	 * if this value is present in a marshalled data array on the native side,
	 * indicates that the pointer value provided by the Java marshaller should
	 * not be changed on the native side (this can happen if a NULL pointer, or
	 * INTRESOURCE value, is passed in a memory location that would otherwise be
	 * a pointer).
	 * </li>
	 * </ul>
	 * @see #makeVariableIndirectPlan()
	 * @see #getCountVariableIndirect()
	 */
	public static final int UNKNOWN_LOCATION = -1;

	//
	// CONSTRUCTORS
	//

	private MarshallPlan(int sizeDirect, int sizeIndirect, int[] ptrArray,
			int[] posArray, int countVariableIndirect) {
		this.sizeDirect            = sizeDirect;
		this.sizeIndirect          = sizeIndirect;
		this.ptrArray              = ptrArray;
		this.posArray              = posArray;
		this.countVariableIndirect = countVariableIndirect;
	}

	/**
	 * Plain value constructor.
	 *
	 * @param sizeDirect Direct size of the data to marshall.
	 * @author Victor Schappert
	 * @since 20130702
	 */
	public static MarshallPlan makeDirectPlan(int sizeDirect) {
		if (sizeDirect < 1) {
			throw new IllegalArgumentException("Can't make a direct NULL plan");
		}
		return makePlanInternal(sizeDirect, 0, NO_PTR_ARRAY, DIRECT_POS_ARRAY,
				0);
	}

	/**
	 * Pointer constructor.
	 * 
	 * @param targetPlan
	 *            Marshalling plan for the value type to which the pointer
	 *            points.
	 * @author Victor Schappert
	 * @since 20130704
	 */
	public static MarshallPlan makePointerPlan(MarshallPlan targetPlan) {
		if (targetPlan.getSizeDirectIntrinsic() < 1) {
			throw new IllegalArgumentException(
					"Can't make a pointer to a NULL plan");
		}
		final int sizeDirect = SizeDirect.POINTER;
		final int sizeIndirect = targetPlan.getSizeDirectIntrinsic() + targetPlan.sizeIndirect;
		// ptrArray
		final int N = 2 + targetPlan.ptrArray.length;
		final int[] ptrArray = new int[N];
		ptrArray[1] = SizeDirect.POINTER; // assert: ptrArray[0] == 0
		int i = 0, j = 2;
		while (j < N) {
			ptrArray[j++] = targetPlan.ptrArray[i++] + SizeDirect.POINTER;
			ptrArray[j++] = targetPlan.ptrArray[i++] + SizeDirect.POINTER;
			// This will correctly move variable indirect indices which index
			// past the end of the data [direct + indirect] array.
		}
		// posArray
		final int P = targetPlan.posArray.length;
		final int[] posArray = new int[1 + P];
		int k = 0;
		while (k < P) {
			final int pos = targetPlan.posArray[k];
			posArray[++k] = pos + SizeDirect.POINTER;
		}
		return makePlanInternal(sizeDirect, sizeIndirect, ptrArray, posArray,
				targetPlan.countVariableIndirect);
	}

	/**
	 * Array constructor.
	 * 
	 * @param elementPlan
	 *            Marshalling plan for the value type of the array elements.
	 * @param numElems
	 *            Number of elements in the array.
	 * @author Victor Schappert
	 * @since 20130704
	 */
	public static MarshallPlan makeArrayPlan(MarshallPlan elementPlan,
			int numElems) {
		if (elementPlan.getSizeDirectIntrinsic() < 1) {
			throw new IllegalArgumentException(
					"Can't make an array of NULL plans");
		}
		assert 0 < numElems;
		final int sizeDirect = numElems * elementPlan.getSizeDirectIntrinsic();
		final int sizeIndirect = numElems * elementPlan.sizeIndirect;
		final int M = elementPlan.ptrArray.length;
		// ptrArray
		final int[] ptrArray = new int[numElems * M];
		int nextViIndex = sizeDirect + sizeIndirect;
		int k = 0; // index into ptrArray
		for (int i = 0; i < numElems; ++i) {
			int j = 0;
			while (j < M) {
				int ptrIndex = elementPlan.ptrArray[j++];
				if (ptrIndex < sizeDirect) {
					ptrIndex += i * elementPlan.getSizeDirectIntrinsic();
				} else {
					ptrIndex += (numElems - 1) * elementPlan.getSizeDirectIntrinsic() + i
							* elementPlan.sizeIndirect;
				}
				ptrArray[k++] = ptrIndex;
				int targetIndex = elementPlan.ptrArray[j++];
				if (elementPlan.getSizeDirectIntrinsic() + elementPlan.sizeIndirect <= targetIndex)
					targetIndex = nextViIndex++;
				else
					targetIndex += (numElems - 1) * elementPlan.getSizeDirectIntrinsic() + i
							* elementPlan.sizeIndirect;
				ptrArray[k++] = targetIndex;
			}
		}
		// posArray
		final int N = elementPlan.posArray.length;
		k = 0; // index into posArray
		final int[] posArray = new int[numElems * N];
		for (int i = 0; i < numElems; ++i) {
			int j = 0;
			while (j < N) {
				int pos = elementPlan.posArray[j++];
				if (0 <= pos && pos < elementPlan.getSizeDirectIntrinsic()) {
					pos += i * elementPlan.getSizeDirectIntrinsic();
				} else if (elementPlan.getSizeDirectIntrinsic() <= pos) {
					assert pos < elementPlan.getSizeDirectIntrinsic() + elementPlan.sizeIndirect;
					pos += (numElems - 1) * elementPlan.getSizeDirectIntrinsic() + i * elementPlan.sizeIndirect;
				} else {
					assert false : "invalid position: " + pos;
				}
				posArray[k++] = pos;
			}
		}
		return makePlanInternal(sizeDirect, sizeIndirect, ptrArray, posArray,
				elementPlan.countVariableIndirect * numElems);
	}

	/**
	 * Structure/parameter constructor.
	 * 
	 * @param children
	 *            List of marshalling plans for each structure member, in order.
	 * @author Victor Schappert
	 * @since 20130702
	 */
	public static MarshallPlan makeContainerPlan(Iterable<MarshallPlan> children) {
		int sizePtrArray = 0;
		int sizePosArray = 0;
		int sizeDirect = 0;
		int sizeIndirect = 0;
		for (MarshallPlan child : children) {
			sizeDirect += child.getSizeDirectIntrinsic();
			sizeIndirect += child.sizeIndirect;
			sizePtrArray += child.ptrArray.length;
			sizePosArray += child.posArray.length;
		}
		// ptrArray + posArray
		final int[] ptrArray = new int[sizePtrArray];
		final int[] posArray = new int[sizePosArray];
		int sizeDirect2 = 0;
		int sizeIndirect2 = 0;
		int countVariableIndirect = 0;
		int nextViIndex = sizeDirect + sizeIndirect;
		int i = 0; // index into ptrArray
		int j = 0; // index into posArray
		for (MarshallPlan childPlan : children) {
			// ptrArray
			final int N = childPlan.ptrArray.length;
			int k = 0;
			while (k < N) {
				int ptrIndex = childPlan.ptrArray[k++];
				if (ptrIndex < childPlan.getSizeDirectIntrinsic()) {
					ptrIndex += sizeDirect2;
				} else {
					ptrIndex += sizeDirect - childPlan.getSizeDirectIntrinsic()
							+ sizeIndirect2;
				}
				ptrArray[i++] = ptrIndex;
				int targetIndex = childPlan.ptrArray[k++];
				if (childPlan.getSizeDirectIntrinsic() + childPlan.sizeIndirect <= targetIndex)
					targetIndex = nextViIndex++;
				else
					targetIndex += sizeDirect - childPlan.getSizeDirectIntrinsic() + sizeIndirect2;
				ptrArray[i++] = targetIndex;
				// This will also correctly move variable indirect indices which
				// index past the end of the data [direct + indirect] array
			}
			// posArray
			final int P = childPlan.posArray.length;
			for (k = 0; k < P; ++k) {
				int pos = childPlan.posArray[k];
				if (0 <= pos && pos < childPlan.getSizeDirectIntrinsic()) {
					pos += sizeDirect2;
				} else if (pos < childPlan.getSizeDirectIntrinsic() + childPlan.sizeIndirect) {
					pos += sizeDirect - childPlan.getSizeDirectIntrinsic() + sizeIndirect2;
				} else {
					assert UNKNOWN_LOCATION == pos;
				}
				posArray[j++] = pos;
			}
			// housekeeping
			sizeDirect2 += childPlan.getSizeDirectIntrinsic();
			sizeIndirect2 += childPlan.sizeIndirect;
			countVariableIndirect += childPlan.countVariableIndirect;
		}
		// Construct
		return makePlanInternal(sizeDirect, sizeIndirect, ptrArray, posArray,
				countVariableIndirect);
	}

	/**
	 * Constructor for a marshall plan for indirect string data.
	 * 
	 * This is the type of data associated with the Suneido types {@code string},
	 * {@code buffer}, and {@code resource}. Because it describes data which is
	 * represented by a pointer to a string whose length can only be determined
	 * at the time the data is marshalled, the {@code ptrArray} index to the
	 * data is set to {@link #UNKNOWN_LOCATION}.
	 * @return
	 */
	public static MarshallPlan makeVariableIndirectPlan() {
		return VARIABLE_INDIRECT_PLAN;
	}

	//
	// INTERNALS
	//

	private static final int[] NO_PTR_ARRAY = new int[0];
	private static final int[] NO_POS_ARRAY = NO_PTR_ARRAY;
	private static final int[] VARIABLE_PTR_ARRAY = new int[] { 0, SizeDirect.POINTER };
	private static final int[] DIRECT_POS_ARRAY = new int[] { 0 };

	private static final MarshallPlan VARIABLE_INDIRECT_PLAN = new MarshallPlan(
			SizeDirect.POINTER, 0, VARIABLE_PTR_ARRAY, DIRECT_POS_ARRAY, 1);

	private static final MarshallPlan NULL_PLAN = new MarshallPlan(0, 0,
			NO_PTR_ARRAY, NO_POS_ARRAY, 0);

	private static MarshallPlan makePlanInternal(int sizeDirect,
			int sizeIndirect, int[] ptrArray, int[] posArray,
			int countVariableIndirect) {
		if (sizeDirect < 1) {
			if (!(0 == sizeDirect && 0 == sizeIndirect && 0 == ptrArray.length
					&& 0 == posArray.length && 0 == countVariableIndirect)) {
				throw new IllegalArgumentException(
						"Invalid MarshallPlan parameters");
			}
			return NULL_PLAN;
		}
		return new MarshallPlan(sizeDirect, sizeIndirect, ptrArray, posArray,
				countVariableIndirect);
	}

	//
	// ACCESSORS
	//

	/**
	 * <p>
	 * Get the direct storage required to marshall the data <em>onto the
	 * stack</em> under the {@code stdcall} calling convention.
	 * </p>
	 * <p>
	 * Under the {@code stdcall} calling convention, each argument uses the
	 * minimum number of 32-bit words required to contain the intrinsic size of
	 * the argument. Thus, for example, the functions
	 * {@code __stdcall void f(char, char);}
	 * {@code __stdcall void g(long, long); } and
	 * {@code __stdcall void h(int64_t, int64_t);} all
	 * require 64 bits of stack storage for the arguments.
	 * </p>
	 *
	 * @return Amount of direct storage required to marshall the data onto the
	 * stack for the {@code stdcall} calling convention 
	 * @see #getSizeDirectIntrinsic()
	 * @see #getSizeIndirect()
	 */
	public int getSizeDirectStack() {
		return sizeDirect;
	}

	/**
	 * <p>
	 * Get the direct storage required to marshall the data by packing it
	 * according to the intrinsic size of the data types (as within a "C"
	 * {@code struct}).
	 * </p>
	 * <p>
	 * This function does not presently deal with alignment issues, so if the
	 * Suneido programmer wants to align particular data members, he must do so
	 * by inserting suitable padding members if necessary.
	 * </p>
	 *
	 * @return Amount of direct storage required to marshall the data
	 * @see #getSizeDirectStack() 
	 * @see #getSizeIndirect()
	 */
	public int getSizeDirectIntrinsic() {
		return sizeDirect;
	}

	/**
	 * Get the indirect storage required to marshall the data. Indirect storage
	 * is the storage occupied by the target of a pointer (or, within a
	 * structure or array, the targets of all the pointers which are directly or
	 * indirectly part of the structure or array).
	 * @return Amount of indirect storage required to marshall the data, in
	 * bytes.
	 * @see #getSizeDirectStack()
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
	 * @see #makeVariableIndirectPlan()
	 * @see #UNKNOWN_LOCATION
	 */
	public int getCountVariableIndirect() {
		return this.countVariableIndirect;
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
		return new Marshaller(sizeDirect, sizeIndirect, countVariableIndirect,
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
					countVariableIndirect == mp.countVariableIndirect &&
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
		result.append(" }, #vi:").append(countVariableIndirect).append(" ]");
		return result.toString();
	}
}
