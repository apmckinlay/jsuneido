/* Copyright 2014 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido.jsdi.abi.x86;

import suneido.jsdi.Buffer;
import suneido.jsdi.DllInterface;
import suneido.jsdi.JSDIException;
import suneido.jsdi.Marshaller;

/**
 * Marshaller customized for x86 marshalling.
 * 
 * @author Victor Schappert
 * @since 20140717
 */
@DllInterface
final class MarshallerX86 extends Marshaller {

	//
	// DATA
	//
	
	private final int[] data;

	//
	// CONSTRUCTORS
	//

	MarshallerX86(int sizeTotal, int variableIndirectCount,
			int[] ptrArray, int[] posArray) {
		super(variableIndirectCount, ptrArray, posArray);
		this.data = new int[sizeTotal / Integer.BYTES];
	} // Deliberately package-internal

	MarshallerX86(int[] data, int[] ptrArray, int[] posArray) {
		super(ptrArray, posArray);
		this.data = data;
	} // Deliberately package-internal

	MarshallerX86(int[] data, int[] ptrArray, int[] posArray,
			Object[] viArray, int[] viInstArray) {
		super(ptrArray, posArray, viArray, viInstArray);
		this.data = data;
	} // Deliberately package-internal


	//
	// INTERNALS
	//

	private void copyToIntArr(byte[] src, int length) {
		new ByteCopierX86(data, nextData(), src).copyToIntArr(length);
	}

	private void copyFromIntArr(byte[] dest, int length) {
		if (0 < length) {
			new ByteCopierX86(data, nextData(), dest).copyFromIntArr(length);	
		}
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
	public int[] getData() {
		return data;
	}

	//
	// ANCESTOR CLASS: Marshaller
	//

	@Override
	public void putBool(boolean value) {
		final int dataIndex = nextData();
		if (value) {
			data[dataIndex >> 2] = 1;
		}
	}

	@Override
	public void putInt8(byte value) {
		final int dataIndex = nextData();
		final int wordIndex = dataIndex >> 002;
		final int byteIndex = dataIndex & 003;
		// The AND with 0xff is necessary to override sign extension because
		// otherwise Java will sign-extend any values in the range [-128..-1],
		// i.e. [0x80..0xff], when promoting them to 'int'. For example,
		// 0xff => 0xffffffff.
// TODO: debug-step through here to make sure I'm right
		final int orMask = ((int)value & 0xff) << 8 * byteIndex;
		data[wordIndex] |= orMask;
	}

	@Override
	public void putInt16(short value) {
		final int dataIndex = nextData();
		final int wordIndex = dataIndex >> 002;
		final int byteIndex = dataIndex & 003; // must be 0th or 2d byte
		assert 0 == byteIndex % 2;
		// AND with 0xffff overrides Java sign extension
// TODO: debug-step through here to make sure I'm right
		final int orMask = ((int)value & 0xffff) << 8 * byteIndex;
		data[wordIndex] |= orMask;
	}

	@Override
	public void putInt32(int value) {
		final int dataIndex = nextData();
		data[dataIndex >> 002] = value;
	}

	@Override
	public void putInt64(long value) {
		final int dataIndex = nextData();
		final int wordIndex = dataIndex >> 002;
		data[wordIndex+0] = (int) (value & 0x00000000ffffffffL);
		data[wordIndex+1] = (int) (value >>> 32);
	}

	@Override
	public void putPointerSizedInt(long value) {
		if (!((long) Integer.MIN_VALUE <= value && value <= (long) Integer.MAX_VALUE))
			throw new JSDIException(
					"can't marshall 32-bit pointer because more than 32 bits used: "
							+ value);
		putInt32((int) value);
	}

	@Override
	public void putZeroTerminatedStringDirect(String value, int maxChars) {
		final int N = Math.min(maxChars - 1, value.length());
		if (0 < N) {
			copyToIntArr(Buffer.copyStr(value, new byte[N], 0, N), N);
		}
	}

	@Override
	public void putZeroTerminatedStringDirect(Buffer value, int maxChars) {
		final int N = Math.min(maxChars - 1, value.length()); 
		if (0 < N) {
			copyToIntArr(value.getInternalData(), N); 
		}
	}

	@Override
	public void putNonZeroTerminatedStringDirect(String value, int maxChars) {
		final int N = Math.min(maxChars, value.length());
		if (0 < N) {
			copyToIntArr(Buffer.copyStr(value, new byte[N], 0, N), N);
		}
	}

	@Override
	public void putNonZeroTerminatedStringDirect(Buffer value, int maxChars) {
		final int N = Math.min(maxChars, value.length());
		if (0 < N) {
			copyToIntArr(value.getInternalData(), N);
		}
	}

	@Override
	public int getInt8() {
		final int dataIndex = nextData();
		final int wordIndex = dataIndex >> 002;
		final int byteIndex = dataIndex & 003;
		// Cast once to get a byte, twice to sign-extend it to int.
		return (int)(byte)((data[wordIndex] >> 8 * byteIndex) & 0xff);
	}

	@Override
	public int getInt16() {
		final int dataIndex = nextData();
		final int wordIndex = dataIndex >> 002;
		final int byteIndex = dataIndex & 003;
		assert 0 == byteIndex % 2;
		// Cast once to get a short, twice to sign-extend it to int.
		return (int)(short)((data[wordIndex] >>> 8 * byteIndex) & 0xffff);
	}

	@Override
	public int getInt32() {
		return data[nextData() >> 002];
	}

	@Override
	public long getInt64() {
		final int dataIndex = nextData();
		final int wordIndex = dataIndex >> 002;
		return data[wordIndex+0] & 0x00000000ffffffffL |
			(long) data[wordIndex+1] << 040;
	}

	@Override
	public long getPointerSizedInt() {
		return (long)getInt32();
	}

	@Override
	public boolean isPtrNull() {
		return 0 == getInt32();
	}

	@Override
	public Buffer getNonZeroTerminatedStringDirect(int numChars, Buffer oldValue) {
		// This could have weird side-effects if Suneido programmer has two
		// containers both with references to the same Buffer and tries to
		// unmarshall some data into one of them. In some cases, after the
		// unmarshalling both containers will refer to the same buffer. In
		// others, the container that was unmarshalled-into will refer to a new
		// buffer. I'm not sure if this can be considered "wrong" or not.		
		if (oldValue != null && numChars <= oldValue.capacity()) {
			copyFromIntArr(oldValue.getInternalData(), numChars);
			oldValue.setSize(numChars);
			return oldValue;
		} else {
			final Buffer newValue = new Buffer(numChars);
			copyFromIntArr(newValue.getInternalData(), numChars);
			return newValue;
		}
	}

	@Override
	protected String getZeroTerminatedStringDirectChecked(int numChars) {
		final Buffer buffer = new Buffer(numChars);
		final int zeroIndex = new ByteCopierX86(data, nextData(),
				buffer.getInternalData()).copyNonZeroFromIntArr(numChars);
		if (0 == zeroIndex) {
			return "";
		} else if (zeroIndex < numChars) {
			buffer.setSize(zeroIndex);
			return buffer.toString();
		} else {
			return null; // Caller will throw an exception
		}
	}
}
