/* Copyright 2014 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido.jsdi.abi.x86;

import suneido.SuInternalError;
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
		this.data = new int[sizeTotal];
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

	private void copyToIntArr01(int wordIndex, byte src) {
		data[wordIndex] |= src & 0xff;
	}

	private void copyToIntArr02(int wordIndex, byte[] src, int srcIndex) {
		data[wordIndex] |=
				(src[srcIndex+0] & 0xff) << 000 |
				(src[srcIndex+1] & 0xff) << 010;
	}

	private void copyToIntArr03(int wordIndex, byte[] src, int srcIndex) {
		data[wordIndex] |=
				(src[srcIndex+0] & 0xff) << 000 |
				(src[srcIndex+1] & 0xff) << 010 |
				(src[srcIndex+2] & 0xff) << 030;
	}

	private void copyToIntArr04(int wordIndex, byte[] src, int srcIndex) {
		data[wordIndex] =
				(src[srcIndex+0] & 0xff) << 000 |
				(src[srcIndex+1] & 0xff) << 010 |
				(src[srcIndex+2] & 0xff) << 020 |
				(int) src[srcIndex+3]    << 030;
	}

	private int copyToIntArr11(int wordIndex, byte src) {
		data[wordIndex] |= (src & 0xff) << 010;
		return 1;
	}

	private int copyToIntArr12(int wordIndex, byte[] src, int srcIndex) {
		data[wordIndex] |=
				(src[srcIndex+0] & 0xff) << 010 |
				(src[srcIndex+1] & 0xff) << 020;
		return 2;
	}

	private int copyToIntArr13(int wordIndex, byte[] src, int srcIndex) {
		data[wordIndex] |=
				(src[srcIndex+0] & 0xff) << 010 |
				(src[srcIndex+1] & 0xff) << 020 |
				src[srcIndex+3] << 030;
		return 3;
	}

	private int copyToIntArr21(int wordIndex, byte src) {
		data[wordIndex] |= (src & 0xff) << 020;
		return 1;
	}

	private int copyToIntArr22(int wordIndex, byte[] src, int srcIndex) {
		data[wordIndex] |=
				(src[srcIndex+0] & 0xff) << 020 |
				(src[srcIndex+1] & 0xff) << 030;
		return 2;
	}

	private int copyToIntArr31(int wordIndex, byte src) {
		data[wordIndex] |= src << 030;
		return 1;
	}

	private void copyToIntArr(byte[] src, int length) {
		assert 0 < length;
		final int dataIndex = nextData();
		int wordIndex = dataIndex >> 002;
		int byteIndex = dataIndex & 003;
		final int lengthMod4 = length % 4; // 0..3
		int code = byteIndex << 010 | lengthMod4;
		int srcIndex = 0;
		// Handle up to first 3 bytes
		handle_first_four: while(true) {
			switch (code) {
			case 001: // byte 0, length 1
				copyToIntArr01(wordIndex, src[srcIndex]);
				return;
			case 002: // byte 0, length 2
				copyToIntArr02(wordIndex, src, srcIndex);
				return;
			case 003: // byte 0, length 3
				copyToIntArr03(wordIndex, src, 0);
				return;
			case 000: // byte 0, length 4+
				break handle_first_four;
	
			case 011: // byte 1, length 1
				copyToIntArr11(wordIndex, src[srcIndex]);
				return;
			case 012: // byte 1, length 2
				copyToIntArr12(wordIndex, src, srcIndex);
				return;
			case 013: // byte 1, length 3
				copyToIntArr13(wordIndex, src, srcIndex);
				return;
			case 010: // byte 1, length 4+
				srcIndex += copyToIntArr13(wordIndex, src, srcIndex);
				break handle_first_four;

			case 021: // byte 2, length 1
				copyToIntArr21(wordIndex, src[srcIndex]);
				return;
			case 022: // byte 2, length 2
				copyToIntArr22(wordIndex, src, srcIndex);
				return;
			case 023: // byte 2, length 3
				srcIndex += copyToIntArr22(wordIndex, src, srcIndex);
				code = 001;
				break;
			case 020: // distance 2, length 4+
				srcIndex += copyToIntArr22(wordIndex, src, srcIndex);
				break handle_first_four;
	
			case 031: // byte 3, length 1
				copyToIntArr31(wordIndex, src[srcIndex]);
				return;
			case 032: // byte 3, length 2
			case 033: // byte 3, length 3
				srcIndex += copyToIntArr31(wordIndex, src[srcIndex]);
				code -= 031;
				break;
			case 030: // byte 3, length 4+
				srcIndex += copyToIntArr31(wordIndex, src[srcIndex]);
				break handle_first_four;

			default:
				throw SuInternalError.unreachable();
			} // switch
		} // handle_first_four
			// Handle groups of four bytes copied into whole words at a time.
		for (; srcIndex + 4 < length; srcIndex += 4, wordIndex++) {
			copyToIntArr04(wordIndex, src, srcIndex);
		}
		// Handle trailing 0-3 bytes
		switch (length - srcIndex) {
		case 0:
			break;
		case 1:
			copyToIntArr01(wordIndex, src[srcIndex]);
			break;
		case 2:
			copyToIntArr02(wordIndex, src, srcIndex);
			break;
		case 3:
			copyToIntArr03(wordIndex, src, srcIndex);
			break;
		default:
			throw SuInternalError.unreachable();
		}
	}

	private void copyFromIntArr01(int wordIndex, byte[] dest, int destIndex) {
		dest[destIndex] = (byte) (data[wordIndex] & 0xff);
	}

	private void copyFromIntArr02(int wordIndex, byte[] dest, int destIndex) {
		final int word = data[wordIndex];
		dest[destIndex+0] = (byte) (word >>> 000 & 0xff);
		dest[destIndex+1] = (byte) (word >>> 010 & 0xff);
	}
	

	private void copyFromIntArr03(int wordIndex, byte[] dest, int destIndex) {
		final int word = data[wordIndex];
		dest[destIndex+0] = (byte) (word >>> 000 & 0xff);
		dest[destIndex+1] = (byte) (word >>> 010 & 0xff);
		dest[destIndex+2] = (byte) (word >>> 020 & 0xff);
	}

	private void copyFromIntArr04(int wordIndex, byte[] dest, int destIndex) {
		final int word = data[wordIndex];
		dest[destIndex+0] = (byte) (word >>> 000 & 0xff);
		dest[destIndex+1] = (byte) (word >>> 010 & 0xff);
		dest[destIndex+2] = (byte) (word >>> 020 & 0xff);
		dest[destIndex+3] = (byte) (word >>> 030 & 0xff);
	}

	private int copyFromIntArr11(int wordIndex, byte[] dest, int destIndex) {
		dest[destIndex] = (byte) (data[wordIndex] >>> 010 & 0xff);
		return 1;
	}

	private int copyFromIntArr12(int wordIndex, byte[] dest, int destIndex) {
		final int word = data[wordIndex];
		dest[destIndex+0] = (byte) (word >>> 010 & 0xff);
		dest[destIndex+1] = (byte) (word >>> 020 & 0xff);
		return 2;
	}

	private int copyFromIntArr13(int wordIndex, byte[] dest, int destIndex) {
		final int word = data[wordIndex];
		dest[destIndex+0] = (byte) (word >>> 010 & 0xff);
		dest[destIndex+1] = (byte) (word >>> 020 & 0xff);
		dest[destIndex+2] = (byte) (word >>> 030 & 0xff);
		return 3;
	}

	private int copyFromIntArr21(int wordIndex, byte[] dest, int destIndex) {
		dest[destIndex] = (byte) (data[wordIndex] >>> 020 & 0xff);
		return 1;
	}

	private int copyFromIntArr22(int wordIndex, byte[] dest, int destIndex) {
		final int word = data[wordIndex];
		dest[destIndex+0] = (byte) (word >>> 020 & 0xff);
		dest[destIndex+1] = (byte) (word >>> 030 & 0xff);
		return 2;
	}

	private int copyFromIntArr31(int wordIndex, byte[] dest, int destIndex) {
		dest[destIndex] = (byte) (data[wordIndex] >>> 030 & 0xff);
		return 1;
	}

	private void copyFromIntArr(byte[] dest, int length) {
		final int dataIndex = nextData();
		if (length < 1) return;
		int wordIndex = dataIndex >> 002;
		int byteIndex = dataIndex & 003;   // 0..3
		final int lengthMod4 = length % 4; // 0..3
		int code = byteIndex << 010 | lengthMod4;
		int destIndex = 0;
		// Handle up to first 3 bytes
		// Handle up to first 3 bytes
		handle_first_four: while(true) {
			switch (code) {
			case 001: // byte 0, length 1
				copyFromIntArr01(wordIndex, dest, destIndex);
				return;
			case 002: // byte 0, length 2
				copyFromIntArr02(wordIndex, dest, destIndex);
				return;
			case 003: // byte 0, length 3
				copyFromIntArr03(wordIndex, dest, 0);
				return;
			case 000: // byte 0, length 4+
				break handle_first_four;
	
			case 011: // byte 1, length 1
				copyFromIntArr11(wordIndex, dest, destIndex);
				return;
			case 012: // byte 1, length 2
				copyFromIntArr12(wordIndex, dest, destIndex);
				return;
			case 013: // byte 1, length 3
				copyFromIntArr13(wordIndex, dest, destIndex);
				return;
			case 010: // byte 1, length 4+
				destIndex += copyFromIntArr13(wordIndex, dest, destIndex);
				break handle_first_four;

			case 021: // byte 2, length 1
				copyFromIntArr21(wordIndex, dest, destIndex);
				return;
			case 022: // byte 2, length 2
				copyFromIntArr22(wordIndex, dest, destIndex);
				return;
			case 023: // byte 2, length 3
				destIndex += copyFromIntArr22(wordIndex, dest, destIndex);
				code = 001;
				break;
			case 020: // distance 2, length 4+
				destIndex += copyFromIntArr22(wordIndex, dest, destIndex);
				break handle_first_four;
	
			case 031: // byte 3, length 1
				copyFromIntArr31(wordIndex, dest, destIndex);
				return;
			case 032: // byte 3, length 2
			case 033: // byte 3, length 3
				destIndex += copyFromIntArr31(wordIndex, dest, destIndex);
				code -= 031;
				break;
			case 030: // byte 3, length 4+
				destIndex += copyFromIntArr31(wordIndex, dest, destIndex);
				break handle_first_four;

			default:
				throw SuInternalError.unreachable();
			} // switch
		} // handle_first_four
			// Handle groups of four bytes copied into whole words at a time.
		for (; destIndex + 4 < length; destIndex += 4, wordIndex++) {
			copyFromIntArr04(wordIndex, dest, destIndex);
		}
		// Handle trailing 0-3 bytes
		switch (length - destIndex) {
		case 0:
			break;
		case 1:
			copyFromIntArr01(wordIndex, dest, destIndex);
			break;
		case 2:
			copyFromIntArr02(wordIndex, dest, destIndex);
			break;
		case 3:
			copyFromIntArr03(wordIndex, dest, destIndex);
			break;
		default:
			throw SuInternalError.unreachable();
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
		return (data[wordIndex] >>> 8 * byteIndex) & 0xff;
	}

	@Override
	public int getInt16() {
		final int dataIndex = nextData();
		final int wordIndex = dataIndex >> 002;
		final int byteIndex = dataIndex & 003;
		assert 0 == byteIndex % 2;
		return (data[wordIndex] >>> 8 * byteIndex) & 0xffff;
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
}
