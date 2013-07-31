package suneido.language.jsdi;

import static org.junit.Assert.*;

import org.junit.Test;

import suneido.language.Compiler;

/**
 * Test for {@link Buffer}.
 * 
 * @author Victor Schappert
 * @since 20130721
 */
public class BufferTest {

	private static final Buffer[] SOME_BUFFERS = new Buffer[] {
			new Buffer(0, ""), new Buffer(1, ""), new Buffer(2, ""),
			new Buffer(1, "A"), new Buffer(1, "!"), new Buffer(2, "A"),
			new Buffer(2, "!"), new Buffer(2, "A!"), new Buffer(1000, "xxxx") };

	private static Buffer doubleCapacity(Buffer old) {
		byte[] bOld = old.getInternalData();
		byte[] bNew = new byte[2 * bOld.length];
		Buffer new_ = new Buffer(bNew, 0, bNew.length);
		for (int k = 0; k < bOld.length; ++k) {
			bNew[k] = bOld[k];
		}
		new_.setAndSetSize(bNew, 0, old.size());
		assert new_.size() == old.size();
		return new_;
	}

	@Test
	public void testEqualsString() {
		assertEquals(new Buffer(0, ""), "");
		assertEquals(new Buffer(1, ""), "\u0000");
		assertEquals(new Buffer(1, "A"), "A");
		assertEquals(new Buffer(100, ""), new String(new char[100]));
	}

	@Test
	public void testEqualsIdentity() {
		for (Buffer buffer : SOME_BUFFERS) {
			assertEquals(buffer, buffer);
		}
	}

	@Test
	public void testEqualsToString() {
		for (Buffer buffer : SOME_BUFFERS) {
			assertEquals(buffer, buffer.toString());
		}
	}

	@Test
	public void testEqualsNull() {
		for (Buffer buffer : SOME_BUFFERS) {
			assertFalse(buffer.equals((Object) null));
			assertFalse(buffer.equals((Buffer) null));
		}
	}

	@Test
	public void testEqualsDifferent() {
		for (Buffer b1 : SOME_BUFFERS)
			for (Buffer b2 : SOME_BUFFERS)
				assertTrue(b1 == b2 || !b1.equals(b2));
	}

	@Test
	public void testEqualsSame() {
		for (Buffer b1 : SOME_BUFFERS) {
			Buffer b2 = new Buffer(b1.getInternalData(), 0,
					b1.getInternalData().length);
			assertEquals(b1, b2);
		}
	}

	@Test
	public void testEqualsCapacityChange() {
		for (Buffer b : SOME_BUFFERS)
			assertEquals(b, doubleCapacity(b));
	}

	@Test
	public void testEqualsUnequalString() {
		for (Buffer b : SOME_BUFFERS) {
			assertFalse(b.equals((String) null));
			assertFalse(b.equals(b.toString() + "X"));
			if (0 < b.size()) {
				String str = b.toString();
				str = str.substring(0, str.length() - 1)
						+ (str.charAt(str.length() - 1) + 1);
				assertFalse(b.equals(str));
			}
		}
	}

	@Test
	public void testSetAndSetSize() {
		Buffer buffer = new Buffer(10, "abcdef");
		byte[] b = new byte[4];
		buffer.copyInternalData(b, 1, 3);
		assertArrayEquals(new byte[] { 0, (byte)'a', (byte)'b', (byte)'c' }, b);
		assertEquals(10, buffer.size());
		assertEquals(10, buffer.capacity());
		buffer.setAndSetSize(b, 0, 2);
		assertEquals(2, buffer.size());
		assertEquals(10, buffer.capacity());
		assertEquals("\u0000a", buffer.toString());
		buffer.setAndSetSize(b, 0, 4);
		assertEquals(4, buffer.size());
		assertEquals(10, buffer.capacity());
		assertEquals("\u0000abc", buffer.toString());
	}

	@Test
	public void testSize() {
		assertEquals(0, Compiler.eval("Buffer(0, '').Size()"));
		assertEquals(1, Compiler.eval("Buffer(1, '').Size()"));
		assertEquals(1, Compiler.eval("Buffer(1, 'a').Size()"));
	}

	@Test(expected=JSDIException.class)
	public void testInvalidSize() {
		Compiler.eval("Buffer(-123.3, 'string');");
	}
}
