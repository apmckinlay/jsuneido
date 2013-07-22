package suneido.language.jsdi;

import static org.junit.Assert.*;

import org.junit.Test;

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
}
