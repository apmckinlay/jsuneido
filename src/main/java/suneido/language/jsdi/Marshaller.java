/* Copyright 2013 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido.language.jsdi;

import static suneido.language.jsdi.VariableIndirectInstruction.NO_ACTION;
import static suneido.language.jsdi.VariableIndirectInstruction.RETURN_JAVA_STRING;
import static suneido.language.jsdi.VariableIndirectInstruction.RETURN_RESOURCE;

import java.util.Arrays;

/**
 * TODO: docs
 * TODO: note in strong strong text that Marshaller can't be reused because
 *       it assumes zeroed-out data array
 * TODO: per discussion w APM note reason why Marshaller can't be cached and
 *       reused even if you re-zero it: because of possible contention btwn
 *       threads calling same DLL
 * NOTE: another reason: sometimes Marshaller uses plan's ptrArray, other times
 *       it copies it...
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
	private final int[] viInstArray; // native side should make strings?
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
	Marshaller(int sizeDirect, int sizeIndirect, int variableIndirectCount,
			int[] ptrArray, int[] posArray) {
		this.data = new byte[sizeDirect + sizeIndirect];
		this.ptrArray = ptrArray;
		this.posArray = posArray;
		if (0 < variableIndirectCount) {
			this.viArray = new Object[variableIndirectCount];
			this.viInstArray = new int[variableIndirectCount];
		} else {
			this.viArray = null;
			this.viInstArray = null;
		}
		this.isPtrArrayCopied = false;
		rewind();
	}

	/**
	 * Deliberately package-internal. Instances should only be constructed by
	 * calling {@link MarshallPlan#makeUnMarshaller(byte[])}.
	 *
	 * @since 20130806
	 */
	Marshaller(byte[] data, int[] ptrArray, int[] posArray) {
		this.data = data;
		this.ptrArray = ptrArray;
		this.posArray = posArray;
		this.viArray = null;
		this.viInstArray = null;
		this.isPtrArrayCopied = false;
		rewind();
	}

	/**
	 * Deliberately package-internal. Instances should only be constructed by
	 * calling {@link MarshallPlan#makeUnMarshaller(byte[], Object[], int[])}.
	 *
	 * @since 20130806
	 */
	Marshaller(byte[] data, int[] ptrArray, int[] posArray, Object[] viArray,
			int[] viInstArray) {
		this.data = data;
		this.ptrArray = ptrArray;
		this.posArray = posArray;
		this.viArray = viArray;
		this.viInstArray = viInstArray;
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
	 * The contents of this array <em>are <strong>not</strong> to be
	 * modified</em>!
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
	public int[] getViInstArray() {
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
		int dataIndex = nextData();
		if (value) {
			// 3 higher-order bytes can remain zero
			data[dataIndex] = (byte) 1;
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
	 * @see #putIntResource(short)
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
	 * @see #putStringPtr(String, VariableIndirectInstruction)
	 * @see #putStringPtr(Buffer, VariableIndirectInstruction)
	 */
	public void putPtr() {
		skipPtr();
	}

	/**
	 * Puts a NULL pointer value at the next position in the marshaller.
	 * @see #putPtr()
	 * @see #isPtrNull()
	 * @see #putNullStringPtr(VariableIndirectInstruction)
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
	 * <p>
	 * Skips over all of the positions occupied by a single variable indirect
	 * pointer.
	 * </p>
	 * @since 20130809
	 * @see #skipBasicArrayElements(int)
	 * @see #skipComplexElement(ElementSkipper)
	 * @see #putNullStringPtr(VariableIndirectInstruction)
	 * @see #putStringPtr(String, VariableIndirectInstruction)
	 * @see #putStringPtr(Buffer, VariableIndirectInstruction)
	 */
	public void skipStringPtr() {
		skipPtr();
		nextVi();
	}

	/**
	 * Puts a non-NULL pointer to variable indirect storage at the next position
	 * in the marshaller.
	 * @param value Non-{@code null} string value to marshall into variable
	 * indirect storage
	 * @param inst Instruction to place in the variable indirect
	 * instructions array
	 * @see #putStringPtr(Buffer, VariableIndirectInstruction)
	 * @see #putNullStringPtr(VariableIndirectInstruction)
	 * @see #putIntResource(short)
	 * @see #getStringPtr()
	 * @see #getStringPtrAlwaysByteArray(Buffer)
	 * @see #getStringPtrMaybeByteArray(Buffer)
	 * @see #putPtr()
	 */
	public void putStringPtr(String value, VariableIndirectInstruction inst) {
		assert value != null;
		skipPtr();
		int viIndex = nextVi();
		viArray[viIndex] = StringConversions.stringToZeroTerminatedByteArray(value);
		viInstArray[viIndex] = inst.ordinal();
	}

	/**
	 * Puts a non-NULL pointer to the internal data array of a {@link Buffer}
	 * at the next position in the marshaller.
	 * @param value Non-{@code null} buffer value whose internal data array is
	 * put into variable indirect storage
	 * @param inst Instruction to place in the variable indirect
	 * instructions array
	 * @see #putStringPtr(String, VariableIndirectInstruction)
	 * @see #putNullStringPtr(VariableIndirectInstruction)
	 * @see #putIntResource(short)
	 * @see #getStringPtrAlwaysByteArray(Buffer)
	 * @see #getStringPtrMaybeByteArray(Buffer)
	 * @see #putPtr()
	 */
	public void putStringPtr(Buffer value, VariableIndirectInstruction inst) {
		assert value != null;
		skipPtr();
		int viIndex = nextVi();
		viArray[viIndex] = value.getInternalData();
		viInstArray[viIndex] = inst.ordinal();
	}

	/**
	 * Puts a NULL pointer to variable indirect storage at the next position in
	 * the marshaller.
	 * @param inst Instruction to place in the variable indirect
	 * instructions array
	 * @see #putStringPtr(String, VariableIndirectInstruction)
	 * @see #putStringPtr(Buffer, VariableIndirectInstruction)
	 * @see #getStringPtr()
	 * @see #getStringPtrAlwaysByteArray(Buffer)
	 * @see #getStringPtrMaybeByteArray(Buffer)
	 * @see #putNullPtr()
	 */
	public void putNullStringPtr(VariableIndirectInstruction inst) {
		skipPtr();
		int viIndex = nextVi();
		viInstArray[viIndex] = inst.ordinal();
		// Assert: the skipped spot in the viArray is null
	}

	/**
	 * <p>
	 * Puts a Win32 {@code INTRESOURCE} value at the next position in the
	 * marshaller.
	 * </p>
	 * <p>
	 * Since an {@code INTRESOURCE} is a type of {@code resource}, this entails
	 * passing a {@code null} variable indirect reference, and putting
	 * {@link VariableIndirectInstruction#RETURN_RESOURCE} into the
	 * corresponding variable indirect instruction entry.
	 * </p>
	 * <p>
	 * <strong>NOTE</strong> that an {@code INTRESOURCE} is technically an
	 * <em>unsigned</em> 16-bit integer stored in the low-order 16 bits of a
	 * string pointer. The caller should take care to pass a signed integer that
	 * is the bitwise equivalent of the desired unsigned {@code INTRESOURCE}
	 * value.
	 * </p>
	 * @param value The 16-bit {@code INTRESOURCE} value as a signed 16-bit\
	 * {@code short}
	 * @since 20130801
	 * @see #getResource()
	 * @see #putStringPtr(String, VariableIndirectInstruction)
	 * @see #putStringPtr(Buffer, VariableIndirectInstruction)
	 * @see #putNullStringPtr(VariableIndirectInstruction)
	 * @see #putShort(short)
	 */
	public void putINTRESOURCE(short value) {
		putShort(value);
		ptrIndex += 2;
		int viIndex = nextVi();
		viInstArray[viIndex] = RETURN_RESOURCE.ordinal();
	}

	/**
	 * <p>
	 * Puts a variable indirect instruction at the next position in the
	 * instruction array <em>without advancing the cursors in the position array
	 * or the pointer array</em>.
	 * </p>
	 * <p>
	 * This method is used in situations in which instructions need to be sent
	 * to the native side even though no actual data needs to be marshalled
	 * over, for example when doing structure copy-outs. See
	 * {@link suneido.language.jsdi.type.Structure#call1(Object)}.
	 * </p>
	 * @param inst Instruction to place in the variable indirect
	 * instructions array
	 * @see #putNullStringPtr(VariableIndirectInstruction)
	 * @see #putStringPtr(Buffer, VariableIndirectInstruction)
	 * @see #putStringPtr(String, VariableIndirectInstruction)
	 * @see #putINTRESOURCE(short)
	 */
	public void putViInstructionOnly(VariableIndirectInstruction inst) {
		// TODO: test for this
		viInstArray[nextVi()] = inst.ordinal();
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

	// TODO: note in dics that like get...Maybe it is only used by InOutString
	public Object getStringPtr() {
		skipPtr();
		int viIndex = nextVi();
		Object value = viArray[viIndex];
		if (null == value) {
			return Boolean.FALSE;
		} else if (RETURN_JAVA_STRING.ordinal() == viInstArray[viIndex]) {
			assert value instanceof String;
			return value;
		} else {
			return getStringPtrAlwaysByteArrayNoAdvance(null, value, true);
		}
	}

	public Object getStringPtrAlwaysByteArray(Buffer oldValue) {
		skipPtr();
		int viIndex = nextVi();
		assert NO_ACTION.ordinal() == viInstArray[viIndex];
		return getStringPtrAlwaysByteArrayNoAdvance(oldValue, viArray[viIndex], false);
	}

	// TODO: note in docs this is only used by InOutString
	public Object getStringPtrMaybeByteArray(Buffer oldValue) {
		skipPtr();
		int viIndex = nextVi();
		Object value = viArray[viIndex];
		if (NO_ACTION.ordinal() == viInstArray[viIndex]) {
			return getStringPtrAlwaysByteArrayNoAdvance(oldValue, value, true);
		} else if (null == value) {
			return Boolean.FALSE;
		} else {
			assert value instanceof String;
			return value;
		}
	}

	/**
	 * Extracts the Win32 resource value at the next position in the marshaller. 
	 * @return A non-{@code null} Integer or String reference representing,
	 * respectively, an INTRESOURCE value or a string {@code resource}
	 * @since 20130801
	 * @see #putINTRESOURCE(short)
	 * @see #getStringPtr()
	 * @see #getStringPtrAlwaysByteArray(Buffer)
	 * @see #getStringPtrMaybeByteArray(Buffer)
	 */
	public Object getResource() {
		skipPtr();
		int viIndex = nextVi();
		assert RETURN_RESOURCE.ordinal() == viInstArray[viIndex];
		Object value = viArray[viIndex];
		if (! (value instanceof Integer || value instanceof String)) {
			throw new InternalError(
				"getResource() expects a non-null Integer or String in the " +
				"variable indirect storage, but got a " +
				(null == value ? "null" : value.getClass().getCanonicalName())
			);
		}
		return value;
	}

	public String getZeroTerminatedStringDirect(int numChars) {
		if (numChars < 1)
		{
			throw new JSDIException(
					"zero-terminated string must have at least one character");
		}
		else
		{
			int dataIndex = nextData();
			byte b = data[dataIndex];
			if (0 == b) return "";
			StringBuilder sb = new StringBuilder(numChars);
			sb.append((char)b);
			for (int k = 1; k < numChars; ++k) {
				b = data[++dataIndex];
				if (0 == b) return sb.toString();
				sb.append((char)b);
			}
			throw new JSDIException("missing zero terminator");
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

	private static Object getStringPtrAlwaysByteArrayNoAdvance(Buffer oldValue,
			Object value, boolean truncate) {
		if (null == value) {
			return Boolean.FALSE;
		} else if (null == oldValue) {
			// This really should never happen, unless a concurrently executing
			// thread deleted the buffer from the container between the time
			// that it was marshalled in and now.
			byte[] b = (byte[])value;
			return new Buffer(b, 0, b.length);
		} else {
			// The truncation code is necessary to mimic the behaviour of
			// CSuneido when it marshalls out a 'string' dll type into an
			// instance of Buffer: the output is truncated as if it were a
			// zero-terminated string.
			if (truncate) {
				Buffer oldBuffer = (Buffer)oldValue;
				oldBuffer.truncate();
			}
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