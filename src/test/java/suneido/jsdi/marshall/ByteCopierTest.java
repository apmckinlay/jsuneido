package suneido.jsdi.marshall;

import static org.junit.Assert.*;

import org.junit.Test;

import suneido.jsdi.DllInterface;

/**
 * Test for {@link ByteCopier}.
 *
 * @author Victor Schappert
 * @since 20140728
 * @see suneido.jsdi.ByteCopier
 */
@DllInterface
public class ByteCopierTest {

	private static final byte[] IN_BYTES = { 1, 2, 3, 4, 5, 6, 7, 8, 9, 0xa,
			0xb, 0xc, 0xd, 0xe, 0xf, 'a', 'b', 'c', 'd', 'e', 'f', 'g', 'h',
			'i', 'j', 'k', 'l', 'm', 'p', 'q', 'r', 's' };

	@Test
	public void testAssumptions() {
		assertEquals(32, IN_BYTES.length);
	}

	@Test
	public void testCopyToLen1() {
		long[] ld = new long[1];
		ByteCopier bc = new ByteCopier(ld, 0, IN_BYTES);
		bc.copyToLongArr(1);
		assertEquals(0x0000000000000001L, ld[0]);
		ld = new long[1];
		bc = new ByteCopier(ld, 1, IN_BYTES);
		bc.copyToLongArr(1);
		assertEquals(0x0000000000000100L, ld[0]);
		ld = new long[1];
		bc = new ByteCopier(ld, 2, IN_BYTES);
		bc.copyToLongArr(1);
		assertEquals(0x0000000000010000L, ld[0]);
		ld = new long[1];
		bc = new ByteCopier(ld, 3, IN_BYTES);
		bc.copyToLongArr(1);
		assertEquals(0x0000000001000000L, ld[0]);
		ld = new long[1];
		bc = new ByteCopier(ld, 4, IN_BYTES);
		bc.copyToLongArr(1);
		assertEquals(0x0000000100000000L, ld[0]);
		ld = new long[1];
		bc = new ByteCopier(ld, 5, IN_BYTES);
		bc.copyToLongArr(1);
		assertEquals(0x0000010000000000L, ld[0]);
		ld = new long[1];
		bc = new ByteCopier(ld, 6, IN_BYTES);
		bc.copyToLongArr(1);
		assertEquals(0x0001000000000000L, ld[0]);
		ld = new long[1];
		bc = new ByteCopier(ld, 7, IN_BYTES);
		bc.copyToLongArr(1);
		assertEquals(0x0100000000000000L, ld[0]);
	}

	@Test
	public void testCopyFromLen1() {
		byte[] bd = new byte[1];
		ByteCopier bc = new ByteCopier(new long[] { 0x0000000000000001L }, 0, bd);
		bc.copyFromLongArr(1);
		assertEquals(1, bd[0]);
		bd = new byte[1];
		bc = new ByteCopier(new long[] { 0x0000000000000100L }, 1, bd);
		bc.copyFromLongArr(1);
		assertEquals(1, bd[0]);
		bd = new byte[1];
		bc = new ByteCopier(new long[] { 0x0000000000010000L }, 2, bd);
		bc.copyFromLongArr(1);
		assertEquals(1, bd[0]);
		bd = new byte[1];
		bc = new ByteCopier(new long[] { 0x0000000001000000L }, 3, bd);
		bc.copyFromLongArr(1);
		assertEquals(1, bd[0]);
		bd = new byte[1];
		bc = new ByteCopier(new long[] { 0x0000000100000000L }, 4, bd);
		bc.copyFromLongArr(1);
		assertEquals(1, bd[0]);
		bd = new byte[1];
		bc = new ByteCopier(new long[] { 0x0000010000000000L }, 5, bd);
		bc.copyFromLongArr(1);
		assertEquals(1, bd[0]);
		bd = new byte[1];
		bc = new ByteCopier(new long[] { 0x0001000000000000L }, 6, bd);
		bc.copyFromLongArr(1);
		assertEquals(1, bd[0]);
		bd = new byte[1];
		bc = new ByteCopier(new long[] { 0x0100000000000000L }, 7, bd);
		bc.copyFromLongArr(1);
		assertEquals(1, bd[0]);
	}

	@Test
	public void testCopyToAndFro() {
		// nLongs:      number of longs in long[], so you can copy up to
		//              MAX_BYTES bytes
		// nBytes:      number of bytes to copy in this pass: 0..MAX_BYTES
		// ldByteIndex: start *byte* index within long[] ... for any given value
		//              of nBytes, this can take a value in the range
		//              0..MAX_BYTES-nBytes
		for (int nLongs = 1; nLongs <= 4; ++nLongs) {
			final int MAX_BYTES = 8 * nLongs;
			for (int nBytes = 0; nBytes < MAX_BYTES; ++nBytes) {
				for (int ldByteIndex = 0; ldByteIndex < MAX_BYTES - nBytes; ++ldByteIndex) {
					final long[] ld = new long[nLongs];
					final byte[] bdIn = new byte[nBytes];
					System.arraycopy(IN_BYTES, ldByteIndex, bdIn, 0, nBytes);
					final ByteCopier bcIn = new ByteCopier(ld, ldByteIndex,
							bdIn);
					bcIn.copyToLongArr(nBytes);
					final byte[] bdOut = new byte[nBytes];
					final ByteCopier bcOut = new ByteCopier(ld, ldByteIndex,
							bdOut);
					bcOut.copyFromLongArr(nBytes);
					assertArrayEquals(bdIn, bdOut);
				}
			}
		}
	}

