package suneido.language.jsdi;

import java.util.Arrays;

/**
 * TODO: docs
 * @author Victor Schappert
 * @since 20130710
 */
@DllInterface
public final class Marshaller {

	//
	// DATA
	//

	private final byte[]  data;
	private       int[]   ptrArray;     // either ref to MarshallPlan's or copy
	private final int[]   posArray;     // reference to MarshallPlan's array
	private       int     ptrIndex;              // index into ptrArray
	private       int     posIndex;              // index into posArray
	private       boolean isPtrArrayCopied;      // whether ptrArray is a copy
	private final ExtensibleByteBuffer variableIndirectStorage;

	//
	// CONSTRUCTORS
	//

	Marshaller(int sizeDirect, int sizeIndirect, int countVariableIndirect,
			int[] ptrArray, int[] posArray) {
		this.data = new byte[sizeDirect + sizeIndirect];
		this.ptrArray = ptrArray;
		this.posArray = posArray;
		this.posIndex = 0;
		this.ptrIndex = 1;
		this.variableIndirectStorage = 0 == countVariableIndirect
				? null
				: new ExtensibleByteBuffer(
					Math.min(
						countVariableIndirect * VARIABLE_INDIRECT_PER_INITIAL_BYTES,
						VARIABLE_INDIRECT_MAX_INITIAL_BYTES	
					)
				);
		this.isPtrArrayCopied = false;
	}

	//
	// ACCESSORS
	//

	// TODO: doc -- NO CHANGING!
	public byte[] getData() {
		return data;
	}

	// TODO: doc -- NO CHANGING -- could belong to a MarshallPlan!
	public int[] getPtrArray() {
		return ptrArray;
	}

	//
	// MUTATORS
	//

	public void putBool(boolean value) {
		if (value) {
			// Other 3 bytes can remain zero. 
			data[nextData()] = (byte)1;
		}
	}

	public void putChar(byte value) {
		data[nextData()] = value;
	}

	public void putShort(short value) {
		int dataIndex = nextData();
		data[dataIndex+0] = (byte)(value & 0xff);
		data[dataIndex+1] = (byte)(value >> 8);
	}

	public void putLong(int value) {
		int dataIndex = nextData();
		data[dataIndex+0] = (byte)(value >>  0);
		data[dataIndex+1] = (byte)(value >>  8);
		data[dataIndex+2] = (byte)(value >> 16);
		data[dataIndex+3] = (byte)(value >> 24);
	}

	public void putInt64(long value) {
		int dataIndex = nextData();
		data[dataIndex+0] = (byte)(value >>  0);
		data[dataIndex+1] = (byte)(value >>  8);
		data[dataIndex+2] = (byte)(value >> 16);
		data[dataIndex+3] = (byte)(value >> 24);
		data[dataIndex+4] = (byte)(value >> 32);
		data[dataIndex+5] = (byte)(value >> 40);
		data[dataIndex+6] = (byte)(value >> 48);
		data[dataIndex+7] = (byte)(value >> 56);
	}

	public void putFloat(float value) {
		putLong(Float.floatToRawIntBits(value));
	}

	public void putDouble(double value) {
		putInt64(Double.doubleToRawLongBits(value));
	}

	public void putPtr() {
		skipPtr();
	}

	public void putNullPtr() {
		int ptrIndex = nextPtr();
		// Indicate to the native side that this pointer doesn't point anywhere
		ptrArray[ptrIndex] = MarshallPlan.UNKNOWN_LOCATION;
		// It is up to the caller to skip over any corresponding data pointer.
	}

	public void putBoolPtr(boolean value) {
		skipPtr();
		putBool(value);
	}

	public void putCharPtr(byte value) {
		skipPtr();
		putChar(value);
	}

	public void putShortPtr(short value) {
		skipPtr();
		putShort(value);
	}

	public void putLongPtr(int value) {
		skipPtr();
		putLong(value);
	}

	public void putInt64Ptr(long value) {
		skipPtr();
		putInt64(value);
	}

	public void putFloatPtr(float value) {
		skipPtr();
		putFloat(value);
	}

	public void putDoublePtr(double value) {
		skipPtr();
		putDouble(value);
	}

	// TODO: note in doc -- you can't do this with arrays of structs because
	//       you have to skip over all the relevant posArray elements for
	//       submembers
	public void skipBasicArrayElements(int numElems) {
		posIndex += numElems;
	}

	public void skipComplexArrayElements(int numElems, MarshallPlan plan) {
		posIndex += numElems * plan.getPosArraySize();
		ptrIndex += numElems * plan.getPtrArraySize();
	}

	public void putStringIndirect(String value, int numTrailingZeros) {
		// Update the pointer array to point to the beginning of this string
		int ptrIndex = nextPtr();
		ptrArray[ptrIndex] = data.length + variableIndirectStorage.pos;
		// Store the string in the variable indirect area and advance the
		// variable indirect cursor past the zero terminator
		variableIndirectStorage.putStr(value, numTrailingZeros);
	}

	public void putZeroTerminatedStringDirect(String value, int maxChars) {
		int dataIndex = nextData();
		copyStr(value, dataIndex, Math.min(maxChars - 1, value.length()));
	}

	public void putNonZeroTerminatedStringDirect(String value, int maxChars) {
		int dataIndex = nextData();
		copyStr(value, dataIndex, Math.min(maxChars,  value.length()));
	}

	//
	// INTERNALS
	//

	private static final int VARIABLE_INDIRECT_PER_INITIAL_BYTES = 256;
	private static final int VARIABLE_INDIRECT_MAX_INITIAL_BYTES = 2048;

	private int nextData() {
		return posArray[posIndex++];
	}

	private int nextPtr() {
		copyPtrArray();
		final int _ptrIndex = ptrIndex;
		ptrIndex += 2;
		return _ptrIndex;
	}

	private void skipPtr() {
		ptrIndex += 2;
		++posIndex;
	}

	private void copyPtrArray() {
		if (! isPtrArrayCopied) {
			ptrArray = Arrays.copyOf(ptrArray, ptrArray.length);
			isPtrArrayCopied = true;
		}
	}

	private int copyStr(String value, int start, int length) {
		int j = start;
		for (int i = 0; i < length; ++i) {
			char ch = value.charAt(i);
			data[j++] = (byte)ch;
		}
		return j;
	}

	//
	// INTERNAL TYPES
	//

	private static final class ExtensibleByteBuffer {
		byte[] arr;
		int    pos;
		public ExtensibleByteBuffer(int initialCapacity) {
			assert 0 < initialCapacity;
			arr = new byte[initialCapacity];
		}
		public void putStr(String value, int numTrailingZeros) {
			final int N = value.length();
			checkCapacity(N + numTrailingZeros);
			for (int i = 0; i < N; ++i) {
				char ch = value.charAt(i);
				arr[pos++] = (byte)ch;
			}
			pos += numTrailingZeros;
		}
		private void checkCapacity(int sizeNeeded) {
			if (arr.length < pos + sizeNeeded) {
				arr = Arrays.copyOf(arr,
						Math.max(arr.length + sizeNeeded, 2 * arr.length));
			}
		}
	}
}