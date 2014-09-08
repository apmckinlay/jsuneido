package suneido.jsdi.marshall;

import suneido.SuInternalError;
import suneido.jsdi.DllInterface;

/**
 * Lightweight class for copying between marshalled representation
 * (Java <code><b>long</b>[]</code>) and 8-bit string-type data
 * (Java <code><b>byte</b>[]</code>, {@link Buffer}, etc.).
 *
 * @author Victor Schappert
 * @since 20140722
 */
@DllInterface
public final class ByteCopier {

	//
	// DATA
	//

	private final long[] ld;
	private final byte[] bd;
	private       int    ldByteIndex;   // conceptually, index into ld as if byte[]
	private       int    ldWordIndex;   // index into ld
	private       int    bdByteIndex;   // index into bd

	//
	// CONSTRUCTORS
	//

	public ByteCopier(long[] ld, int ldByteIndex, byte[] bd) {
		this.ld = ld;
		this.bd = bd;
		this.ldByteIndex = ldByteIndex;
		this.ldWordIndex = ldByteIndex / Long.BYTES;
		this.bdByteIndex = 0;
	}

	//
	// MUTATORS
	//

	public void copyToLongArr(int length) {
		final int byteIndex0to7 = ldByteIndex % 8; // 0..7
		final int length0to8 = length < 8 ? length : 8; // 0..8
		int code = byteIndex0to7 << 4 | length0to8;
		// Handle up to first 7 bytes
		// In 99.9% of cases, the data structure where the byte array exists
		// will have a reasonable aligned layout such that it begins on either
		// byte 0 or byte 4 of a long (i.e. it is 8-byte or 4-byte aligned).
		switch (code) {
		case 0x00: // byte 0, length 0
		case 0x10: // byte 1, length 0
		case 0x20: // byte 2, length 0
		case 0x30: // byte 3, length 0
		case 0x40: // byte 4, length 0
		case 0x50: // byte 5, length 0
		case 0x60: // byte 6, length 0
		case 0x70: // byte 7, length 0
			return;

		case 0x01: // byte 0, length 1
			copyToLongArr01();
			return;
		case 0x02: // byte 0, length 2
			copyToLongArr02();
			return;
		case 0x03: // byte 0, length 3
			copyToLongArr03();
			return;
		case 0x04: // byte 0, length 4
			copyToLongArr04();
			return;
		case 0x05: // byte 0, length 5
			copyToLongArr05();
			return;
		case 0x06: // byte 0, length 6
			copyToLongArr06();
			return;
		case 0x07: // byte 0, length 7
			copyToLongArr07();
			return;
		case 0x08: // byte 0, length 8+
			break;

		case 0x41: // byte 4, length 1
			copyToLongArr41();
			return;
		case 0x42: // byte 4, length 2
			copyToLongArr42();
			return;
		case 0x43: // byte 4, length 3
			copyToLongArr43();
			return;
		case 0x44: // byte 4, length 4 -- fall through
		case 0x45: // byte 4, length 5 -- fall through
		case 0x46: // byte 4, length 6 -- fall through
		case 0x47: // byte 4, length 7 -- fall through
		case 0x48: // byte 4, length 8+
			copyToLongArr44();
			bdByteIndex += 4;
			length -= 4;
			break;

		default:
			final int byteCountMN = calcByteCountMN(byteIndex0to7, length0to8);
			copyToLongArrMN(byteIndex0to7, byteCountMN);
			length -= byteCountMN;
			break;
		} // switch
		// Handle groups of eight bytes copied into whole words at a time.
		final int MAX_BD_BYTE_INDEX = bdByteIndex + length;
		while (bdByteIndex <= MAX_BD_BYTE_INDEX - 8) {
			copyToLongArr08();
			bdByteIndex += 8;
			++ldWordIndex;
		}
		// Handle trailing 0-7 bytes
		switch (MAX_BD_BYTE_INDEX - bdByteIndex) {
		case 0:
			break;
		case 1:
			copyToLongArr01();
			break;
		case 2:
			copyToLongArr02();
			break;
		case 3:
			copyToLongArr03();
			break;
		case 4:
			copyToLongArr04();
			break;
		case 5:
			copyToLongArr05();
			break;
		case 6:
			copyToLongArr06();
			break;
		case 7:
			copyToLongArr07();
			break;
		default:
			throw SuInternalError.unreachable();
		}
	}

