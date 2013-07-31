package suneido.language.jsdi;

import java.util.Arrays;

/**
 * TODO: docs
 * TODO: note in strong strong text that Marshaller can't be reused because
 *       it assumes zeroed-out data array
 * 
 * @author Victor Schappert
 * @since 20130710
 * @see MarshallPlan
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
	// PUBLIC CONSTANTS
	//

	/**
	 * If this value is present in a marshalled <em>pointer</em> array on the
	 * native side, indicates that the pointer value provided by the Java
	 * marshaller should not be changed on the native side (this can happen if a
	 * NULL pointer, or INTRESOURCE value, is passed in a memory location that
	 * would otherwise be a pointer).
	 *
	 * @see #makeVariableIndirectPlan()
	 * @see #getVariableIndirectCount()
	 */
	public static final int UNKNOWN_LOCATION = -1;

	//
	// CONSTRUCTORS
	//

	/**
	 * Deliberately package-internal. Instances should only be constructed by
	 * calling {@link MarshallPlan#makeMarshaller()}.
	 */
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

	/**
	 * <p>
	 * Returns the marshaller's internal <em>data</em> array for the purposes of
	 * passing it to {@code native} function calls.
	 * </p>
	 * <p>
	 * While the contents of this array may be modified by {@code native} calls
	 * which respect the JSDI marshalling framework, it should be treated as
	 * opaque by other Java code and not modified by Java calls under any
	 * circumstances!
	 * </p>
	 * @return Data array
	 * @see #getPtrArray()
	 * @see #getViArray()
	 * @see #getViInstArray()
	 */
	public byte[] getData() {
		return data;
	}

	/**
	 * <p>
	 * Returns the marshaller's internal <em>em pointer</em> array for the
	 * purposes of passing it to {@code native} function calls.
	 * </p>
	 * <p>
	 * The contents of this array <em>are <strong>not</strong> to be modified
	 * by anyone, under any circumstances</em>! This array <em>may</em> be a
	 * direct reference to the internal array pointer array of a
	 * {@link MarshallPlan}, or it may be a copy. In either case, modifying it
	 * will be disastrous.
	 * </p>
	 * @return Pointer array
	 * @see #getData()
	 * @see #getViArray()
	 * @see #getViInstArray()
	 */
	public int[] getPtrArray() {
		return ptrArray;
	}

	/**
	 * <p>
	 * Returns the marshaller's internal <em>variable indirect</em> array for
	 * the purposes of passing it to {@code native} function calls.
	 * </p>
	 * <p>
	 * The value returned may be {@code null} if the {@link MarshallPlan} on
	 * which this marshaller is based does not require variable indirect
	 * storage.
	 * </p>
	 * <p>
	 * The contents of this array <em>are <strong>not</strong> to be modified</em>!
	 * </p>
	 * @return Variable indirect array (may be {@code null})
	 * @since 20130718
	 * @see #getViInstArray()
	 * @see #getData()
	 * @see #getPtrArray()
	 */
	public Object[] getViArray() {
		return viArray;
	}

	/**
	 * <p>
	 * Returns the marshaller's internal <em>variable indirect instruction</em>
	 * array for the purpose of passing it to {@code native} function calls.
	 * </p>
	 * <p>
	 * The value returned may be {@code null} if the {@link MarshallPlan} on
	 * which this marshaller is based does not require variable indirect
	 * storage.
	 * </p>
	 * <p>
	 * The contents of this array <em>are <strong>not</strong> to be modified</em>!
	 * </p>
	 * @return Variable indirect instruction array (may be {@code null})
	 * @since 20130718
	 * @see #getViArray()
	 * @see #getData()
	 * @see #getPtrArray()
	 */
	public boolean[] getViInstArray() {
		return viInstArray;
	}

	//
	// MUTATORS
	//

	/**
	 * <p>
	 * Rewinds the marshaller, setting all internal positions to their initial
	 * values.
	 * </p>
	 * <p>
	 * After putting all data into the marshaller, it is necessary to
	 * {@code rewind()} in order to get data out from the beginning. In other
	 * words, you should not begin to call {@code getX()} methods until first
	 * rewinding.
	 * </p>
	 * @since 20130718
	 */
	public void rewind() {
		this.posIndex = 0;
		this.ptrIndex = 1;
		this.viIndex  = 0;
	}

	/**
	 * Puts a JSDI {@code bool} value at the next position in the marshaller.
	 * @param value Boolean value
	 * @see #getBool()
	 */
	public void putBool(boolean value) {
		if (value) {
			// 3 higher-order bytes can remain zero
			data[nextData()] = (byte) 1;
		}
	}

	/**
	 * Puts a JSDI {@code char} value at the next position in the marshaller.
	 * @param value Single-byte character value
	 * @see #getChar()
	 */
	public void putChar(byte value) {
		data[nextData()] = (byte)value;
	}

	/**
	 * Puts a JSDI {@code short} value at the next position in the marshaller.
	 * @param value 16-bit JSDI {@code short} value
	 * @see #getShort()
	 */
	public void putShort(short value) {
		int dataIndex = nextData();
		data[dataIndex + 0] = (byte) (value >>> 000);
		data[dataIndex + 1] = (byte) (value >>> 010);
	}

	/**
	 * Puts a JSDI {@code long} value at the next position in the marshaller.
	 * @param value 32-bit JSDI {@code long} value
	 * @see #getLong()
	 */
	public void putLong(int value) {
		int dataIndex = nextData();
		data[dataIndex + 0] = (byte) (value >>> 000);
		data[dataIndex + 1] = (byte) (value >>> 010);
		data[dataIndex + 2] = (byte) (value >>> 020);
		data[dataIndex + 3] = (byte) (value >>> 030);
	}

	/**
	 * Puts a JSDI {@code int64} value at the next position in the marshaller.
	 * @param value 64-bit JSDI {@code int64} value
	 * @see #getInt64()
	 */
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

	/**
	 * Puts a JSDI {@code float} value at the next position in the marshaller.
	 * @param value 32-bit JSDI {@code float} value
	 * @see #getFloat()
	 */
	public void putFloat(float value) {
		putLong(Float.floatToRawIntBits(value));
	}

	/**
	 * Puts a JSDI {@code double} value at the next position in the marshaller.
	 * @param value 64-bit JSDI {@code double} value
	 * @see #getDouble()
	 */
	public void putDouble(double value) {
		putInt64(Double.doubleToRawLongBits(value));
	}

	/**
	 * Puts a non-NULL pointer value at the next position in the marshaller.
	 * @see #putNullPtr()
	 * @see #isPtrNull()
	 * @see #putStringPtr(String, boolean)
	 * @see #putStringPtr(Buffer, boolean)
	 */
	public void putPtr() {
		skipPtr();
	}

	/**
	 * Puts a NULL pointer value at the next position in the marshaller.
	 * @see #putPtr()
	 * @see #isPtrNull()
	 * @see #putNullStringPtr(boolean)
	 */
	public void putNullPtr() {
		++posIndex;
		int ptrIndex = nextPtrIndexAndCopy();
		// Indicate to the native side that this pointer doesn't point anywhere
		ptrArray[ptrIndex] = Marshaller.UNKNOWN_LOCATION;
		// It is up to the caller to skip over any corresponding data pointer.
	}

	/**
	 * <p>
	 * Skips over the next {@code numElems} positions in the marshaller.
	 * </p>
	 * <p>
	 * This method <em>cannot</em> be used for skipping over elements of an
	 * array of compound types (<em>ie</em> {@code struct}'s) because the
	 * marshaller does not know how many internal positions are occupied by
	 * each element of the array. However, it is suitable for skipping over
	 * the elements of arrays of primitive types, since each element in a
	 * primitive array corresponds to only one position in the marshaller.
	 * </p>
	 * @param numElems Number of positions to skip
	 * @see #skipComplexElement(ElementSkipper)
	 */
	public void skipBasicArrayElements(int numElems) {
		posIndex += numElems;
	}

	/**
	 * <p>
	 * Skips over all of the positions occupied by the complex element
	 * described by {@code skipper}.
	 * </p>
	 * @param skipper Object indicating how many positions to skip over
	 * @see #skipBasicArrayElements(int)
	 */
	public void skipComplexElement(ElementSkipper skipper) {
		posIndex += skipper.nPos;
		ptrIndex += skipper.nPtr;
	}

	/**
	 * Puts a non-NULL pointer to variable indirect storage at the next position
	 * in the marshaller.
	 * @param value Non-{@code null} string value to marshall into variable
	 * indirect storage
	 * @param expectStringBack Instruction to place in the variable indirect
	 * instructions array ({@code true} indicates that the native-side
	 * unmarshalling code should return a {@link String})
	 * @see #putStringPtr(Buffer, boolean)
	 * @see #putNullStringPtr(boolean)
	 * @see #putPtr()
	 */
	public void putStringPtr(String value, boolean expectStringBack) {
		skipPtr();
		int viIndex = nextVi();
		viArray[viIndex] = StringConversions.stringToZeroTerminatedByteArray(value);
		viInstArray[viIndex] = expectStringBack;
	}

	/**
	 * Puts a non-NULL pointer to the internal data array of a {@link Buffer}
	 * at the next position in the marshaller.
	 * @param value Non-{@code null} buffer value whose internal data array is
	 * put into variable indirect storage
	 * @param expectStringBack Instruction to place in the variable indirect
	 * instructions array ({@code true} indicates that the native-side
	 * unmarshalling code should return a {@link String})
	 * @see #putStringPtr(String, boolean)
	 * @see #putNullStringPtr(boolean)
	 * @see #putPtr()
	 */
	public void putStringPtr(Buffer value, boolean expectStringBack) {
		skipPtr();
		int viIndex = nextVi();
		viArray[viIndex] = value.getInternalData();
		viInstArray[viIndex] = expectStringBack;
	}

	/**
	 * Puts a NULL pointer to variable indirect storage at the next position in
	 * the marshaller.
	 * @param expectStringBack Instruction to place in the variable indirect
	 * instructions array ({@code true} indicates that the native-side
	 * unmarshalling code should return a {@link String})
	 * @see #putStringPtr(String, boolean)
	 * @see #putStringPtr(Buffer, boolean)
	 * @see #putNullPtr()
	 */
	public void putNullStringPtr(boolean expectStringBack) {
		++posIndex;
		int ptrIndex = nextPtrIndexAndCopy();
		ptrArray[ptrIndex] = Marshaller.UNKNOWN_LOCATION;
		int viIndex = nextVi();
		viInstArray[viIndex] = expectStringBack;
		// Assert: the skipped spot in the viArray is null
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
		return 0 == getLong();
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