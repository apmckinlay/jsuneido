/* Copyright 2013 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido.language.jsdi;

import static suneido.language.jsdi.VariableIndirectInstruction.NO_ACTION;
import static suneido.language.jsdi.VariableIndirectInstruction.RETURN_JAVA_STRING;
import static suneido.language.jsdi.VariableIndirectInstruction.RETURN_RESOURCE;

import java.util.Arrays;

import javax.annotation.concurrent.NotThreadSafe;

import suneido.SuInternalError;

/**
 * <p>
 * A container that can marshall Suneido values represented as Java objects into
 * a binary representation suitable to pass to the native side.
 * </p>
 * <p>
 * This class is deliberately "dumb" in the sense that it does not know about,
 * or care about, the vast majority of memory-layout issues: for the most part,
 * it simply follows the "script" given to it by the {@link MarshallPlan} that
 * created it. For example, if some code calls {@link #putInt16(int)}, the
 * marshaller will simply copy the given 16-bit integer at the next position
 * dictated by the marshall plan. This class is therefore generally reusable
 * across operating systems and CPU architectures since it simply "does what
 * it is told" by the marshall plan. 
 * </p>
 * <p>
 * This class <strong>must not be cached or reused</strong> (<em>ie</em> a new
 * instance should be requested for each marshalling roundtrip) for the
 * following reasons:
 * <ul>
 * <li>
 * it makes certain assumptions, such as that its internal data arrays are
 * zeroed-out (which only occurs on construction); and
 * </li>
 * <li>
 * it is not thread-safe, so contention between several threads calling the
 * same {@code dll} could cause serious problems.
 * </li>
 * </ul>
 * </p>
 *
 * @author Victor Schappert
 * @since 20130710
 * @see MarshallPlan
 */
@DllInterface
@NotThreadSafe
public abstract class Marshaller {

	//
	// DATA
	//

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