	public void copyFromLongArr(int length) {
		final int byteIndex0to7 = ldByteIndex % 8; // 0..7
		final int length0to8 = length < 8 ? length : 8; // 0..8
		int code = byteIndex0to7 << 4 | length0to8;
		// Handle up to first 7 bytes
		// In 99.9% of cases, the data structure where the byte array exists
		// will have a reasonable aligned layout such that it begins on either
		// byte 0 or byte 4 of a long (i.e. it is 8-byte or 4-byte aligned).
		switch (code) {
		case 0x00: // byte 0, length 0
		case 0x10: // byte 1, length 0
		case 0x20: // byte 2, length 0
		case 0x30: // byte 3, length 0
		case 0x40: // byte 4, length 0
		case 0x50: // byte 5, length 0
		case 0x60: // byte 6, length 0
		case 0x70: // byte 7, length 0
			return;

		case 0x01: // byte 0, length 1
			copyFromLongArr01();
			return;
		case 0x02: // byte 0, length 2
			copyFromLongArr02();
			return;
		case 0x03: // byte 0, length 3
			copyFromLongArr03();
			return;
		case 0x04: // byte 0, length 4
			copyFromLongArr04();
			return;
		case 0x05: // byte 0, length 5
			copyFromLongArr05();
			return;
		case 0x06: // byte 0, length 6
			copyFromLongArr06();
			return;
		case 0x07: // byte 0, length 7
			copyFromLongArr07();
			return;
		case 0x08: // byte 0, length 8+
			break;

		case 0x41: // byte 4, length 1
			copyFromLongArr41();
			return;
		case 0x42: // byte 4, length 2
			copyFromLongArr42();
			return;
		case 0x43: // byte 4, length 3
			copyFromLongArr43();
			return;
		case 0x44: // byte 4, length 4 -- fall through
		case 0x45: // byte 4, length 5 -- fall through
		case 0x46: // byte 4, length 6 -- fall through
		case 0x47: // byte 4, length 7 -- fall through
		case 0x48: // byte 4, length 8+
			copyFromLongArr44();
			bdByteIndex += 4;
			length -= 4;
			break;

		default:
			final int byteCountMN = calcByteCountMN(byteIndex0to7, length0to8);
			copyFromLongArrMN(byteIndex0to7, byteCountMN);
			length -= byteCountMN;
			break;
		} // switch
		// Handle groups of eight bytes copied from whole words at a time.
		final int MAX_BD_BYTE_INDEX = bdByteIndex + length;
		while (bdByteIndex <= MAX_BD_BYTE_INDEX - 8) {
			copyFromLongArr08();
			bdByteIndex += 8;
			++ldWordIndex;
		}
		// Handle trailing 0-7 bytes
		switch (MAX_BD_BYTE_INDEX - bdByteIndex) {
		case 0:
			break;
		case 1:
			copyFromLongArr01();
			break;
		case 2:
			copyFromLongArr02();
			break;
		case 3:
			copyFromLongArr03();
			break;
		case 4:
			copyFromLongArr04();
			break;
		case 5:
			copyFromLongArr05();
			break;
		case 6:
			copyFromLongArr06();
			break;
		case 7:
			copyFromLongArr07();
			break;
		default:
			throw SuInternalError.unreachable();
		}
	}

