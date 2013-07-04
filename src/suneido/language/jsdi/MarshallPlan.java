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

	public MarshallPlan(int sizeDirect) {
		assert 0 < sizeDirect;
		this.sizeDirect = sizeDirect;
		this.sizeIndirect = 0;
		this.ptrArray = new int[0];
	}

	public MarshallPlan(int sizeDirect, int sizeIndirect, int[] ptrArray) {
		assert 0 < sizeDirect && 0 < sizeIndirect;
		assert ptrArray.length / 2 <= sizeDirect / SizeDirect.POINTER
			: "Must have direct space for each pointer";
		assert 0 <= sizeIndirect;
		assert ptrArray.length / 2 <= sizeIndirect
			: "Must have at least one byte of indirect space for each pointer";
		this.sizeDirect = sizeDirect;
		this.sizeIndirect = sizeIndirect;
		this.ptrArray = ptrArray;
	}

	public MarshallPlan(Iterable<MarshallPlan> children) {
		int sizeDirect_ = 0;
		int sizePtrArray = 0;
		for (MarshallPlan child : children) {
			sizeDirect_ += child.sizeDirect;
			sizePtrArray += child.ptrArray.length;
		}
		sizeDirect = sizeDirect_;
		ptrArray = new int[sizePtrArray];
		int i = 0;
		int sizeIndirect_ = 0;
		for (MarshallPlan child : children) {
			final int N = child.ptrArray.length;
			int j = 0;
			while (j < N) {
				ptrArray[i++] = child.ptrArray[j++] + sizeIndirect_;
				ptrArray[i++] = child.ptrArray[j++] + sizeIndirect_;
			}
			sizeIndirect_ += child.sizeIndirect;
		}
		sizeIndirect = sizeIndirect_;
	}

	//
	// ACCESSORS
	//

	public int getSizeDirect() {
		return sizeDirect;
	}

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

	//
	// ANCESTOR CLASS: Object
	//

	@Override
	public String toString() {
		StringBuilder result = new StringBuilder(128);
		result.append("MarshallPlan[").append(sizeDirect).append(", ")
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
