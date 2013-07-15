package suneido.language.jsdi;

import suneido.language.jsdi.type.SizeDirect;

// TODO: Make test
// TODO: DOC
public final class MarshallPlan {

	//
	// DATA
	//

	private final int sizeDirect;
	private final int sizeIndirect;
	private final int[] ptrArray;
	private final boolean hasVariableIndirect;

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
	 * @see #hasVariableIndirect()
	 */
	public static final int UNKNOWN_LOCATION = -1;

	//
	// CONSTRUCTORS
	//

	private MarshallPlan(int sizeDirect, int sizeIndirect, int[] ptrArray,
			boolean hasVariableIndirect) {
		assert 0 < sizeDirect;
		this.sizeDirect          = sizeDirect;
		this.sizeIndirect        = sizeIndirect;
		this.ptrArray            = ptrArray;
		this.hasVariableIndirect = hasVariableIndirect;
	}

	/**
	 * Plain value constructor.
	 *
	 * @param sizeDirect Direct size of the data to marshall.
	 * @author Victor Schappert
	 * @since 20130702
	 */
	public static MarshallPlan makeDirectPlan(int sizeDirect) {
		return new MarshallPlan(sizeDirect, 0, new int[0], false);
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
		final int sizeDirect = SizeDirect.POINTER;
		final int sizeIndirect = targetPlan.sizeDirect + targetPlan.sizeIndirect;
		final int N = 2 + targetPlan.ptrArray.length;
		final int[] ptrArray = new int[N];
		ptrArray[1] = SizeDirect.POINTER; // assert: ptrArray[0] = 0
		int i = 0, j = 2;
		while (j < N) {
			ptrArray[j++] = targetPlan.ptrArray[i++] + SizeDirect.POINTER;
			ptrArray[j++] = movePtrIndex(targetPlan.ptrArray[i++],
					SizeDirect.POINTER);
		}
		return new MarshallPlan(sizeDirect, sizeIndirect, ptrArray,
				targetPlan.hasVariableIndirect);
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
		assert 0 < numElems;
		final int sizeDirect = numElems * elementPlan.sizeDirect;
		final int sizeIndirect = numElems * elementPlan.sizeIndirect;
		final int M = elementPlan.ptrArray.length;
		final int[] ptrArray = new int[numElems * M];
		int k = 0; // index into ptrArray
		int offsetDirect = 0;
		int offsetIndirect = (numElems - 1) * elementPlan.sizeDirect;
		for (int i = 0; i < numElems; ++i) {
			int j = 0;
			while (j < M) {
				ptrArray[k++] = elementPlan.ptrArray[j++] + offsetDirect;
				ptrArray[k++] = movePtrIndex(elementPlan.ptrArray[j++],
						offsetIndirect);
			}
			offsetDirect += elementPlan.sizeDirect;
			offsetIndirect += elementPlan.sizeIndirect;
		}
		return new MarshallPlan(sizeDirect, sizeIndirect, ptrArray,
				elementPlan.hasVariableIndirect);
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
		int sizeDirect = 0;
		for (MarshallPlan child : children) {
			sizeDirect += child.sizeDirect;
			sizePtrArray += child.ptrArray.length;
		}
		final int[] ptrArray = new int[sizePtrArray];
		int sizeDirect2 = 0;
		int sizeIndirect = 0;
		boolean hasVariableIndirect = false;
		int i = 0;
		for (MarshallPlan child : children) {
			final int N = child.ptrArray.length;
			int j = 0;
			while (j < N) {
				int ptrIndex = child.ptrArray[j++];
				if (ptrIndex < child.sizeDirect) {
					ptrIndex += sizeDirect2;
				} else {
					ptrIndex += sizeDirect - child.sizeDirect + sizeIndirect;
				}
				ptrArray[i++] = ptrIndex;
				int targetIndex = child.ptrArray[j++];
				assert UNKNOWN_LOCATION == targetIndex || 
						(child.sizeDirect <= targetIndex
						&& targetIndex < child.sizeDirect + child.sizeIndirect);
				ptrArray[i++] = movePtrIndex(targetIndex, sizeDirect - child.sizeDirect + sizeIndirect);
			}
			sizeDirect2 += child.sizeDirect;
			sizeIndirect += child.sizeIndirect;
			hasVariableIndirect |= child.hasVariableIndirect;
		}
		// Sort by index of the pointer storage (not the target storage)...
		// Double-indirects can cause the array to become unordered by pointer
		// storage location.
		sortTupleArray(ptrArray, ptrArray.length);
		// Construct
		return new MarshallPlan(sizeDirect, sizeIndirect, ptrArray,
				hasVariableIndirect);
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

	private static final MarshallPlan VARIABLE_INDIRECT_PLAN = new MarshallPlan(
			SizeDirect.POINTER, 0, new int[] { 0, UNKNOWN_LOCATION }, true);

	//
	// INTERNALS
	//

	private static int movePtrIndex(int ptrIndex, int offset) {
		return UNKNOWN_LOCATION != ptrIndex
			? ptrIndex + offset
			: UNKNOWN_LOCATION
			;
	}

	private static void sortTupleArray(final int[] arr, final int N) {
		// This is just a simple selection sort, since most pointer arrays
		// aren't big in practice and selection sort, although O(n^2), works
		// well on small inputs. In the unlikely event performance becomes an
		// issue, we can make it into a heap sort.
		for (int endIndex = 0; endIndex < N; endIndex += 2) {
			int minIndex = endIndex;
			int tmp = arr[minIndex];
			int min = tmp;
			// Search for the minimum value
			for (int searchIndex = endIndex + 2; searchIndex < N; searchIndex += 2) {
				if (arr[searchIndex] < min) {
					min = arr[searchIndex];
					minIndex = searchIndex;
				}
			}
			// Swap the minimum value
			final int min2 = arr[minIndex + 1];
			final int tmp2 = arr[endIndex + 1];
			arr[endIndex] = min;
			arr[endIndex + 1] = min2;
			arr[minIndex] = tmp;
			arr[minIndex + 1] = tmp2;
		}
	}

	//
	// ACCESSORS
	//

	/**
	 * Get the direct storage required to marshall the data. Direct storage is
	 * the storage occupied by a value on the stack, or within another
	 * structure.
	 * @return Amount of direct storage required to marshall the data, in bytes.
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
	 * @see #getSizeDirect()
	 */
	public int getSizeIndirect() {
		return sizeIndirect;
	}

	/**
	 * Returns the pointer array.
	 * <p>
	 * The pointer array is an array containing an even number of integers where
	 * for each successive tuple
	 * <code>&lt;x<sub>i</sub>,&nbsp;x<sub>i+1</sub>&gt;</code>,
	 * <code>x<sub>i</sub></code> represents the index into the data array which
	 * contains a placeholder for a pointer, and <code>x<sub>i+1</sub></code>
	 * represents the index into the data array which contains the data
	 * pointed-to.
	 * </p>
	 * <p>
	 * The caller must treat the array returned as read-only and not modify it.
	 * </p>
	 * 
	 * @return The pointer array. DO NOT MODIFY THE ARRAY RETURNED.
	 */
	public int[] getPtrArray() {
		return ptrArray;
	}

	/**
	 * Returns {@code true} iff the plan has variable indirect storage
	 * (<em>ie</em> the amount of indirect storage is determinable only at the
	 * time the data is actually marshalled). 
	 * @return Whether the marshall plan requires a variable amount of indirect
	 * storage.
	 * @see #makeVariableIndirectPlan()
	 * @see #UNKNOWN_LOCATION
	 */
	public boolean hasVariableIndirect() {
		return this.hasVariableIndirect;
	}

	/**
	 * Creates a marshaller instance for marshalling all data described by this
	 * plan, both direct and indirect.
	 *
	 * @return Marshaller based on this plan
	 */
	public Marshaller makeMarshaller() {
		return new Marshaller(sizeDirect, sizeIndirect);
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
		int k = 0;
		if (k < N) {
			result.append(' ').append(ptrArray[k++]).append(':')
					.append(ptrArray[k++]);
			while (k < N) {
				result.append(", ").append(ptrArray[k++]).append(':')
						.append(ptrArray[k++]);
			}
		}
		if (hasVariableIndirect) {
			result.append(" }, vi ]");
		} else {
			result.append(" }, no-vi ]");
		}
		return result.toString();
	}
}