	public int copyNonZeroFromLongArr(int length) {
		final int byteIndex0to7 = ldByteIndex % 8; // 0..7
		final int length0to8 = length < 8 ? length : 8; // 0..8
		int code = byteIndex0to7 << 4 | length0to8;
		int zeroIndex;
		int amtCopied = 0;
		// Handle up to first 7 bytes
		// In 99.9% of cases, the data structure where the byte array exists
		// will have a reasonable aligned layout such that it begins on either
		// byte 0 or byte 4 of a long (i.e. it is 8-byte or 4-byte aligned).
		switch (code) {
			case 0x00: // byte 0, length 0
			case 0x10: // byte 1, length 0
			case 0x20: // byte 2, length 0
			case 0x30: // byte 3, length 0
			case 0x40: // byte 4, length 0
			case 0x50: // byte 5, length 0
			case 0x60: // byte 6, length 0
			case 0x70: // byte 7, length 0
				return 0;

			case 0x01: // byte 0, length 1
				copyFromLongArr01();
				return whereZero1();
			case 0x02: // byte 0, length 2
				copyFromLongArr02();
				return whereZero2();
			case 0x03: // byte 0, length 3
				copyFromLongArr03();
				return whereZero3();
			case 0x04: // byte 0, length 4
				copyFromLongArr04();
				return whereZero4();
			case 0x05: // byte 0, length 5
				copyFromLongArr05();
				return whereZero5();
			case 0x06: // byte 0, length 6
				copyFromLongArr06();
				return whereZero6();
			case 0x07: // byte 0, length 7
				copyFromLongArr07();
				return whereZero7();
			case 0x08: // byte 0, length 8+
				break;

			case 0x41: // byte 4, length 1
				copyFromLongArr41();
				return whereZero1();
			case 0x42: // byte 4, length 2
				copyFromLongArr42();
				return whereZero2();
			case 0x43: // byte 4, length 3
				copyFromLongArr43();
				return whereZero3();
			case 0x44: // byte 4, length 4 -- fall through
			case 0x45: // byte 4, length 5 -- fall through
			case 0x46: // byte 4, length 6 -- fall through
			case 0x47: // byte 4, length 7 -- fall through
			case 0x48: // byte 4, length 8+
				copyFromLongArr44();
				zeroIndex = whereZero4();
				if (4 != zeroIndex) {
					return zeroIndex;
				}
				bdByteIndex += 4;
				amtCopied = 4;
				break;
			default:
				amtCopied = calcByteCountMN(byteIndex0to7, length0to8);
				zeroIndex = copyFromLongArrMNCheckZero(byteIndex0to7, amtCopied);
				if (zeroIndex < amtCopied) {
					return zeroIndex;
				}
				break;
			} // switch
		// Handle groups of eight bytes copied from whole words at a time.
		final int MAX_BD_BYTE_INDEX = bdByteIndex + length - amtCopied;
		while (bdByteIndex <= MAX_BD_BYTE_INDEX - 8) {
			copyFromLongArr08();
			zeroIndex = whereZero8();
			bdByteIndex += 8;
			if (zeroIndex < bdByteIndex)
				return zeroIndex;
			++ldWordIndex;
		}
		// Handle trailing 0-7 bytes
		switch (MAX_BD_BYTE_INDEX - bdByteIndex) {
		case 0:
			return length;
		case 1:
			copyFromLongArr01();
			return whereZero1();
		case 2:
			copyFromLongArr02();
			return whereZero2();
		case 3:
			copyFromLongArr03();
			return whereZero3();
		case 4:
			copyFromLongArr04();
			return whereZero4();
		case 5:
			copyFromLongArr05();
			return whereZero5();
		case 6:
			copyFromLongArr06();
			return whereZero6();
		case 7:
			copyFromLongArr07();
			return whereZero7();
		default:
			throw SuInternalError.unreachable();
		}
	}

	//
	// INTERNALS
	//

	private void copyToLongArr01() {
		ld[ldWordIndex] |= bd[bdByteIndex] & 0xffL;
	}

	private void copyToLongArr02() {
		ld[ldWordIndex] |=
				(bd[bdByteIndex+0] & 0xffL) << 000 |
				(bd[bdByteIndex+1] & 0xffL) << 010;
	}

	private void copyToLongArr03() {
		ld[ldWordIndex] |=
				(bd[bdByteIndex+0] & 0xffL) << 000 |
				(bd[bdByteIndex+1] & 0xffL) << 010 |
				(bd[bdByteIndex+2] & 0xffL) << 020;
	}

	private void copyToLongArr04() {
		ld[ldWordIndex] =
				(bd[bdByteIndex+0] & 0xffL) << 000 |
				(bd[bdByteIndex+1] & 0xffL) << 010 |
				(bd[bdByteIndex+2] & 0xffL) << 020 |
				(bd[bdByteIndex+3] & 0xffL) << 030;
	}

	private void copyToLongArr05() {
		ld[ldWordIndex] =
				(bd[bdByteIndex+0] & 0xffL) << 000 |
				(bd[bdByteIndex+1] & 0xffL) << 010 |
				(bd[bdByteIndex+2] & 0xffL) << 020 |
				(bd[bdByteIndex+3] & 0xffL) << 030 |
				(bd[bdByteIndex+4] & 0xffL) << 040;
	}