	protected Marshaller(int variableIndirectCount, int[] ptrArray,
			int[] posArray) {
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

	protected Marshaller(int[] ptrArray, int[] posArray) {
		this.ptrArray = ptrArray;
		this.posArray = posArray;
		this.viArray = null;
		this.viInstArray = null;
		this.isPtrArrayCopied = false;
		rewind();
	}

	protected Marshaller(int[] ptrArray, int[] posArray, Object[] viArray,
			int[] viInstArray) {
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
	public final int[] getPtrArray() {
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
	public final Object[] getViArray() {
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
	public final int[] getViInstArray() {
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
	public final void rewind() {
		this.posIndex = 0;
		this.ptrIndex = 1;
		this.viIndex  = 0;
	}

	/**
	 * Puts a JSDI {@code bool} value at the next position in the marshaller.
	 * @param value Boolean value
	 * @see #getBool()
	 */
	public abstract void putBool(boolean value);

	/**
	 * Puts a JSDI {@code int8} value at the next position in the marshaller.
	 * @param value Single-byte integer value
	 * @see #getInt8()
	 */
	public abstract void putInt8(byte value);

	/**
	 * Puts a JSDI {@code int16} value at the next position in the marshaller.
	 * @param value 16-bit JSDI {@code int16} value
	 * @see #getInt16()
	 * @see #putIntResource(short)
	 */
	public abstract void putInt16(short value);

	/**
	 * Puts a JSDI {@code int32} value at the next position in the marshaller.
	 * @param value 32-bit JSDI {@code int32} value
	 * @see #getInt32()
	 */
	public abstract void putInt32(int value);

	/**
	 * Puts a JSDI {@code int64} value at the next position in the marshaller.
	 * @param value 64-bit JSDI {@code int64} value
	 * @see #getInt64()
	 */
	public abstract void putInt64(long value);
	/**
	 * Puts an integer having the same size as a pointer at the next position
	 * in the marshaller.
	 * @param value Integer value
	 * @since 20140717
	 * @see #getPointerSizedInt()
	 */
	public abstract void putPointerSizedInt(long value);

	/**
	 * Puts a JSDI {@code float} value at the next position in the marshaller.
	 * @param value 32-bit JSDI {@code float} value
	 * @see #getFloat()
	 */
	public final void putFloat(float value) {
		putInt32(Float.floatToRawIntBits(value));
	}

	/**
	 * Puts a JSDI {@code double} value at the next position in the marshaller.
	 * @param value 64-bit JSDI {@code double} value
	 * @see #getDouble()
	 */
	public final void putDouble(double value) {
		putInt64(Double.doubleToRawLongBits(value));
	}

	/**
	 * Puts a non-NULL pointer value at the next position in the marshaller.
	 * @see #putNullPtr()
	 * @see #isPtrNull()
	 * @see #putStringPtr(String, VariableIndirectInstruction)
	 * @see #putStringPtr(Buffer, VariableIndirectInstruction)
	 */
	public final void putPtr() {
		skipPtr();
	}

	/**
	 * Puts a NULL pointer value at the next position in the marshaller.
	 * @see #putPtr()
	 * @see #isPtrNull()
	 * @see #putNullStringPtr(VariableIndirectInstruction)
	 */
	public final void putNullPtr() {
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
	public final void skipBasicArrayElements(int numElems) {
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
	public final void skipComplexElement(ElementSkipper skipper) {
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
	public final void skipStringPtr() {
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
	public final void putStringPtr(String value, VariableIndirectInstruction inst) {
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
	public final void putStringPtr(Buffer value, VariableIndirectInstruction inst) {
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
	public final void putNullStringPtr(VariableIndirectInstruction inst) {
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
	 * @param value The 16-bit {@code INTRESOURCE} value as a signed 16-bit
	 * {@code short}
	 * @since 20130801
	 * @see #getResource()
	 * @see #putStringPtr(String, VariableIndirectInstruction)
	 * @see #putStringPtr(Buffer, VariableIndirectInstruction)
	 * @see #putNullStringPtr(VariableIndirectInstruction)
	 * @see #putInt16(short)
	 */
	public final void putINTRESOURCE(short value) {
		putInt16(value);
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
	public final void putViInstructionOnly(VariableIndirectInstruction inst) {
		// TODO: test for this
		viInstArray[nextVi()] = inst.ordinal();
	}

	/**
	 * <p>
	 * Puts a Java {@link String} into the next position in the marshaller,
	 * converting it to a zero-terminated string of 8-bit characters.
	 * </p>
	 * <p>
	 * At most <code>maxChars - 1</code> characters from <code>value</code> are
	 * marshalled because a zero-terminator must be appended to the end of the
	 * string. If <code>value</code> is shorter than <code>maxChars - 1</code>,
	 * the unfilled positions are filled with zeroes.
	 * </p>
	 * @param value Non-NULL string to put
	 * @param maxChars Maximum number of characters to put <em>including the
	 * zero-terminator</em>
	 * @see #putZeroTerminatedStringDirect(Buffer, int)
	 * @see #putNonZeroTerminatedStringDirect(String, int)
	 * @see #getZeroTerminatedStringDirect(int)
	 */
	public abstract void putZeroTerminatedStringDirect(String value, int maxChars);

	/**
	 * <p>
	 * Puts a {@link Buffer} into the next position in the marshaller as a
	 * zero-terminated string of 8-bit characters.
	 * </p>
	 * <p>
	 * At most <code>maxChars - 1</code> characters from <code>value</code> are
	 * marshalled because a zero-terminator must be appended to the end of the
	 * string. If <code>value</code> is shorter than <code>maxChars - 1</code>,
	 * the unfilled positions are filled with zeroes.
	 * </p>
	 * @param value Non-NULL {@link Buffer} to put
	 * @param maxChars Maximum number of characters to put <em>including the
	 * zero-terminator</em>
	 * @see #putZeroTerminatedStringDirect(String, int)
	 * @see #putNonZeroTerminatedStringDirect(Buffer, int)
	 * @see #getZeroTerminatedStringDirect(int)
	 */
	public abstract void putZeroTerminatedStringDirect(Buffer value, int maxChars);

	/**
	 * <p>
	 * Puts a Java {@link String} into the next position in the marshaller,
	 * converting it to a string of 8-bit characters.
	 * </p>
	 * <p>
	 * At most <code>maxChars</code> characters from <code>value</code> are
	 * marshalled. If <code>value</code> is shorter than <code>maxChars</code>,
	 * the unfilled positions are filled with zeroes.
	 * </p>
	 * @param value Non-NULL string to put
	 * @param maxChars Maximum number of characters to put
	 * @see #putNonZeroTerminatedStringDirect(Buffer, int)
	 * @see #putZeroTerminatedStringDirect(String, int)
	 * @see #getNonZeroTerminatedStringDirect(int, Buffer)
	 */
	public abstract void putNonZeroTerminatedStringDirect(String value, int maxChars);

	/**
	 * <p>
	 * Puts a {@link Buffer} into the next position in the marshaller as a
	 * string of 8-bit characters.
	 * </p>
	 * <p>
	 * At most <code>maxChars</code> characters from <code>value</code> are
	 * marshalled. If <code>value</code> is shorter than <code>maxChars</code>,
	 * the unfilled positions are filled with zeroes.
	 * </p>
	 * @param value Non-NULL {@link Buffer} to put
	 * @param maxChars Maximum number of characters to put
	 * @see #putNonZeroTerminatedStringDirect(String, int)
	 * @see #putZeroTerminatedStringDirect(Buffer, int)
	 * @see #getNonZeroTerminatedStringDirect(int, Buffer)
	 */
	public abstract void putNonZeroTerminatedStringDirect(Buffer value,
			int maxChars);

	/**
	 * Extracts a JSDI {@code bool} value from the next position in the
	 * marshaller.
	 * @return Boolean value
	 * @see #putBool(boolean)
	 */
	public final boolean getBool() {
		return getInt32() != 0;
	}

	/**
	 * Extracts a JSDI {@code int8} value from the next position in the
	 * marshaller.
	 * @return Single-byte integer value
	 * @see #putInt8(byte)
	 */
	public abstract int getInt8();

	/**
	 * Extracts a JSDI {@code int16} value from the next position in the
	 * marshaller.
	 * @return 16-bit JSDI {@code int16} value
	 * @see #putInt16(short)
	 */
	public abstract int getInt16();

	/**
	 * Extracts a JSDI {@code int32} value from the next position in the
	 * marshaller.
	 * @return 32-bit JSDI {@code int32} value
	 * @see #putInt32(int)
	 */
	public abstract int getInt32();

	/**
	 * Extracts a JSDI {@code int64} value from the next position in the
	 * marshaller.
	 * @return 64-bit JSDI {@code int64} value
	 * @see #putInt64(long)
	 */
	public abstract long getInt64();

	/**
	 * <p>
	 * Extracts an integer value that has the same size as the platform's
	 * native pointer size.
	 * </p>
	 * <p>
	 * Even though the number of bytes read out of the marshaller might not be
	 * {@code sizeof(Java long)}, the return value of this method is a signed
	 * Java {@code long} value that is equivalent to a <em>signed</em> integer
	 * whose width in bytes is {@link PrimitiveSize#POINTER}. For example, if
	 * the platform native pointer size is 4, the returned value will be in the
	 * domain of a signed 32-bit integer; if 8, it will be in the domain of a
	 * signed 64-bit integer.
	 * </p>
	 * <p>
	 * Note that because the result is a signed integer, a 32-bit pointer whose
	 * high-order bit is 1 will span the entire 64-bit Java {@code long}.
	 * (<em>eg</em> {@code 0xffffffff => 0xffffffffffffffffL}).
	 * </p>
	 * @return Signed integer
	 * @see #putPointerSizedInt(long)
	 */
	public abstract long getPointerSizedInt();

	/**
	 * Extracts a JSDI {@code float} value from the next position in the
	 * marshaller.
	 * @return 32-bit JSDI {@code float} value
	 * @see #putFloat(float)
	 */
	public final float getFloat() {
		return Float.intBitsToFloat(getInt32());
	}

	/**
	 * Extracts a JSDI {@code double} value from the next position in the
	 * marshaller.
	 * @return 64-bit JSDI {@code double} value
	 * @see #putDouble(double)
	 */
	public final double getDouble() {
		return Double.longBitsToDouble(getInt64());
	}

	/**
	 * <p>
	 * Extracts a pointer-sized value from the next position in the marshaller
	 * and returns {@code true} iff the value represents a NULL pointer.
	 * </p>
	 * <p>
	 * Like all the {@code get*()} methods, this method advances the marshaller
	 * position.
	 * </p>
	 * @return Whether the pointer is NULL
	 * @since 20130717
	 * @see #putPtr()
	 * @see #putNullPtr()
	 */
	public abstract boolean isPtrNull();

	/**
	 * <p>
	 * Extracts a JSDI {@code string} value from the next position in the
	 * marshaller.
	 * </p>
	 * <p>
	 * Depending on how the {@code string} was marshalled <em>in</em>, the
	 * actual type of the return value may be one of:
	 * <ul>
	 * <li>
	 * {@link Boolean}, <em>but only the value {@link Boolean#FALSE}</em>:
	 * indicates a NULL string pointer;
	 * </li>
	 * <li>
	 * {@link String}, if the pointer is valid and the variable indirect
	 * instruction is {@link VariableIndirectInstruction#RETURN_JAVA_STRING}; 
	 * </li>
	 * <li>
	 * {@link Buffer}, if the pointer is valid and the variable indirect
	 * instruction is {@link VariableIndirectInstruction#NO_ACTION}. 
	 * </li>
	 * </ul>
	 * </p>
	 * @return Unmarshalled {@code string} value
	 * @see #putStringPtr(String, VariableIndirectInstruction)
	 * @see #putStringPtr(Buffer, VariableIndirectInstruction)
	 * @see #getStringPtrAlwaysByteArray(Buffer)
	 */
	public final Object getStringPtr() {
		// NOTE: As of 20140718, this method is used only for marshalling
		//       InOutString.
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

	/**
	 * <p>
	 * Extracts a JSDI {@code string} value from the next position in the
	 * marshaller where the type of the actual value marshalled <em>in</em> was
	 * a {@link Buffer}.
	 * </p>
	 * <p>
	 * Depending on how the {@code string} was marshalled <em>in</em>, the
	 * actual type of the return value may be one of:
	 * <ul>
	 * <li>
	 * {@link Boolean}, <em>but only the value {@link Boolean#FALSE}</em>:
	 * indicates a NULL string pointer;
	 * </li>
	 * <li>
	 * {@link Buffer}, if the pointer is valid and the variable indirect
	 * instruction is {@link VariableIndirectInstruction#NO_ACTION}. 
	 * </li>
	 * <li>
	 * {@link String}, if the pointer is valid and the variable indirect
	 * instruction is {@link VariableIndirectInstruction#RETURN_JAVA_STRING}; 
	 * </li>
	 * </ul>
	 * </p>
	 * @param oldValue The buffer that was passed to
	 *        {@link #putStringPtr(Buffer, VariableIndirectInstruction)}&mdash;
	 *        if non-NULL it will receive the marshalled out value
	 * @return Unmarshalled {@code string} value&mdash;will be {@code oldValue},
	 *         modified to contain the marshalled out value if {@code oldValue}
	 *         is non-NULL, or a new instance of {@link Buffer} otherwise
	 * @see #putStringPtr(String, VariableIndirectInstruction)
	 * @see #putStringPtr(Buffer, VariableIndirectInstruction)
	 * @see #getStringPtr()
	 * @see #getStringPtrAlwaysByteArray(Buffer)
	 */
	public final Object getStringPtrMaybeByteArray(Buffer oldValue) {
		// NOTE: As of 20140718, this method is used only for marshalling
		//       InOutString.
		// NOTE: The reason the variable indirect value "may be" a byte array
		//       (rather than definitely is) is subtly concurrency-related. The
		//       InOutString is calling this method based on the fact that the
		//       "oldValue" passed to the InOutString is a Buffer. However, if
		//       the InOutString is directly or indirectly a member of a
		//       Structure, then some part of the SuContainer that was
		//       marshalled *im* may have been modified. Suppose the container
		//       contained a String value during marshall in, but another thread
		//       modified it to be a Buffer on marshall out. So we *expect* a
		//       byte array with almost 100% likelihood, but nevertheless in
		//       strange cases the variable indirect array *might not contain*
		//       one.
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
	 * <p>
	 * Extracts a JSDI {@code buffer} value from the next position in the
	 * marshaller.
	 * </p>
	 * <p>
	 * The actual type of the return value may be one of:
	 * <ul>
	 * <li>
	 * {@link Boolean}, <em>but only the value {@link Boolean#FALSE}</em>:
	 * indicates a NULL string pointer;
	 * </li>
	 * <li>
	 * {@link Buffer}, if the pointer is valid. 
	 * </li>
	 * </ul>
	 * </p>
	 * @param oldValue The buffer that was passed to
	 *        {@link #putStringPtr(Buffer, VariableIndirectInstruction)}&mdash;
	 *        if non-NULL it will receive the marshalled out value
	 * @return Unmarshalled {@code string} value&mdash;will be {@code oldValue},
	 *         modified to contain the marshalled out value if {@code oldValue}
	 *         is non-NULL, or a new instance of {@link Buffer} otherwise
	 * @see #putStringPtr(Buffer, VariableIndirectInstruction)
	 * @see #getStringPtr()
	 * @see #getStringPtrMaybeByteArray(Buffer)
	 */
	public final Object getStringPtrAlwaysByteArray(Buffer oldValue) {
		// NOTE: As of 20140718, this method is used only for marshalling
		//       BufferType.
		skipPtr();
		int viIndex = nextVi();
		assert NO_ACTION.ordinal() == viInstArray[viIndex];
		return getStringPtrAlwaysByteArrayNoAdvance(oldValue, viArray[viIndex], false);
	}

	/**
	 * Extracts the Win32 resource value at the next position in the marshaller.
	 * @return A non-{@code null} Integer or String reference representing,
	 * respectively, an {@code INTRESOURCE} value or a string {@code resource}
	 * @since 20130801
	 * @see #putINTRESOURCE(short)
	 * @see #getStringPtr()
	 * @see #getStringPtrAlwaysByteArray(Buffer)
	 * @see #getStringPtrMaybeByteArray(Buffer)
	 */
	public final Object getResource() {
		skipPtr();
		int viIndex = nextVi();
		assert RETURN_RESOURCE.ordinal() == viInstArray[viIndex];
		Object value = viArray[viIndex];
		if (! (value instanceof Integer || value instanceof String)) {
			throw new SuInternalError(
				"getResource() expects a non-null Integer or String in the " +
				"variable indirect storage, but got a " +
				(null == value ? "null" : value.getClass().getCanonicalName())
			);
		}
		return value;
	}

	public final String getZeroTerminatedStringDirect(int numChars) {
		if (numChars < 1)
		{
			throw new JSDIException(
					"zero-terminated string must have at least one character");
		}
		else
		{
			return getZeroTerminatedStringDirectSafe(numChars);
		}
	}

	public abstract Buffer getNonZeroTerminatedStringDirect(int numChars,
			Buffer oldValue);

	//
	// INTERNALS
	//

	protected int nextData() {
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
			// cSuneido when it marshalls out a 'string' dll type into an
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

	protected abstract String getZeroTerminatedStringDirectSafe(int numChars);
}