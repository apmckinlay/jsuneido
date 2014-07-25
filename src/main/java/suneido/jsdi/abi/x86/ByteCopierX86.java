package suneido.jsdi.abi.x86;

import suneido.SuInternalError;

/**
 * Lightweight class for copying between x86 marshalled representation
 * (<code><strong>int</strong>[]</code>) and 8-bit string-type data
 * (<code><strong>byte</strong>[]</code>, {@link Buffer}, etc.).
 *
 * @author Victor Schappert
 * @since 20140722
 */
final class ByteCopierX86 {

	//
	// DATA
	//

	private final int[]  id;
	private final byte[] bd;
	private       int    idByteIndex;   // conceptually, index into id as if byte[]
	private       int    idWordIndex;   // index into id
	private       int    bdByteIndex;   // index into bd

	//
	// CONSTRUCTORS
	//

	public ByteCopierX86(int[] id, int idByteIndex, byte[] bd) {
		this.id = id;
		this.bd = bd;
		this.idByteIndex = idByteIndex;
		this.idWordIndex = idByteIndex / Integer.BYTES;
		this.bdByteIndex = 0;
	}

	//
	// MUTATORS
	//

	public void copyToIntArr(int length) {
		assert 0 < length;
		final int byteIndex0to3 = idByteIndex % 4; // 0..3
		final int length0to3 = length < 4 ? length : 0; // 0..3
		int code = byteIndex0to3 << 4 | length0to3;
		// Handle up to first 3 bytes
		handle_first_four: while(true) {
			switch (code) {
			case 0x01: // byte 0, length 1
				copyToIntArr01();
				return;
			case 0x02: // byte 0, length 2
				copyToIntArr02();
				return;
			case 0x03: // byte 0, length 3
				copyToIntArr03();
				return;
			case 0x00: // byte 0, length 4+
				break handle_first_four;

			case 0x11: // byte 1, length 1
				copyToIntArr11();
				return;
			case 0x12: // byte 1, length 2
				copyToIntArr12();
				return;
			case 0x13: // byte 1, length 3
				copyToIntArr13();
				return;
			case 0x10: // byte 1, length 4+
				copyToIntArr13();
				break handle_first_four;

			case 0x21: // byte 2, length 1
				copyToIntArr21();
				return;
			case 0x22: // byte 2, length 2
				copyToIntArr22();
				return;
			case 0x23: // byte 2, length 3
				copyToIntArr22();
				code = 001;
				break;
			case 0x20: // distance 2, length 4+
				copyToIntArr22();
				break handle_first_four;
	
			case 0x31: // byte 3, length 1
				copyToIntArr31();
				return;
			case 0x32: // byte 3, length 2
			case 0x33: // byte 3, length 3
				copyToIntArr31();
				code -= 031;
				break;
			case 0x30: // byte 3, length 4+
				copyToIntArr31();
				break handle_first_four;

			default:
				throw SuInternalError.unreachable();
			} // switch
		} // handle_first_four
		// Handle groups of four bytes copied into whole words at a time.
		final int MAX_BD_BYTE_INDEX = bdByteIndex + length;
		while (bdByteIndex <= MAX_BD_BYTE_INDEX - 4) {
			copyToIntArr04();
			bdByteIndex += 4;
			++idWordIndex;
		}
		// Handle trailing 0-3 bytes
		switch (MAX_BD_BYTE_INDEX - bdByteIndex) {
		case 0:
			break;
		case 1:
			copyToIntArr01();
			break;
		case 2:
			copyToIntArr02();
			break;
		case 3:
			copyToIntArr03();
			break;
		default:
			throw SuInternalError.unreachable();
		}
	}

	public void copyFromIntArr(int length) {
		assert 0 < length;
		final int byteIndex0to3 = idByteIndex % 4; // 0..3
		final int length0to3 = length < 4 ? length : 0; // 0..3
		int code = byteIndex0to3 << 4 | length0to3;
		// Handle up to first 3 bytes
		handle_first_four: while(true) {
			switch (code) {
			case 0x01: // byte 0, length 1
				copyFromIntArr01();
				return;
			case 0x02: // byte 0, length 2
				copyFromIntArr02();
				return;
			case 0x03: // byte 0, length 3
				copyFromIntArr03();
				return;
			case 0x00: // byte 0, length 4+
				break handle_first_four;
	
			case 0x11: // byte 1, length 1
				copyFromIntArr11();
				return;
			case 0x12: // byte 1, length 2
				copyFromIntArr12();
				return;
			case 0x13: // byte 1, length 3
				copyFromIntArr13();
				return;
			case 0x10: // byte 1, length 4+
				copyFromIntArr13();
				break handle_first_four;

			case 0x21: // byte 2, length 1
				copyFromIntArr21();
				return;
			case 0x22: // byte 2, length 2
				copyFromIntArr22();
				return;
			case 0x23: // byte 2, length 3
				copyFromIntArr22();
				code = 001;
				break;
			case 0x20: // distance 2, length 4+
				copyFromIntArr22();
				break handle_first_four;
	
			case 0x31: // byte 3, length 1
				copyFromIntArr31();
				return;
			case 0x32: // byte 3, length 2
			case 0x33: // byte 3, length 3
				copyFromIntArr31();
				code -= 031;
				break;
			case 0x30: // byte 3, length 4+
				copyFromIntArr31();
				break handle_first_four;

			default:
				throw SuInternalError.unreachable();
			} // switch
		} // handle_first_four
			// Handle groups of four bytes copied into whole words at a time.
		final int MAX_BD_BYTE_INDEX = bdByteIndex + length;
		while (bdByteIndex <= MAX_BD_BYTE_INDEX - 4) {
			copyFromIntArr04();
			bdByteIndex += 4;
			++idWordIndex;
		}
		// Handle trailing 0-3 bytes
		switch (MAX_BD_BYTE_INDEX - bdByteIndex) {
		case 0:
			break;
		case 1:
			copyFromIntArr01();
			break;
		case 2:
			copyFromIntArr02();
			break;
		case 3:
			copyFromIntArr03();
			break;
		default:
			throw SuInternalError.unreachable();
		}
	}