	private void copyToLongArr06() {
		ld[ldWordIndex] =
				(bd[bdByteIndex+0] & 0xffL) << 000 |
				(bd[bdByteIndex+1] & 0xffL) << 010 |
				(bd[bdByteIndex+2] & 0xffL) << 020 |
				(bd[bdByteIndex+3] & 0xffL) << 030 |
				(bd[bdByteIndex+4] & 0xffL) << 040 |
				(bd[bdByteIndex+5] & 0xffL) << 050;
	}

	private void copyToLongArr07() {
		ld[ldWordIndex] =
				(bd[bdByteIndex+0] & 0xffL) << 000 |
				(bd[bdByteIndex+1] & 0xffL) << 010 |
				(bd[bdByteIndex+2] & 0xffL) << 020 |
				(bd[bdByteIndex+3] & 0xffL) << 030 |
				(bd[bdByteIndex+4] & 0xffL) << 040 |
				(bd[bdByteIndex+5] & 0xffL) << 050 |
				(bd[bdByteIndex+6] & 0xffL) << 060;
	}
	
	private void copyToLongArr08() {
		ld[ldWordIndex] =
				(bd[bdByteIndex+0] & 0xffL) << 000 |
				(bd[bdByteIndex+1] & 0xffL) << 010 |
				(bd[bdByteIndex+2] & 0xffL) << 020 |
				(bd[bdByteIndex+3] & 0xffL) << 030 |
				(bd[bdByteIndex+4] & 0xffL) << 040 |
				(bd[bdByteIndex+5] & 0xffL) << 050 |
				(bd[bdByteIndex+6] & 0xffL) << 060 |
				((long) bd[bdByteIndex+7])  << 070;
	}
	
	private void copyToLongArr41() {
		ld[ldWordIndex] |= (bd[bdByteIndex+0] & 0xffL) << 040;
	}

	private void copyToLongArr42() {
		ld[ldWordIndex] |=
				(bd[bdByteIndex+0] & 0xffL) << 040 |
				(bd[bdByteIndex+1] & 0xffL) << 050;
	}

	private void copyToLongArr43() {
		ld[ldWordIndex] |=
				(bd[bdByteIndex+0] & 0xffL) << 040 |
				(bd[bdByteIndex+1] & 0xffL) << 050 |
				(bd[bdByteIndex+2] & 0xffL) << 060;
	}

	private void copyToLongArr44() {
		ld[ldWordIndex] |=
				(bd[bdByteIndex+0] & 0xffL) << 040 |
				(bd[bdByteIndex+1] & 0xffL) << 050 |
				(bd[bdByteIndex+2] & 0xffL) << 060 |
				((long)bd[bdByteIndex+3])   << 070;
		++ldWordIndex;
	}

	private void copyToLongArrMN(int byteIndex, int length) {
		assert 1 <= byteIndex && byteIndex < 8 && 4 != byteIndex;
		assert 0 < length && length < 8;
		int shift = byteIndex * 010;
		final int maxShift = shift + (length - 1) * 010;
		long word = (bd[bdByteIndex++] & 0xffL) << shift;
		while (shift < maxShift) {
			shift += 010;
			word |= (bd[bdByteIndex++] & 0xffL) << shift;
		}
		ld[ldWordIndex++] |= word;
	}

	private static int calcByteCountMN(int byteIndex, int length) {
		assert 1 <= byteIndex && byteIndex < 8 && 4 != byteIndex;
		assert 0 < length && length <= 8;
		final int maxIndex = Math.min(byteIndex + length - 1, 7);
		return maxIndex - byteIndex + 1;
	}

	private void copyFromLongArr01() {
		bd[bdByteIndex] = (byte) (ld[ldWordIndex] & 0xffL);
	}

	private void copyFromLongArr02() {
		final long word = ld[ldWordIndex];
		bd[bdByteIndex+0] = (byte) (word >>> 000 & 0xffL);
		bd[bdByteIndex+1] = (byte) (word >>> 010 & 0xffL);
	}

