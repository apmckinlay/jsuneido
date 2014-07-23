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
	
	private final byte[] data;  // TODO: This will be int

	//
	// CONSTRUCTORS
	//

	MarshallerX86(int sizeDirect, int sizeIndirect, int variableIndirectCount,
			int[] ptrArray, int[] posArray) {
		super(variableIndirectCount, ptrArray, posArray);
		this.data = new byte[sizeDirect + sizeIndirect];
	} // Deliberately package-internal

	MarshallerX86(byte[] data, int[] ptrArray, int[] posArray) {
		super(ptrArray, posArray);
		this.data = data;
	} // Deliberately package-internal

	MarshallerX86(byte[] data, int[] ptrArray, int[] posArray,
			Object[] viArray, int[] viInstArray) {
		super(ptrArray, posArray, viArray, viInstArray);
		this.data = data;
	} // Deliberately package-internal

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

	//
	// ANCESTOR CLASS: Marshaller
	//

	@Override
	public void putBool(boolean value) {
		int dataIndex = nextData();
		if (value) {
			// 3 higher-order bytes can remain zero
			data[dataIndex] = (byte) 1;
		}
	}

	@Override
	public void putInt8(byte value) {
		data[nextData()] = (byte)value;
	}

	@Override
	public void putInt16(short value) {
		int dataIndex = nextData();
		data[dataIndex + 0] = (byte) (value >>> 000);
		data[dataIndex + 1] = (byte) (value >>> 010);
	}

	@Override
	public void putInt32(int value) {
		int dataIndex = nextData();
		data[dataIndex + 0] = (byte) (value >>> 000);
		data[dataIndex + 1] = (byte) (value >>> 010);
		data[dataIndex + 2] = (byte) (value >>> 020);
		data[dataIndex + 3] = (byte) (value >>> 030);
	}

	@Override
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

	@Override
	public void putPointerSizedInt(long value) {
		if (!((long) Integer.MIN_VALUE <= value && value <= (long) Integer.MAX_VALUE))
			throw new JSDIException(
					"Can't marshall 32-bit pointer because more than 32 bits used: "
							+ value);
		putInt32((int) value);
	}

	@Override
	public void putZeroTerminatedStringDirect(String value, int maxChars) {
		int dataIndex = nextData();
		Buffer.copyStr(value, data, dataIndex,
				Math.min(maxChars - 1, value.length()));
	}

	@Override
	public void putZeroTerminatedStringDirect(Buffer value, int maxChars) {
		int dataIndex = nextData();
		value.copyInternalData(data, dataIndex, maxChars - 1);
	}

	@Override
	public void putNonZeroTerminatedStringDirect(String value, int maxChars) {
		int dataIndex = nextData();
		Buffer.copyStr(value, data, dataIndex,
				Math.min(maxChars, value.length()));
	}

	@Override
	public void putNonZeroTerminatedStringDirect(Buffer value, int maxChars) {
		int dataIndex = nextData();
		value.copyInternalData(data, dataIndex, maxChars);
	}

	@Override
	public int getInt8() {
		return data[nextData()];
	}

	@Override
	public int getInt16() {
		final int dataIndex = nextData();
		// Note: the bitwise AND with 0xff is to avoid EVIL Java sign extension
		//       (because Java promotes bitwise operands to int and then sign-
		//       extends the 0xff byte).
		return (data[dataIndex + 0] & 0xff) << 000 |
				data[dataIndex + 1] << 010;
	}

	@Override
	public int getInt32() {
		final int dataIndex = nextData();
		return
			(data[dataIndex + 0] & 0xff) << 000 |
			(data[dataIndex + 1] & 0xff) << 010 |
			(data[dataIndex + 2] & 0xff) << 020 |
			data[dataIndex + 3] << 030;
	}

	@Override
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

	@Override
	public long getPointerSizedInt() {
		return (long)getInt32();
	}

	@Override
	public boolean isPtrNull() {
		return 0 == getInt32();
	}

	@Override
	protected String getZeroTerminatedStringDirectSafe(int numChars) {
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

	@Override
	public Buffer getNonZeroTerminatedStringDirect(int numChars, Buffer oldValue) {
		int dataIndex = nextData();
		if (oldValue != null && numChars <= oldValue.capacity()) {
			oldValue.setAndSetSize(data, dataIndex, dataIndex + numChars);
			return oldValue;
		} else {
			return new Buffer(data, dataIndex, dataIndex + numChars);
		}
	}
}