	public int copyNonZeroFromIntArr(int length) {
		assert 0 < length;
		final int byteIndex0to3 = idByteIndex % 4; // 0..3
		final int length0to3 = length < 4 ? length : 0; // 0..3
		int code = byteIndex0to3 << 4 | length0to3;
		int zeroIndex = -1;
		// Handle up to first 3 bytes
		handle_first_four: while(true) {
			switch (code) {
			case 0x01: // byte 0, length 1
				copyFromIntArr01();
				return whereZero1();
			case 0x02: // byte 0, length 2
				copyFromIntArr02();
				return whereZero2();
			case 0x03: // byte 0, length 3
				copyFromIntArr03();
				return whereZero3();
			case 0x00: // byte 0, length 4+
				break handle_first_four;
	
			case 0x11: // byte 1, length 1
				copyFromIntArr11();
				return whereZero1();
			case 0x12: // byte 1, length 2
				copyFromIntArr12();
				return whereZero2();
			case 0x13: // byte 1, length 3
				copyFromIntArr13();
				return whereZero3();
			case 0x10: // byte 1, length 4+
				copyFromIntArr13();
				break handle_first_four;

			case 0x21: // byte 2, length 1
				copyFromIntArr21();
				return whereZero1();
			case 0x22: // byte 2, length 2
				copyFromIntArr22();
				return whereZero2();
			case 0x23: // byte 2, length 3
				copyFromIntArr22();
				code = 001;
				break;
			case 0x20: // distance 2, length 4+
				copyFromIntArr22();
				break handle_first_four;
	
			case 0x31: // byte 3, length 1
				copyFromIntArr31();
				return whereZero1();
			case 0x32: // byte 3, length 2
			case 0x33: // byte 3, length 3
				copyFromIntArr31();
				code -= 031;
				zeroIndex = whereZero3();
				if (zeroIndex < 3)
					return zeroIndex;
				break;
			case 0x30: // byte 3, length 4+
				copyFromIntArr31();
				zeroIndex = whereZero3();
				if (zeroIndex < 3)
					return zeroIndex;
				break handle_first_four;

			default:
				throw SuInternalError.unreachable();
			} // switch
		} // handle_first_four
			// Handle groups of four bytes copied into whole words at a time.
		final int MAX_BD_BYTE_INDEX = bdByteIndex + length;
		while (bdByteIndex <= MAX_BD_BYTE_INDEX - 4) {
			copyFromIntArr04();
			zeroIndex = whereZero4();
			bdByteIndex += 4;
			if (zeroIndex < bdByteIndex)
				return zeroIndex;
			++idWordIndex;
		}
		// Handle trailing 0-3 bytes
		switch (MAX_BD_BYTE_INDEX - bdByteIndex) {
		case 0:
			return length;
		case 1:
			copyFromIntArr01();
			return whereZero1();
		case 2:
			copyFromIntArr02();
			return whereZero2();
		case 3:
			return whereZero3();
		default:
			throw SuInternalError.unreachable();
		}
	}

	//
	// INTERNALS
	//

	private void copyToIntArr01() {
		id[idWordIndex] |= bd[bdByteIndex] & 0xff;
	}

	private void copyToIntArr02() {
		id[idWordIndex] |=
				(bd[bdByteIndex+0] & 0xff) << 000 |
				(bd[bdByteIndex+1] & 0xff) << 010;
	}

	private void copyToIntArr03() {
		id[idWordIndex] |=
				(bd[bdByteIndex+0] & 0xff) << 000 |
				(bd[bdByteIndex+1] & 0xff) << 010 |
				(bd[bdByteIndex+2] & 0xff) << 020;
	}

	private void copyToIntArr04() {
		id[idWordIndex] =
				(bd[bdByteIndex+0] & 0xff) << 000 |
				(bd[bdByteIndex+1] & 0xff) << 010 |
				(bd[bdByteIndex+2] & 0xff) << 020 |
				(int) bd[bdByteIndex+3]    << 030;
	}