	private void copyFromLongArr03() {
		final long word = ld[ldWordIndex];
		bd[bdByteIndex+0] = (byte) (word >>> 000 & 0xffL);
		bd[bdByteIndex+1] = (byte) (word >>> 010 & 0xffL);
		bd[bdByteIndex+2] = (byte) (word >>> 020 & 0xffL);
	}

	private void copyFromLongArr04() {
		final long word = ld[ldWordIndex];
		bd[bdByteIndex+0] = (byte) (word >>> 000 & 0xffL);
		bd[bdByteIndex+1] = (byte) (word >>> 010 & 0xffL);
		bd[bdByteIndex+2] = (byte) (word >>> 020 & 0xffL);
		bd[bdByteIndex+3] = (byte) (word >>> 030 & 0xffL);
	}

	private void copyFromLongArr05() {
		final long word = ld[ldWordIndex];
		bd[bdByteIndex+0] = (byte) (word >>> 000 & 0xffL);
		bd[bdByteIndex+1] = (byte) (word >>> 010 & 0xffL);
		bd[bdByteIndex+2] = (byte) (word >>> 020 & 0xffL);
		bd[bdByteIndex+3] = (byte) (word >>> 030 & 0xffL);
		bd[bdByteIndex+4] = (byte) (word >>> 040 & 0xffL);
	}

	private void copyFromLongArr06() {
		final long word = ld[ldWordIndex];
		bd[bdByteIndex+0] = (byte) (word >>> 000 & 0xffL);
		bd[bdByteIndex+1] = (byte) (word >>> 010 & 0xffL);
		bd[bdByteIndex+2] = (byte) (word >>> 020 & 0xffL);
		bd[bdByteIndex+3] = (byte) (word >>> 030 & 0xffL);
		bd[bdByteIndex+4] = (byte) (word >>> 040 & 0xffL);
		bd[bdByteIndex+5] = (byte) (word >>> 050 & 0xffL);
	}

	private void copyFromLongArr07() {
		final long word = ld[ldWordIndex];
		bd[bdByteIndex+0] = (byte) (word >>> 000 & 0xffL);
		bd[bdByteIndex+1] = (byte) (word >>> 010 & 0xffL);
		bd[bdByteIndex+2] = (byte) (word >>> 020 & 0xffL);
		bd[bdByteIndex+3] = (byte) (word >>> 030 & 0xffL);
		bd[bdByteIndex+4] = (byte) (word >>> 040 & 0xffL);
		bd[bdByteIndex+5] = (byte) (word >>> 050 & 0xffL);
		bd[bdByteIndex+6] = (byte) (word >>> 060 & 0xffL);
	}

	private void copyFromLongArr08() {
		final long word = ld[ldWordIndex];
		bd[bdByteIndex+0] = (byte) (word >>> 000 & 0xffL);
		bd[bdByteIndex+1] = (byte) (word >>> 010 & 0xffL);
		bd[bdByteIndex+2] = (byte) (word >>> 020 & 0xffL);
		bd[bdByteIndex+3] = (byte) (word >>> 030 & 0xffL);
		bd[bdByteIndex+4] = (byte) (word >>> 040 & 0xffL);
		bd[bdByteIndex+5] = (byte) (word >>> 050 & 0xffL);
		bd[bdByteIndex+6] = (byte) (word >>> 060 & 0xffL);
		bd[bdByteIndex+7] = (byte) (word >>> 070);
	}

	private void copyFromLongArr41() {
		bd[bdByteIndex] = (byte) (ld[ldWordIndex] >>> 040 & 0xffL);
	}

	private void copyFromLongArr42() {
		final long word = ld[ldWordIndex];
		bd[bdByteIndex+0] = (byte) (word >>> 040 & 0xffL);
		bd[bdByteIndex+1] = (byte) (word >>> 050 & 0xffL);
	}

	private void copyFromLongArr43() {
		final long word = ld[ldWordIndex];
		bd[bdByteIndex+0] = (byte) (word >>> 040 & 0xffL);
		bd[bdByteIndex+1] = (byte) (word >>> 050 & 0xffL);
		bd[bdByteIndex+2] = (byte) (word >>> 060 & 0xffL);
	}

	private void copyFromLongArr44() {
		final long word = ld[ldWordIndex];
		bd[bdByteIndex+0] = (byte) (word >>> 040 & 0xffL);
		bd[bdByteIndex+1] = (byte) (word >>> 050 & 0xffL);
		bd[bdByteIndex+2] = (byte) (word >>> 060 & 0xffL);
		bd[bdByteIndex+3] = (byte) (word >>> 070);
		++ldWordIndex;
	}

