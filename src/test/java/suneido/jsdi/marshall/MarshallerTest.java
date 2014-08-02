package suneido.jsdi.marshall;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.BeforeClass;
import org.junit.Test;

import suneido.util.testing.Assumption;

/**
 * Test for {@link Marshaller}.
 *
 * @author Victor Schappert
 * @since 20140801
 */
public class MarshallerTest {

	private static Marshaller md(Integer... sizes) {
		final int N = sizes.length;
		if (0 < N) {
			int[] posArray = new int[N];
			posArray[0] = 0;
			for (int k = 1; k < N; ++k) {
				posArray[k] = posArray[k-1] + sizes[k-1];
			}
			int sizeTotal = posArray[N-1] + sizes[N-1];
			return new TestMarshaller(
					PrimitiveSize.sizeLongs(sizeTotal), 0, null, posArray);
		}
		throw new IllegalArgumentException("can't have empty size array");
	}

	@BeforeClass
	public static void setUpBeforeClass() throws Exception {
		Assumption.jvmIsOnWindows();
	}

	@Test
	public void testPutBoolRegression() {
		// This is a regression test for an issue I found 20140801 introduced
		// when Marshaller was refactored from byte[] -> long[]. Putting a bool
		// was annihilating the whole 8-byte long rather than the four bytes
		// where it belongs.
		Marshaller m = md(PrimitiveSize.INT32, PrimitiveSize.BOOL,
				PrimitiveSize.BOOL, PrimitiveSize.INT32, PrimitiveSize.INT8,
				PrimitiveSize.INT8, PrimitiveSize.INT16, PrimitiveSize.BOOL);
		m.putInt32(0x10101010);
		m.putBool(true);
		m.putBool(false);
		m.putInt32(0x89abcdef);
		m.putInt8((byte)'@');
		m.putInt8((byte)'$');
		m.putInt16((short)0x4444);
		m.putBool(true);
		m.rewind();
		assertEquals(0x10101010, m.getInt32());
		assertTrue(m.getBool());
		assertFalse(m.getBool());
		assertEquals(0x89abcdef, m.getInt32());
		assertEquals((int)'@', m.getInt8());
		assertEquals((int)'$', m.getInt8());
		assertEquals(0x4444, m.getInt16());
		assertTrue(m.getBool());
	}
}
