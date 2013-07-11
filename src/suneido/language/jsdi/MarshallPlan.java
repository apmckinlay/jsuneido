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

	//
	// CONSTRUCTORS
	//

	/**
	 * Plain value constructor.
	 *
	 * @param sizeDirect Direct size of the data to marshall.
	 * @author Victor Schappert
	 * @since 20130702
	 */
	public MarshallPlan(int sizeDirect) {
		assert 0 < sizeDirect;
		this.sizeDirect = sizeDirect;
		this.sizeIndirect = 0;
		this.ptrArray = new int[0];
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
	public MarshallPlan(MarshallPlan targetPlan) {
		sizeDirect = SizeDirect.POINTER;
		sizeIndirect = targetPlan.sizeDirect + targetPlan.sizeIndirect;
		final int N = 2 + targetPlan.ptrArray.length;
		ptrArray = new int[N];
		ptrArray[1] = SizeDirect.POINTER; // assert: ptrArray[0] = 0
		int i = 0, j = 2;
		while (j < N) {
			ptrArray[j++] = SizeDirect.POINTER + targetPlan.ptrArray[i++];
			ptrArray[j++] = SizeDirect.POINTER + targetPlan.ptrArray[i++];
		}
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
	public MarshallPlan(MarshallPlan elementPlan, int numElems) {
		assert 0 < numElems;
		sizeDirect = numElems * elementPlan.sizeDirect;
		sizeIndirect = numElems * elementPlan.sizeIndirect;
		final int M = elementPlan.ptrArray.length;
		ptrArray = new int[numElems * M];
		int k = 0; // index into ptrArray
		int offsetDirect = 0;
		int offsetIndirect = (numElems - 1) * elementPlan.sizeDirect;
		for (int i = 0; i < numElems; ++i) {
			int j = 0;
			while (j < M) {
				ptrArray[k++] = elementPlan.ptrArray[j++] + offsetDirect;
				ptrArray[k++] = elementPlan.ptrArray[j++] + offsetIndirect;
			}
			offsetDirect += elementPlan.sizeDirect;
			offsetIndirect += elementPlan.sizeIndirect;
		}
	}

	/**
	 * Structure/parameter constructor.
	 * 
	 * @param children
	 *            List of marshalling plans for each structure member, in order.
	 * @author Victor Schappert
	 * @since 20130702
	 */
	public MarshallPlan(Iterable<MarshallPlan> children) {
		int sizePtrArray = 0;
		int sizeDirect_ = 0;
		for (MarshallPlan child : children) {
			sizeDirect_ += child.sizeDirect;
			sizePtrArray += child.ptrArray.length;
		}
		this.ptrArray = new int[sizePtrArray];
		this.sizeDirect = sizeDirect_;
		int sizeDirect2_ = 0;
		int sizeIndirect_ = 0;
		int i = 0;
		for (MarshallPlan child : children) {
			final int N = child.ptrArray.length;
			int j = 0;
			while (j < N) {
				int ptrIndex = child.ptrArray[j++];
				if (ptrIndex < child.sizeDirect) {
					ptrIndex += sizeDirect2_;
				} else {
					ptrIndex += this.sizeDirect - child.sizeDirect
							+ sizeIndirect_;
				}
				ptrArray[i++] = ptrIndex;
				int targetIndex = child.ptrArray[j++];
				assert child.sizeDirect <= targetIndex
						&& targetIndex < child.sizeDirect + child.sizeIndirect;
				targetIndex += this.sizeDirect - child.sizeDirect
						+ sizeIndirect_;
				ptrArray[i++] = targetIndex;
			}
			sizeDirect2_ += child.sizeDirect;
			sizeIndirect_ += child.sizeIndirect;
		}
		sizeIndirect = sizeIndirect_;
		// Sort by index of the pointer storage (not the target storage)...
		// Double-indirects can cause the array to become unordered by pointer
		// storage location.
		sortTupleArray(ptrArray, ptrArray.length);
	}

	//
	// INTERNALS
	//

	private static void sortTupleArray(final int[] arr, final int N) {
		// This is just a simple selection sort, since most pointer arrays
		// aren't big in practise and selection sort, although O(n^2), works
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
		result.append(" } ]");
		return result.toString();
	}
}