	private void copyFromLongArrMN(int byteIndex, int length) {
		assert 1 <= byteIndex && byteIndex < 8 && 4 != byteIndex;
		assert 0 < length && length < 8;
		int shift = byteIndex * 010;
		final int maxShift = shift + (length - 1) * 010;
		final long word = ld[ldWordIndex++];
		bd[bdByteIndex++] = (byte) (word >>> shift & 0xffL);
		while (shift < maxShift) {
			shift += 010;
			bd[bdByteIndex++] = (byte) (word >>> shift & 0xffL);
		}
	}

	private int copyFromLongArrMNCheckZero(int byteIndex, int length) {
		assert 1 <= byteIndex && byteIndex < 8 && 4 != byteIndex;
		assert 0 < length && length < 8;
		int shift = byteIndex * 010;
		final int maxShift = shift + (length - 1) * 010;
		final long word = ld[ldWordIndex++];
		byte b = (byte) (word >>> shift & 0xffL);
		bd[bdByteIndex++] = b;
		if (0 == b) {
			return 0;
		}
		for (int k = 1; shift < maxShift; ++k) {
			shift += 010;
			b = (byte) (word >>> shift & 0xffL);
			bd[bdByteIndex++] = b;
			if (0 == b) {
				return k;
			}
		}
		return length;
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

	private int whereZero5() {
		if (0 == bd[bdByteIndex]) {
			return bdByteIndex;
		} else if (0 == bd[bdByteIndex+1]) {
			return bdByteIndex + 1;
		} else if (0 == bd[bdByteIndex+2]) {
			return bdByteIndex + 2;
		} else if (0 == bd[bdByteIndex+3]) {
			return bdByteIndex + 3;
		} else if (0 == bd[bdByteIndex+4]) {
			return bdByteIndex + 4;
		} else {
			return bdByteIndex + 5;
		}
	}

	private int whereZero6() {
		if (0 == bd[bdByteIndex]) {
			return bdByteIndex;
		} else if (0 == bd[bdByteIndex+1]) {
			return bdByteIndex + 1;
		} else if (0 == bd[bdByteIndex+2]) {
			return bdByteIndex + 2;
		} else if (0 == bd[bdByteIndex+3]) {
			return bdByteIndex + 3;
		} else if (0 == bd[bdByteIndex+4]) {
			return bdByteIndex + 4;
		} else if (0 == bd[bdByteIndex+5]) {
			return bdByteIndex + 5;
		} else {
			return bdByteIndex + 6;
		}
	}

	private int whereZero7() {
		if (0 == bd[bdByteIndex]) {
			return bdByteIndex;
		} else if (0 == bd[bdByteIndex+1]) {
			return bdByteIndex + 1;
		} else if (0 == bd[bdByteIndex+2]) {
			return bdByteIndex + 2;
		} else if (0 == bd[bdByteIndex+3]) {
			return bdByteIndex + 3;
		} else if (0 == bd[bdByteIndex+4]) {
			return bdByteIndex + 4;
		} else if (0 == bd[bdByteIndex+5]) {
			return bdByteIndex + 5;
		} else if (0 == bd[bdByteIndex+6]) {
			return bdByteIndex + 6;
		} else {
			return bdByteIndex + 7;
		}
	}

	private int whereZero8() {
		if (0 == bd[bdByteIndex]) {
			return bdByteIndex;
		} else if (0 == bd[bdByteIndex+1]) {
			return bdByteIndex + 1;
		} else if (0 == bd[bdByteIndex+2]) {
			return bdByteIndex + 2;
		} else if (0 == bd[bdByteIndex+3]) {
			return bdByteIndex + 3;
		} else if (0 == bd[bdByteIndex+4]) {
			return bdByteIndex + 4;
		} else if (0 == bd[bdByteIndex+5]) {
			return bdByteIndex + 5;
		} else if (0 == bd[bdByteIndex+6]) {
			return bdByteIndex + 6;
		} else if (0 == bd[bdByteIndex+7]) {
			return bdByteIndex + 7;
		} else {
			return bdByteIndex + 8;
		}
	}

}
