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
	 * @see #getSizeIndirect()
	 */
	public int getSizeDirect() {
		return sizeDirect;
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