	@Test
	public void testCopyFromNonZeroLen1() {
		byte[] bd = new byte[1];
		ByteCopier bc = new ByteCopier(new long[] { 0x0000000000000001L }, 0, bd);
		assertEquals(1, bc.copyNonZeroFromLongArr(1));
		assertEquals(1, bd[0]);
		bd = new byte[1];
		bc = new ByteCopier(new long[] { 0x0000000000000100L }, 1, bd);
		assertEquals(1, bc.copyNonZeroFromLongArr(1));
		assertEquals(1, bd[0]);
		bd = new byte[1];
		bc = new ByteCopier(new long[] { 0x0000000000010000L }, 2, bd);
		assertEquals(1, bc.copyNonZeroFromLongArr(1));
		assertEquals(1, bd[0]);
		bd = new byte[1];
		bc = new ByteCopier(new long[] { 0x0000000001000000L }, 3, bd);
		assertEquals(1, bc.copyNonZeroFromLongArr(1));
		assertEquals(1, bd[0]);
		bd = new byte[1];
		bc = new ByteCopier(new long[] { 0x0000000100000000L }, 4, bd);
		assertEquals(1, bc.copyNonZeroFromLongArr(1));
		assertEquals(1, bd[0]);
		bd = new byte[1];
		bc = new ByteCopier(new long[] { 0x0000010000000000L }, 5, bd);
		assertEquals(1, bc.copyNonZeroFromLongArr(1));
		assertEquals(1, bd[0]);
		bd = new byte[1];
		bc = new ByteCopier(new long[] { 0x0001000000000000L }, 6, bd);
		assertEquals(1, bc.copyNonZeroFromLongArr(1));
		assertEquals(1, bd[0]);
		bd = new byte[1];
		bc = new ByteCopier(new long[] { 0x0100000000000000L }, 7, bd);
		assertEquals(1, bc.copyNonZeroFromLongArr(1));
		assertEquals(1, bd[0]);
	}

	@Test
	public void testCopyToAndFroNonZero() {
		// nLongs:      number of longs in long[], so you can copy up to
		//              MAX_BYTES bytes
		// nBytes:      number of bytes to copy in this pass: 0..MAX_BYTES
		// ldByteIndex: start *byte* index within long[] ... for any given value
		//              of nBytes, this can take a value in the range
		//              0..MAX_BYTES-nBytes
		// zeroPos:     rotating zero position in the byte array: a number from
		//              0..nBytes, where 0..nBytes-1 is an index and nBytes
		//              means no zero.
		for (int nLongs = 1; nLongs <= 4; ++nLongs) {
			final int MAX_BYTES = 8 * nLongs;
			for (int nBytes = 0; nBytes < MAX_BYTES; ++nBytes) {
				for (int ldByteIndex = 0; ldByteIndex < MAX_BYTES - nBytes; ++ldByteIndex) {
					for (int zeroPos = 0; zeroPos <= nBytes; ++zeroPos) {
						final long[] ld = new long[nLongs];
						final byte[] bdIn = new byte[nBytes];
						System.arraycopy(IN_BYTES, ldByteIndex, bdIn, 0, nBytes);
						if (zeroPos < nBytes) {
							bdIn[zeroPos] = 0;
						}
						final ByteCopier bcIn = new ByteCopier(ld, ldByteIndex,
								bdIn);
						bcIn.copyToLongArr(nBytes);
						final byte[] bdOut = new byte[nBytes];
						final ByteCopier bcOutNormal = new ByteCopier(ld,
								ldByteIndex, bdOut);
						bcOutNormal.copyFromLongArr(nBytes);
						assertArrayEquals(bdIn, bdOut);
						final ByteCopier bcOutNonZero = new ByteCopier(ld, ldByteIndex, bdOut);
						int zeroPosFound = bcOutNonZero.copyNonZeroFromLongArr(nBytes);
						assertEquals(zeroPos, zeroPosFound);
					} // for zeroPos
				} // for ldByteIndex
			} // for nBytes
		} // for nLongs
	}

	@Test
	public void testCopyToNoOverlap() {
		long[] ld = new long[1];
		byte[] bdAB = new byte[] { (byte)'A', (byte)'B' };
		byte[] bdCD = new byte[] { (byte)'C', (byte)'D' };
		byte[] bdABCD = new byte[4];
		ByteCopier bc1 = new ByteCopier(ld, 1, bdAB);
		bc1.copyToLongArr(2);
		ByteCopier bc2 = new ByteCopier(ld, 3, bdCD);
		bc2.copyToLongArr(2);
		ByteCopier bc3 = new ByteCopier(ld, 1, bdABCD);
		bc3.copyFromLongArr(4);
		assertArrayEquals(new byte[] { bdAB[0], bdAB[1], bdCD[0], bdCD[1] }, bdABCD);
	}
}
