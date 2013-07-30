package suneido.language.jsdi;

import suneido.language.jsdi.type.PrimitiveSize;

/**
 * Helper functions for making {@link Marshaller}'s and {@link MarshallPlan}'s.
 * 
 * @author Victor Schappert
 * @since 20130725
 */
public final class MarshallTestUtil {

	public static MarshallPlan nullPlan() {
		return new MarshallPlan(0, 0, new int[0], new int[0], 0);
	}

	public static MarshallPlan directPlan(int sizeDirect) {
		return new MarshallPlan(sizeDirect, 0, new int[0], new int[] { 0 }, 0);
	}

	public static MarshallPlan pointerPlan(int sizeDirect) {
		return new MarshallPlan(PrimitiveSize.POINTER, sizeDirect, new int[] {
				0, PrimitiveSize.POINTER }, new int[] { 0,
				PrimitiveSize.POINTER }, 0);
	}

	public static MarshallPlan arrayPlan(int sizeDirect, int numElems) {
		int[] posArray = new int[numElems];
		for (int k = 1; k < numElems; ++k) {
			posArray[k] = sizeDirect * k;
		}
		return new MarshallPlan(sizeDirect * numElems, 0, new int[0], posArray,
				0);
	}

	public static MarshallPlan variableIndirectPlan() {
		return new MarshallPlan(PrimitiveSize.POINTER, 0, new int[] { 0,
				PrimitiveSize.POINTER },
				new int[] { 0, PrimitiveSize.POINTER }, 1);
	}

	public static MarshallPlan compoundPlan(int numArrayElems,
			int... sizeDirect) {
		final int numMembers = sizeDirect.length;
		int[] posArray = new int[numMembers * numArrayElems];
		int totalSizeDirect = sizeDirect[0];
		for (int i = 1; i < numMembers; ++i) {
			posArray[i] = posArray[i - 1] + sizeDirect[i - 1];
			totalSizeDirect += sizeDirect[i];
		}
		totalSizeDirect *= numArrayElems;
		for (int j = 1; j < numArrayElems; ++j) {
			posArray[numMembers * j] = posArray[numMembers * j - 1]
					+ sizeDirect[numMembers - 1];
			for (int k = 1; k < numMembers; ++k) {
				posArray[numMembers * j + k] = posArray[numMembers * j + k - 1]
						+ sizeDirect[k - 1];
			}
		}
		return new MarshallPlan(totalSizeDirect, 0, new int[0], posArray, 0);
	}

	public static MarshallPlan paramPlan(int... sizeDirect) {
		int[] sizeDirect2 = new int[sizeDirect.length];
		for (int k = 0; k < sizeDirect.length; ++k) {
			sizeDirect2[k] = PrimitiveSize.sizeWholeWords(sizeDirect[k]);
		}
		return compoundPlan(1, sizeDirect2);
	}
}