	private void copyToIntArr11() {
		id[idWordIndex] |= (bd[bdByteIndex] & 0xff) << 010;
		++bdByteIndex;
	}

	private void copyToIntArr12() {
		id[idWordIndex] |=
				(bd[bdByteIndex+0] & 0xff) << 010 |
				(bd[bdByteIndex+1] & 0xff) << 020;
		bdByteIndex += 2;
	}

	private void copyToIntArr13() {
		id[idWordIndex] |=
				(bd[bdByteIndex+0] & 0xff) << 010 |
				(bd[bdByteIndex+1] & 0xff) << 020 |
				bd[bdByteIndex+2] << 030;
		bdByteIndex += 3;
	}

	private void copyToIntArr21() {
		id[idWordIndex] |= (bd[bdByteIndex] & 0xff) << 020;
		++bdByteIndex;
	}

	private void copyToIntArr22() {
		id[idWordIndex] |=
				(bd[bdByteIndex+0] & 0xff) << 020 |
				(bd[bdByteIndex+1] & 0xff) << 030;
		bdByteIndex += 2;
	}

	private void copyToIntArr31() {
		id[idWordIndex] |= bd[bdByteIndex] << 030;
		++bdByteIndex;
	}

	private void copyFromIntArr01() {
		bd[bdByteIndex] = (byte) (id[idWordIndex] & 0xff);
	}

	private void copyFromIntArr02() {
		final int word = id[idWordIndex];
		bd[bdByteIndex+0] = (byte) (word >>> 000 & 0xff);
		bd[bdByteIndex+1] = (byte) (word >>> 010 & 0xff);
	}
	

	private void copyFromIntArr03() {
		final int word = id[idWordIndex];
		bd[bdByteIndex+0] = (byte) (word >>> 000 & 0xff);
		bd[bdByteIndex+1] = (byte) (word >>> 010 & 0xff);
		bd[bdByteIndex+2] = (byte) (word >>> 020 & 0xff);
	}

	private void copyFromIntArr04() {
		final int word = id[idWordIndex];
		bd[bdByteIndex+0] = (byte) (word >>> 000 & 0xff);
		bd[bdByteIndex+1] = (byte) (word >>> 010 & 0xff);
		bd[bdByteIndex+2] = (byte) (word >>> 020 & 0xff);
		bd[bdByteIndex+3] = (byte) (word >>> 030 & 0xff);
	}

	private void copyFromIntArr11() {
		bd[bdByteIndex] = (byte) (id[idWordIndex] >>> 010 & 0xff);
		++bdByteIndex;
	}

	private void copyFromIntArr12() {
		final int word = id[idWordIndex];
		bd[bdByteIndex+0] = (byte) (word >>> 010 & 0xff);
		bd[bdByteIndex+1] = (byte) (word >>> 020 & 0xff);
		bdByteIndex += 2;
	}

	private void copyFromIntArr13() {
		final int word = id[idWordIndex];
		bd[bdByteIndex+0] = (byte) (word >>> 010 & 0xff);
		bd[bdByteIndex+1] = (byte) (word >>> 020 & 0xff);
		bd[bdByteIndex+2] = (byte) (word >>> 030 & 0xff);
		bdByteIndex += 3;
	}

	private void copyFromIntArr21() {
		bd[bdByteIndex] = (byte) (id[idWordIndex] >>> 020 & 0xff);
		++bdByteIndex;
	}

	private void copyFromIntArr22() {
		final int word = id[idWordIndex];
		bd[bdByteIndex+0] = (byte) (word >>> 020 & 0xff);
		bd[bdByteIndex+1] = (byte) (word >>> 030 & 0xff);
		bdByteIndex += 2;
	}

	private void copyFromIntArr31() {
		bd[bdByteIndex] = (byte) (id[idWordIndex] >>> 030 & 0xff);
		++bdByteIndex;
	}

	private int whereZero1() {
		return 0 == bd[bdByteIndex] ? bdByteIndex : bdByteIndex + 1;
	}

	private int whereZero2() {
		if (0 == bd[bdByteIndex]) {
			return bdByteIndex;
		} else if (0 == bd[bdByteIndex+1]) {
			return bdByteIndex + 1;
		} else {
			return bdByteIndex + 2;
		}
	}

	private int whereZero3() {
		if (0 == bd[bdByteIndex]) {
			return bdByteIndex;
		} else if (0 == bd[bdByteIndex+1]) {
			return bdByteIndex + 1;
		} else if (0 == bd[bdByteIndex+2]) {
			return bdByteIndex + 2;
		} else {
			return bdByteIndex + 3;
		}
	}

	private int whereZero4() {
		if (0 == bd[bdByteIndex]) {
			return bdByteIndex;
		} else if (0 == bd[bdByteIndex+1]) {
			return bdByteIndex + 1;
		} else if (0 == bd[bdByteIndex+2]) {
			return bdByteIndex + 2;
		} else if (0 == bd[bdByteIndex+3]) {
			return bdByteIndex + 3;
		} else {
			return bdByteIndex + 4;
		}
	}
}
