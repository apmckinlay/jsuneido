package suneido.language.jsdi;

import java.util.Arrays;

/**
 * TODO: docs
 * 
 * @author Victor Schappert
 * @since 20130710
 */
@DllInterface
public final class Marshaller {

	//
	// DATA
	//

	private final byte[] data;
	private int[] ptrArray; // either ref to MarshallPlan's or copy
	private final int[] posArray; // reference to MarshallPlan's array
	private int ptrIndex; // index into ptrArray
	private int posIndex; // index into posArray
	private boolean isPtrArrayCopied; // whether ptrArray is a copy
	private final Object[] viArray; // byte[], String, or null
	private final boolean[] viInstArray; // native side should make strings?
	private int viIndex; // index into viArray and viInstArray

	//
	// CONSTRUCTORS
	//

	Marshaller(int sizeDirect, int sizeIndirect, int countVariableIndirect,
			int[] ptrArray, int[] posArray) {
		this.data = new byte[sizeDirect + sizeIndirect];
		this.ptrArray = ptrArray;
		this.posArray = posArray;
		if (0 < countVariableIndirect) {
			this.viArray = new Object[countVariableIndirect];
			this.viInstArray = new boolean[countVariableIndirect];
		} else {
			this.viArray = null;
			this.viInstArray = null;
		}
		this.isPtrArrayCopied = false;
		rewind();
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

	// TODO: doc -- since 20130718
	public Object[] getViArray() {
		return viArray;
	}

	// TODO: doc -- since 20130718
	public boolean[] getViInstArray() {
		return viInstArray;
	}

	//
	// MUTATORS
	//

	// TODO: doc -- since 20130718
	public void rewind() {
		this.posIndex = 0;
		this.ptrIndex = 1;
		this.viIndex  = 0;
	}

	public void putBool(boolean value) {
		if (value) {
			// 3 higher-order bytes can remain zero
			data[nextData()] = (byte) 1;
		}
	}

	public void putChar(byte value) {
		data[nextData()] = (byte)value;
	}

	public void putShort(short value) {
		int dataIndex = nextData();
		data[dataIndex + 0] = (byte) (value >>> 000);
		data[dataIndex + 1] = (byte) (value >>> 010);
	}

	public void putLong(int value) {
		int dataIndex = nextData();
		data[dataIndex + 0] = (byte) (value >>> 000);
		data[dataIndex + 1] = (byte) (value >>> 010);
		data[dataIndex + 2] = (byte) (value >>> 020);
		data[dataIndex + 3] = (byte) (value >>> 030);
	}

	public void putInt64(long value) {
		int dataIndex = nextData();
		data[dataIndex + 0] = (byte) (value >>> 000);
		data[dataIndex + 1] = (byte) (value >>> 010);
		data[dataIndex + 2] = (byte) (value >>> 020);
		data[dataIndex + 3] = (byte) (value >>> 030);
		data[dataIndex + 4] = (byte) (value >>> 040);
		data[dataIndex + 5] = (byte) (value >>> 050);
		data[dataIndex + 6] = (byte) (value >>> 060);
		data[dataIndex + 7] = (byte) (value >>> 070);
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
		++posIndex;
		int ptrIndex = nextPtrIndexAndCopy();
		// Indicate to the native side that this pointer doesn't point anywhere
		ptrArray[ptrIndex] = MarshallPlan.UNKNOWN_LOCATION;
		// It is up to the caller to skip over any corresponding data pointer.
	}

	// TODO: note in doc -- you can't do this with arrays of structs because
	// you have to skip over all the relevant posArray elements for
	// submembers
	public void skipBasicArrayElements(int numElems) {
		posIndex += numElems;
	}

	public void skipComplexArrayElements(ElementSkipper skipper) {
		posIndex += skipper.nPos;
		ptrIndex += skipper.nPtr;
	}

	public void putNullStringPtr(boolean expectStringBack) {
		int ptrIndex = nextPtrIndexAndCopy();
		ptrArray[ptrIndex] = MarshallPlan.UNKNOWN_LOCATION;
		int viIndex = nextVi();
		viInstArray[viIndex] = expectStringBack;
		// Assert: the skipped spot in the viArray is null
	}

	public void putStringPtr(String value, boolean expectStringBack) {
		skipPtr();
		int viIndex = nextVi();
		viArray[viIndex] = StringConversions.stringToZeroTerminatedByteArray(value);
		viInstArray[viIndex] = expectStringBack;
	}

	public void putStringPtr(Buffer value, boolean expectStringBack) {
		skipPtr();
		int viIndex = nextVi();
		viArray[viIndex] = value.getInternalData();
		viInstArray[viIndex] = expectStringBack;
	}

	public void putZeroTerminatedStringDirect(String value, int maxChars) {
		int dataIndex = nextData();
		Buffer.copyStr(value, data, dataIndex, Math.min(maxChars - 1, value.length()));
	}

	// TODO: in docs note that maxchars includes the zero terminator 
	public void putZeroTerminatedStringDirect(Buffer value, int maxChars) {
		int dataIndex = nextData();
		value.copyInternalData(data, dataIndex, maxChars - 1);
	}

	public void putNonZeroTerminatedStringDirect(String value, int maxChars) {
		int dataIndex = nextData();
		Buffer.copyStr(value, data, dataIndex, Math.min(maxChars, value.length()));
	}

	public void putNonZeroTerminatedStringDirect(Buffer value, int maxChars) {
		int dataIndex = nextData();
		value.copyInternalData(data, dataIndex, maxChars);
	}

	public boolean getBool() {
		return getLong() != 0;
	}

	public int getChar() {
		return data[nextData()];
	}

	public int getShort() {
		final int dataIndex = nextData();
		// Note: the bitwise AND with 0xff is to avoid EVIL Java sign extension
		//       (because Java promotes bitwise operands to int and then sign-
		//       extends the 0xff byte).
		return (data[dataIndex + 0] & 0xff) << 000 |
				data[dataIndex + 1] << 010;
	}

	public int getLong() {
		final int dataIndex = nextData();
		return
			(data[dataIndex + 0] & 0xff) << 000 |
			(data[dataIndex + 1] & 0xff) << 010 |
			(data[dataIndex + 2] & 0xff) << 020 |
			data[dataIndex + 3] << 030;
	}

	public long getInt64() {
		final int dataIndex = nextData();
		return
				  (data[dataIndex + 0] & 0xffL) << 000
				| (data[dataIndex + 1] & 0xffL) << 010
				| (data[dataIndex + 2] & 0xffL) << 020
				| (data[dataIndex + 3] & 0xffL) << 030
				| (data[dataIndex + 4] & 0xffL) << 040
				| (data[dataIndex + 5] & 0xffL) << 050
				| (data[dataIndex + 6] & 0xffL) << 060
				| (long) data[dataIndex + 7] << 070;
	}

	public float getFloat() {
		return Float.intBitsToFloat(getLong());
	}

	public double getDouble() {
		return Double.longBitsToDouble(getInt64());
	}

	// TODO: docs since 20130717 ... return true iff a null pointer
	public boolean isPtrNull() {
		boolean result = MarshallPlan.UNKNOWN_LOCATION == ptrArray[ptrIndex];
		skipPtr();
		return result;
	}

	public Object getStringPtr() {
		int viIndex = nextVi();
		Object value = viArray[viIndex];
		if (null == value) {
			return Boolean.FALSE;
		} else if (viInstArray[viIndex]) {
			assert value instanceof String;
			return value;
		} else {
			return getStringPtrAlwaysByteArray(null, value);
		}
	}

	public Object getStringPtrAlwaysByteArray(Buffer oldValue) {
		int viIndex = nextVi();
		assert false == viInstArray[viIndex];
		return getStringPtrAlwaysByteArray(oldValue, viArray[viIndex]);
	}

	public Object getStringPtrMaybeByteArray(Buffer oldValue) {
		int viIndex = nextVi();
		Object value = viArray[viIndex];
		if (! viInstArray[viIndex]) {
			return getStringPtrAlwaysByteArray(oldValue, value);
		} else if (null == value) {
			return Boolean.FALSE;
		} else {
			assert value instanceof String;
			return value;
		}
	}

	public String getZeroTerminatedStringDirect(int numChars) {
		int dataIndex = nextData();
		if (0 < numChars) {
			byte b = data[dataIndex];
			if (0 == b) return "";
			StringBuilder sb = new StringBuilder(numChars);
			sb.append((char)b);
			for (int k = 1; k < numChars; ++k) {
				b = data[++dataIndex];
				if (0 == b) break;
				sb.append((char)b);
			}
			return sb.toString();
		} else {
			return "";
		}
	}

	public Buffer getNonZeroTerminatedStringDirect(int numChars, Buffer oldValue) {
		int dataIndex = nextData();
		if (oldValue != null && numChars <= oldValue.capacity()) {
			oldValue.setAndSetSize(data, dataIndex, dataIndex + numChars);
			return oldValue;
		} else {
			return new Buffer(data, dataIndex, dataIndex + numChars);
		}
	}

	//
	// INTERNALS
	//

	private int nextData() {
		return posArray[posIndex++];
	}

// TODO: delete me
//	private int nextPtr() {
//		final int ptr = ptrArray[ptrIndex];
//		ptrIndex += 2;
//		return ptr;
//	}

	private int nextPtrIndexAndCopy() {
		copyPtrArray();
		final int _ptrIndex = ptrIndex;
		ptrIndex += 2;
		return _ptrIndex;
	}

	private int nextVi() {
		return viIndex++;
	}

	private void skipPtr() {
		ptrIndex += 2; // skip over pointer in ptrArray
		++posIndex;    // skip over pointer in posArray
	}

	private void copyPtrArray() {
		if (!isPtrArrayCopied) {
			ptrArray = Arrays.copyOf(ptrArray, ptrArray.length);
			isPtrArrayCopied = true;
		}
	}

	private static Object getStringPtrAlwaysByteArray(Buffer oldValue, Object value) {
		if (null == value) {
			return Boolean.FALSE;
		} else if (null == oldValue) {
			// This really should never happen, unless a concurrently executing
			// thread deleted the buffer from the container between the time
			// that it was marshalled in and now.
			byte[] b = (byte[])value;
			return new Buffer(b, 0, b.length);
		} else {
			// In the ordinary course, oldValue will be the Buffer that was
			// originally marshalled in, and since it's data array was passed
			// by reference to the native side, it has already inherited the
			// changes. If the Suneido programmer is running concurrent threads
			// and replaced the original buffer with a new one, he deserves to
			// get a stale value!
			return oldValue;
		}
	}
}