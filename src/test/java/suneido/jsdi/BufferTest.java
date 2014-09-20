/* Copyright 2013 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido.jsdi;

import static org.junit.Assert.*;
import static suneido.util.testing.Throwing.assertThrew;

import java.nio.ByteBuffer;

import org.junit.Test;

import suneido.compiler.Compiler;
import suneido.jsdi.Buffer;
import suneido.jsdi.JSDIException;
import suneido.runtime.Pack;

/**
 * Test for {@link Buffer}.
 *
 * @author Victor Schappert
 * @since 20130721
 */
@DllInterface
public class BufferTest {

	private static final Buffer copyBuffer(Buffer buffer) {
		return new Buffer(buffer.length(), buffer);
	}

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
		new_.setAndSetSize(bNew, 0, old.length());
		assert new_.length() == old.length();
		return new_;
	}

	private static Object eval(String code) {
		return Compiler.eval(code);
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
			if (0 < b.length()) {
				String str = b.toString();
				str = str.substring(0, str.length() - 1)
						+ (str.charAt(str.length() - 1) + 1);
				assertFalse(b.equals(str));
			}
		}
	}

	@Test
	public void testHashCode() {
		for (Buffer b1 : SOME_BUFFERS) {
			for (Buffer b2 : SOME_BUFFERS) {
				if (b1 == b2) {
					b2 = copyBuffer(b2);
					assertEquals(b1, b2);
					assertEquals(b2, b1);
					assertEquals(b1.hashCode(), b2.hashCode());
				}
				b1 = copyBuffer(b1);
				b2 = copyBuffer(b2);
				b1.truncate();
				b2.truncate();
				if (b1.equals(b2)) {
					assertEquals(b1.hashCode(), b2.hashCode());
				}
			}
		}

	}

	@Test
	public void testSize() {
		assertEquals(0, new Buffer(0, "").length());
		assertEquals(1, Compiler.eval("Buffer(1).Size()"));
		assertEquals(1, Compiler.eval("Buffer(1, '').Size()"));
		assertEquals(1, Compiler.eval("Buffer(1, 'a').Size()"));
	}

	@Test(expected=JSDIException.class)
	public void testSizeCantBeExplicitlyZero() {
		Compiler.eval("Buffer(0)");
	}

	@Test(expected=JSDIException.class)
	public void testSizeInvalid() {
		Compiler.eval("Buffer(-123.3, 'string');");
	}

	@Test
	public void testSizeBigEnoughForContents() {
		for (final String s : new String[] { "Buffer(2)", "'ab'" }) {
			assertThrew(() -> {
				String code = String.format("Buffer(1, %s)", s);
				Compiler.eval(code);
			}, JSDIException.class, "Buffer must be large enough for initial");
		}
	}

	@Test
	public void testTruncate() {
		{
			Buffer buffer = new Buffer(0, "");
			buffer.truncate();
			assertEquals(0, buffer.length());
			assertEquals(buffer, "");
		}
		{
			Buffer buffer = new Buffer(1, "a");
			assertEquals(1, buffer.length());
			assertEquals(buffer, "a");
			buffer.truncate();
			assertEquals(1, buffer.length());
			assertEquals(buffer, "a");
		}
		{
			Buffer buffer = new Buffer(10, "");
			assertFalse(buffer.equals(""));
			buffer.truncate();
			assertEquals(0, buffer.length());
			assertEquals(buffer, "");
		}
		{
			Buffer buffer = new Buffer(3, "a");
			assertEquals(buffer, "a\u0000\u0000");
			assertEquals(3, buffer.length());
			assertFalse(buffer.equals("a"));
			buffer.truncate();
			assertEquals(1, buffer.length());
			assertEquals(buffer, "a");
		}
	}

	@Test
	public void testCharSequence_length() {
		Buffer b = new Buffer(100, "x");
		assertEquals(100, b.length());
		b.truncate();
		assertEquals(1, b.length());
		b = new Buffer(0, "");
		assertEquals(0, b.length());
		b = new Buffer(5, "hello");
		assertEquals(5, b.length());
		b.truncate();
		assertEquals(5, b.length());
	}

	@Test
	public void testCharSequence_charAt() {
		for (final String S : new String[] { "", "$", "xy", "@@@",
				"...mankind are more disposed to suffer while the evils are sufferable..." }) {
			for (final int N : new int[] { 0, 1, 2, 3, 4, 5, 50, 100 }) {
				String s = S;
				String s2 = s;
				if (N < s.length()) {
					s = s.substring(0, N);
					s2 = s;
				} else if (s.length() < N) {
					s2 = s + new String(new char[N - s.length()]); // zero pad right
				}
				final Buffer b = new Buffer(N, s);
				for (int k = 0; k < N; ++k) {
					assertEquals(s2.charAt(k), b.charAt(k));
				}
				assertThrew(() -> { b.charAt(N); },
						IndexOutOfBoundsException.class);
				assertThrew(() -> { b.charAt(-1); },
						IndexOutOfBoundsException.class);
			}
		}
	}

	@Test
	public void testCharSequence_subSequence() {
		for (final String S : new String[] { "", "$", "xy", "@@@",
		"...mankind are more disposed to suffer while the evils are sufferable..." }) {
			for (final int N : new int[] { 0, 1, 2, 3, 4, 5, 50, 100 }) {
				String s = S;
				String s2 = s;
				if (N < s.length()) {
					s = s.substring(0, N);
					s2 = s;
				} else if (s.length() < N) {
					s2 = s + new String(new char[N - s.length()]); // zero pad right
				}
				final Buffer b = new Buffer(N, s);
				assertEquals(N, b.length());
				assertEquals(s2, b.toString());
				for (int i = 0; i <= N; ++i) {
					for (int j = i; j <= N; ++j) {
						CharSequence x = s2.subSequence(i, j);
						CharSequence y = b.subSequence(i, j);
						assertEquals(x, y);
						assertEquals(y, x);
					}
				}
				assertThrew(() -> { b.subSequence(0, N + 1); },
						StringIndexOutOfBoundsException.class);
				assertThrew(() -> { b.subSequence(-1, N); },
						StringIndexOutOfBoundsException.class);

			}
		}
	}

	@Test
	public void testStringQ() {
		// To be consistent with CSuneido
		assertSame(Boolean.TRUE, eval("String?(Buffer(10, 'hello'))"));
	}

	@Test
	public void testBufferQ() {
		assertSame(Boolean.TRUE, eval("Buffer?(Buffer(10, 'hello'))"));

	}

	@Test
	public void testGet_Char() {
		for (Buffer b : SOME_BUFFERS) {
			String s = b.toString();
			assertEquals(s.length(), b.length());
			for (int k = 0; k < b.length(); ++k) {
				assertEquals(s.charAt(k), ((CharSequence)b.get(k)).charAt(0));
				Buffer bTemp = copyBuffer(b).truncate();
				String sTemp = bTemp.toString();
				assertEquals(
						"" + s.charAt(k) /* convert char -> String */,
						eval(String.format("(Buffer(%d, '%s'))[%d]", b.length(), sTemp, k))
				);
			}
		}
	}

	@Test
	public void testGet_RangeTo() {
		assertEquals("", eval("(Buffer(1, '1'))[0..0]"));
		assertEquals("1", eval("(Buffer(1, '1'))[0..1]"));
		assertEquals("", eval("(Buffer(1, '1'))[1..1]"));
		assertEquals("", eval("(Buffer(1, '1'))[1..0]"));

		assertEquals("", eval("(Buffer(2, '1'))[0..0]"));
		assertEquals("1", eval("(Buffer(2, '1'))[0..1]"));
		assertEquals("1\u0000", eval("(Buffer(2, '1'))[0..2]"));
		assertEquals("\u0000", eval("(Buffer(2, '1'))[1..2]"));
		assertEquals("", eval("(Buffer(2, '1'))[2..3]"));
	}

	@Test
	public void testGet_RangeLen() {
		assertEquals("", eval("(Buffer(1, '1'))[0::0]"));
		assertEquals("1", eval("(Buffer(1, '1'))[0::1]"));
		assertEquals("", eval("(Buffer(1, '1'))[1::1]"));
		assertEquals("", eval("(Buffer(1, '1'))[1::0]"));

		assertEquals("", eval("(Buffer(2, '1'))[0::0]"));
		assertEquals("1", eval("(Buffer(2, '1'))[0::1]"));
		assertEquals("1\u0000", eval("(Buffer(2, '1'))[0::2]"));
		assertEquals("1\u0000", eval("(Buffer(2, '1'))[0::]"));
		assertEquals("1\u0000", eval("(Buffer(2, '1'))[::2]"));
		assertEquals("\u0000", eval("(Buffer(2, '1'))[1::1]"));
		assertEquals("\u0000", eval("(Buffer(2, '1'))[1::2]"));
		assertEquals("", eval("(Buffer(2, '1'))[2::3]"));
		assertEquals("\u0000", eval("(Buffer(2, '1'))[-1::1]"));
		assertEquals("\u0000", eval("(Buffer(2, '1'))[-1::3]"));
		assertEquals("1\u0000", eval("(Buffer(2, '1'))[-2::2]"));
	}

	@Test
	public void testType() {
		assertEquals("Buffer", eval("Type(Buffer(1, ''))"));
	}

	@Test
	public void testPackSize() {
		assertEquals(0, new Buffer(0, "").packSize());
		for (Buffer b : SOME_BUFFERS) {
			assertEquals(b.packSize(), Pack.packSize(b.toString()));
		}
	}

	@Test
	public void testPacking() {
		for (Buffer b : SOME_BUFFERS) {
			ByteBuffer bb = Pack.pack(b);
			Object u = Pack.unpack(bb);
			assertEquals(b.toString(), u);
		}
	}

	@Test
	public void testIter() {
		assertEquals("gfedcba",
			eval(
				"x = Buffer(7, 'abcdefg')\n" +
				"y = x.Iter()\n" +
				"z = y.Next()\n" +
				"a = ''\n" +
				"while (z isnt y)\n" +
				"    {\n" +
				"    a = z $ a\n" +
				"    z = y.Next()\n" +
				"    }\n" +
				"a"
			)
		);
	}
}
