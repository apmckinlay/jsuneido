package suneido.language.jsdi.dll;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static suneido.language.jsdi.MarshallTestUtil.pointerPlan;
import static suneido.language.jsdi.VariableIndirectInstruction.NO_ACTION;
import static suneido.language.jsdi.VariableIndirectInstruction.RETURN_JAVA_STRING;
import static suneido.util.testing.Throwing.assertThrew;

import java.util.ArrayList;

import org.junit.BeforeClass;
import org.junit.Test;

import suneido.language.jsdi.*;
import suneido.language.jsdi.type.PrimitiveSize;
import suneido.util.testing.Assumption;

/**
 * Test for the native calling mechanism using the <code>_Test*@#</code>
 * functions exported from the JSDI DLL.
 * 
 * @author Victor Schappert
 * @since 20130723
 * @see NativeCall
 */
public class NativeCallTest {

	private static final NativeCall[] DOF_NORET_VI_OR_FLOAT;
	private static final NativeCall[] IND_NORET_VI_OR_FLOAT;
	private static final NativeCall[] VI_NORET_VI_OR_FLOAT;
	private static final NativeCall[] VI_RETVI;
	static {
		ArrayList<NativeCall> dof_noret_vi_or_float = new ArrayList<NativeCall>();
		ArrayList<NativeCall> ind_noret_vi_or_float = new ArrayList<NativeCall>();
		ArrayList<NativeCall> vi_noret_vi_or_float = new ArrayList<NativeCall>();
		ArrayList<NativeCall> vi_retvi = new ArrayList<NativeCall>();
		for (NativeCall nativecall : NativeCall.values()) {
			if (nativecall.isFloatingPointReturn())
				continue;
			if (ReturnTypeGroup.VARIABLE_INDIRECT == nativecall
					.getReturnTypeGroup()) {
				vi_retvi.add(nativecall);
			} else {
				if (nativecall.isDirectOrFast()) {
					dof_noret_vi_or_float.add(nativecall);
				} else if (CallGroup.INDIRECT == nativecall.getCallGroup()) {
					ind_noret_vi_or_float.add(nativecall);
				} else {
					assert CallGroup.VARIABLE_INDIRECT == nativecall.getCallGroup();
					vi_noret_vi_or_float.add(nativecall);
				}
			}
		}
		DOF_NORET_VI_OR_FLOAT = dof_noret_vi_or_float.toArray(new NativeCall[dof_noret_vi_or_float.size()]);
		IND_NORET_VI_OR_FLOAT = ind_noret_vi_or_float.toArray(new NativeCall[ind_noret_vi_or_float.size()]);
		VI_NORET_VI_OR_FLOAT = vi_noret_vi_or_float.toArray(new NativeCall[vi_noret_vi_or_float.size()]);
		VI_RETVI = vi_retvi.toArray(new NativeCall[vi_retvi.size()]);
	}

	@BeforeClass
	public static void setUpBeforeClass() throws Exception {
		Assumption.jvmIs32BitOnWindows();
		// Trigger load of JSDI DLL. Otherwise, LoadLibrary() might fail if
		// JSDI DLL isn't in a Windows standard DLL search path, and you'll get
		// an UnsatisfiedLinkError.
		JSDI.getInstance();
	}

	@Test
	public void testDirect_AllZeroes() {
		// THE TEST FUNCTIONS ARE DESIGNED TO BEHAVE NICELY IF THEY GET ALL
		// ZEROS for arguments (even if they expect pointers). So as long as we
		// send the proper amount of zeroes, all should be well.
		for (TestCall testcall : TestCall.values()) {
			if (Mask.DOUBLE == testcall.returnValueMask) continue;
			for (NativeCall nativecall : DOF_NORET_VI_OR_FLOAT) {
				{
					Marshaller m = testcall.plan.makeMarshaller();
					long result = nativecall.invoke(testcall.ptr,
							testcall.plan.getSizeDirect(), m)
							& testcall.returnValueMask.value;
					assertEquals(0L, result);
				}
			}
		}
	}

	@Test
	public void testDirect_AllZeroes_FastCalling() {
		for (TestCall testcall : TestCall.values()) {
			if (Mask.DOUBLE == testcall.returnValueMask) continue; 
			long result = 0xbe5077eddeadbea7L;
			switch (PrimitiveSize.minWholeWords(testcall.plan.getSizeDirect())) {
			case 0:
				result = NativeCall.callReturnInt64(testcall.ptr);
				break;
			case 1:
				result = NativeCall.callLReturnInt64(testcall.ptr, 0);
				break;
			case 2:
				result = NativeCall.callLLReturnInt64(testcall.ptr, 0, 0);
				break;
			case 3:
				result = NativeCall.callLLLReturnInt64(testcall.ptr, 0, 0, 0);
				break;
			default:
				continue;
			}
			assertEquals(0L, result & testcall.returnValueMask.value);
		}
	}

	@Test
	public void testDirect_32bit_FastCalling() {
		// little-endian
		assertEquals(27L, NativeCall.callLReturnInt64(TestCall.CHAR.ptr, 27)
				& TestCall.CHAR.returnValueMask.value);
		assertEquals(0x2728L,
				NativeCall.callLReturnInt64(TestCall.SHORT.ptr, 0x2728)
						& TestCall.SHORT.returnValueMask.value);
		assertEquals(0x18120207,
				NativeCall.callLReturnInt64(TestCall.LONG.ptr, 0x18120207)
						& TestCall.LONG.returnValueMask.value);
		// little-endian, and pushed on the stack in reverse order
		assertEquals(0xdeadbeef19820207L, NativeCall.callLLReturnInt64(
				TestCall.INT64.ptr, 0x19820207, 0xdeadbeef));
		// sum functions
		assertEquals(3L,
				NativeCall.callLLReturnInt64(TestCall.SUM_TWO_LONGS.ptr, 1, 2)
						& TestCall.SUM_TWO_LONGS.returnValueMask.value);
		assertEquals(
				-1,
				(int)(NativeCall.callLLReturnInt64(TestCall.SUM_TWO_LONGS.ptr,
						Integer.MAX_VALUE, Integer.MIN_VALUE)
						& TestCall.SUM_TWO_LONGS.returnValueMask.value));
		assertEquals(
				6L,
				NativeCall.callLLLReturnInt64(TestCall.SUM_THREE_LONGS.ptr, 3,
						2, 1) & TestCall.SUM_THREE_LONGS.returnValueMask.value);
		assertEquals(
				0L,
				NativeCall.callLLLReturnInt64(TestCall.SUM_THREE_LONGS.ptr, 1,
						Integer.MIN_VALUE, Integer.MAX_VALUE)
						& TestCall.SUM_THREE_LONGS.returnValueMask.value);
	}

	@Test
	public void testDirect_Misc() {
		// functions that return the same value
		{
			final TestCall[] f = new TestCall[] { TestCall.CHAR,
					TestCall.SHORT, TestCall.LONG, TestCall.INT64 };
			final long[] x = new long[] { 27L, 0x2728L, 0x18120207L,
					0xdeadbeef19820207L };
			for (int k = 0; k < f.length; ++k) {
				Marshaller m = f[k].plan.makeMarshaller();
				if (PrimitiveSize.LONG == f[k].plan.getSizeDirect())
					m.putLong((int) x[k]);
				else if (PrimitiveSize.INT64 == f[k].plan.getSizeDirect())
					m.putInt64(x[k]);
				else
					throw new RuntimeException("This test is busted");
				for (NativeCall nativecall : DOF_NORET_VI_OR_FLOAT) {
					assertEquals(
							(long) x[k],
							nativecall.invoke(f[k].ptr,
									f[k].plan.getSizeDirect(), m)
									& f[k].returnValueMask.value);
				}
			}
		}
		// sum functions
		{
			Marshaller m = TestCall.SUM_TWO_LONGS.plan.makeMarshaller();
			m.putLong(1);
			m.putLong(2);
			for (NativeCall nativecall : DOF_NORET_VI_OR_FLOAT) {
				assertEquals(
						3,
						nativecall.invoke(TestCall.SUM_TWO_LONGS.ptr,
								TestCall.SUM_TWO_LONGS.plan.getSizeDirect(), m)
								& TestCall.SUM_TWO_LONGS.returnValueMask.value);
			}
		}
		{
			Marshaller m = TestCall.SUM_THREE_LONGS.plan.makeMarshaller();
			m.putLong(3);
			m.putLong(2);
			m.putLong(1);
			for (NativeCall nativecall : DOF_NORET_VI_OR_FLOAT) {
				assertEquals(
						6,
						nativecall.invoke(TestCall.SUM_THREE_LONGS.ptr,
								TestCall.SUM_THREE_LONGS.plan.getSizeDirect(),
								m)
								& TestCall.SUM_THREE_LONGS.returnValueMask.value);
			}
		}
		{
			Marshaller m = TestCall.SUM_FOUR_LONGS.plan.makeMarshaller();
			m.putLong(-100);
			m.putLong(99);
			m.putLong(-200);
			m.putLong(199);
			assertEquals(
					-2,
					(int) (NativeCall.DIRECT_RETURN_INT64.invoke(
							TestCall.SUM_FOUR_LONGS.ptr,
							TestCall.SUM_FOUR_LONGS.plan.getSizeDirect(), m) & TestCall.SUM_FOUR_LONGS.returnValueMask.value));
		}
		{
			Marshaller m = TestCall.SUM_PACKED_CHAR_CHAR_SHORT_LONG.plan
					.makeMarshaller();
			m.putChar(Byte.MIN_VALUE);
			m.putChar(Byte.MAX_VALUE);
			m.putShort(Short.MIN_VALUE);
			m.putLong(Integer.MAX_VALUE);
			assertEquals(
					(int) Byte.MIN_VALUE + (int) Byte.MAX_VALUE
							+ (int) Short.MIN_VALUE + Integer.MAX_VALUE,
					(int) (NativeCall.DIRECT_RETURN_INT64.invoke(
							TestCall.SUM_PACKED_CHAR_CHAR_SHORT_LONG.ptr,
							TestCall.SUM_PACKED_CHAR_CHAR_SHORT_LONG.plan
									.getSizeDirect(), m) & TestCall.SUM_PACKED_CHAR_CHAR_SHORT_LONG.returnValueMask.value));
		}
	}

	@Test
	public void testDirect_ReturnFloat() {
		// Functions which return 1.0
		final NativeCall n = NativeCall.DIRECT_RETURN_DOUBLE;
		{
			final TestCall[] f = { TestCall.RETURN1_0FLOAT,
					TestCall.RETURN1_0DOUBLE };
			for (TestCall testcall : f) {
				Marshaller m = testcall.plan.makeMarshaller();
				assertEquals(1.0, Double.longBitsToDouble(n.invoke(
						testcall.ptr, testcall.plan.getSizeDirect(), m)), 0.0);
			}
		}
		// Functions which return the input value
		{
			{
				TestCall testcall = TestCall.FLOAT;
				Marshaller m = testcall.plan.makeMarshaller();
				m.putFloat(33.5f);
				assertEquals(33.5, Double.longBitsToDouble(n.invoke(
						testcall.ptr, testcall.plan.getSizeDirect(), m)), 0.0);
			}
			{
				TestCall testcall = TestCall.DOUBLE;
				Marshaller m = testcall.plan.makeMarshaller();
				m.putDouble(-3333333.5f);
				assertEquals(-3333333.5, Double.longBitsToDouble(n.invoke(
						testcall.ptr, testcall.plan.getSizeDirect(), m)), 0.0);
			}
		}
		// Sum functions
		{
			{
				TestCall testcall = TestCall.SUM_TWO_FLOATS;
				Marshaller m = testcall.plan.makeMarshaller();
				m.putFloat(.75f);
				m.putFloat(-.50f);
				assertEquals(.25, Double.longBitsToDouble(n.invoke(
						testcall.ptr, testcall.plan.getSizeDirect(), m)), 0.0);
			}
			{
				TestCall testcall = TestCall.SUM_TWO_DOUBLES;
				Marshaller m = testcall.plan.makeMarshaller();
				m.putDouble(.5);
				m.putDouble(-.25);
				assertEquals(.25, Double.longBitsToDouble(n.invoke(
						testcall.ptr, testcall.plan.getSizeDirect(), m)), 0.0);
			}
		}
	}

	@Test
	public void testIndirectOnlyNullPointers() {
		MarshallPlan plan = pointerPlan(PrimitiveSize.POINTER);
		{
			Marshaller m = plan.makeMarshaller();
			m.putNullPtr();
			for (TestCall testcall : new TestCall[] {
					TestCall.HELLO_WORLD_OUT_PARAM, TestCall.NULL_PTR_OUT_PARAM }) {
				for (NativeCall nativecall : IND_NORET_VI_OR_FLOAT) {
					nativecall.invoke(testcall.ptr, plan.getSizeDirect(), m);
				}
			}
		}
	}

	@Test
	public void testIndirectOnlyReceiveValue() {
		MarshallPlan plan = pointerPlan(PrimitiveSize.POINTER);
		{
			for (NativeCall nativecall : IND_NORET_VI_OR_FLOAT) {
				Marshaller m = plan.makeMarshaller();
				m.putPtr();
				nativecall.invoke(TestCall.HELLO_WORLD_OUT_PARAM.ptr,
						plan.getSizeDirect(), m);
				m.rewind();
				assertFalse(m.isPtrNull());
				// The function called should have returned a non-zero value.
				assertFalse(0 == m.getLong());
			}
		}
		{
			for (NativeCall nativecall : IND_NORET_VI_OR_FLOAT) {
				Marshaller m = plan.makeMarshaller();
				m.putPtr();
				m.putLong(100); // This will be replaced by 0
				nativecall.invoke(TestCall.NULL_PTR_OUT_PARAM.ptr,
						plan.getSizeDirect(), m);
				m.rewind();
				assertFalse(m.isPtrNull());
				// The function called should have returned a non-zero value.
				assertEquals(0, m.getLong());
			}
		}
	}

	@Test
	public void testIndirectOnlySendValue() {
		TestCall testcall = TestCall.RETURN_PTR_PTR_PTR_DOUBLE;
		Marshaller m = testcall.plan.makeMarshaller();
		final double doubleValue = 100.0 + 1E-14;
		m.putPtr();
		m.putPtr();
		m.putPtr();
		m.putDouble(doubleValue);
		final long longValue = NativeCall.INDIRECT_RETURN_INT64.invoke(
				testcall.ptr, testcall.plan.getSizeDirect(), m);
		assertEquals(doubleValue, Double.longBitsToDouble(longValue), 0.0);
	}

	@Test
	public void testVariableIndirectOnlyNullPointers() {
		//
		// StrLen
		//
		{
			TestCall testcall = TestCall.STRLEN;
			MarshallPlan plan = testcall.plan;
			for (NativeCall nativecall : VI_NORET_VI_OR_FLOAT) {
				Marshaller m = plan.makeMarshaller();
				m.putNullStringPtr(NO_ACTION);
				nativecall.invoke(testcall.ptr, plan.getSizeDirect(), m);
			}
		}
		//
		// HelloWorldOutBuffer
		//
		{
			TestCall testcall = TestCall.HELLO_WORLD_OUT_BUFFER;
			MarshallPlan plan = testcall.plan;
			for (NativeCall nativecall : VI_NORET_VI_OR_FLOAT) {
				Marshaller m = plan.makeMarshaller();
				m.putNullStringPtr(NO_ACTION);
				m.putLong(0);
				nativecall.invoke(testcall.ptr, plan.getSizeDirect(), m);
			}
		}
		//
		// SumString
		//
		{
			TestCall testcall = TestCall.SUM_STRING;
			MarshallPlan plan = testcall.plan;
			for (NativeCall nativecall : VI_NORET_VI_OR_FLOAT) {
				final Marshaller m = plan.makeMarshaller();
				m.putNullPtr();
				m.skipBasicArrayElements(8);
				m.putNullStringPtr(RETURN_JAVA_STRING);
				m.putNullStringPtr(NO_ACTION);
				m.skipBasicArrayElements(1);
				m.putNullPtr();
				m.skipBasicArrayElements(8);
				m.putNullStringPtr(RETURN_JAVA_STRING);
				m.putNullStringPtr(NO_ACTION);
				m.skipBasicArrayElements(1);
				m.skipBasicArrayElements(1);
				assertThrew( // should be at the end of the marshaller
						new Runnable() {
							public void run() {
								m.putChar((byte) 0);
							}
						}, ArrayIndexOutOfBoundsException.class);
				long result = nativecall.invoke(testcall.ptr,
						plan.getSizeDirect(), m);
				assertEquals(0L, result & testcall.returnValueMask.value);
			}
		}
		//
		// SumResource
		//
		{
			TestCall testcall = TestCall.SUM_RESOURCE;
			MarshallPlan plan = testcall.plan;
			for (NativeCall nativecall : VI_NORET_VI_OR_FLOAT) {
				final Marshaller m = plan.makeMarshaller();
				m.putINTRESOURCE((short) 0);
				m.putNullPtr();
				m.putINTRESOURCE((short) 0);
				assertThrew( // should be at the end of the marshaller
						new Runnable() {
							public void run() {
								m.putChar((byte) 0);
							}
						}, ArrayIndexOutOfBoundsException.class);
				long result = nativecall.invoke(testcall.ptr,
						plan.getSizeDirect(), m);
				assertEquals(0L, result & testcall.returnValueMask.value);
			}
		}
	}

	@Test
	public void testVariableIndirectInString_StrLen() {
		final String[] strings = new String[] { "", "1", "22", "333", "4444",
				"55555", "666666", "7777777", "88888888", "999999999",
				"0000000000" };
		TestCall testcall = TestCall.STRLEN;
		MarshallPlan plan = testcall.plan;
		for (NativeCall nativecall : VI_NORET_VI_OR_FLOAT) {
			for (int k = 0; k <= 10; ++k) {
				Marshaller m = plan.makeMarshaller();
				m.putStringPtr(strings[k], NO_ACTION);
				long result = nativecall.invoke(testcall.ptr,
						plan.getSizeDirect(), m);
				assertEquals((long) k, result & testcall.returnValueMask.value);
			}
		}
	}

	@Test
	public void testVariableIndirectOutBuffer() {
		final int[] sizes = new int[] { 0, 1, 2, 4, "hello world".length(),
				"hello world".length() + 1, 32, 64, 100, 1000 };
		final String hello_world = "hello world\u0000";
		TestCall testcall = TestCall.HELLO_WORLD_OUT_BUFFER;
		MarshallPlan plan = testcall.plan;
		for (NativeCall nativecall : VI_NORET_VI_OR_FLOAT) {
			for (int size : sizes) {
				byte[] src = new byte[size];
				Buffer buffer = new Buffer(src, 0, size); // has a copy of src
				Marshaller m = plan.makeMarshaller();
				m.putStringPtr(buffer, NO_ACTION);
				m.putLong(size);
				nativecall.invoke(testcall.ptr, plan.getSizeDirect(), m);
				int endIndex = hello_world.length() < size ? hello_world
						.length() : size;
				String comparator = hello_world.substring(0, endIndex);
				comparator += new String(new char[size <= endIndex ? 0 : size
						- endIndex]);
				assertEquals(buffer, comparator);
			}
		}
	}

	@Test
	public void testVariableIndirectSumString() {
		final TestCall testcall = TestCall.SUM_STRING;
		final NativeCall nativecall = NativeCall.VARIABLE_INDIRECT_RETURN_INT64;
		final int sizeDirect = testcall.plan.getSizeDirect();
		final long mask = testcall.returnValueMask.value;
		//
		// TEST 1
		//
		{
			final Marshaller m = TestCall.marshall(
					new TestCall.Recursive_StringSum("12345678", null), null);
			assertThrew(new Runnable() {
				public void run() {
					m.putChar((byte) 0);
				}
			}, ArrayIndexOutOfBoundsException.class);
			assertEquals(12345678L,
					nativecall.invoke(testcall.ptr, sizeDirect, m) & mask);
		}
		//
		// TEST 2
		//
		{
			for (int k = 0; k <= 10; ++k) {
				Buffer buffer = new Buffer(new byte[k], 0, k);
				final Marshaller m = TestCall.marshall(
						new TestCall.Recursive_StringSum("987654321", buffer),
						null);
				assertThrew(new Runnable() {
					public void run() {
						m.putChar((byte) 0);
					}
				}, ArrayIndexOutOfBoundsException.class);
				assertEquals(987654321L,
						nativecall.invoke(testcall.ptr, sizeDirect, m) & mask);
				String got = buffer.toString();
				if (-1 < got.indexOf('\u0000'))
					got = got.substring(0, got.indexOf('\u0000'));
				assertEquals("987654321".substring(0, Math.max(k - 1, 0)), got);
			}
		}
		//
		// TEST 3
		//
		{
			final Marshaller m = TestCall.marshall(
					new TestCall.Recursive_StringSum("404", null, -2, 1, -4, 3,
							-6, 5, -8, 7),
					new TestCall.Recursive_StringSum("-200", null, -100, -75,
							-50, -25, 50, -25, 50, -25));
			assertThrew(new Runnable() {
				public void run() {
					m.putChar((byte) 0);
				}
			}, ArrayIndexOutOfBoundsException.class);
			assertEquals(0L, nativecall.invoke(testcall.ptr, sizeDirect, m)
					& mask);
		}
		//
		// TEST 4
		//
		{
			for (int outer = 0; outer <= 32; ++outer) {
				for (int inner = 0; inner <= 32; ++inner) {
					Buffer outerBuffer = new Buffer(new byte[outer], 0, outer);
					Buffer innerBuffer = new Buffer(new byte[inner], 0, inner);
					final Marshaller m = TestCall.marshall(
							new TestCall.Recursive_StringSum("404",
									outerBuffer, -2, 1, -4, 3, -6, 5, -8, 7),
							new TestCall.Recursive_StringSum("-200",
									innerBuffer, -100, -75, -50, -25, 50, -25,
									50, -25));
					assertThrew(new Runnable() {
						public void run() {
							m.putChar((byte) 0);
						}
					}, ArrayIndexOutOfBoundsException.class);
					assertEquals(0L,
							nativecall.invoke(testcall.ptr, sizeDirect, m)
									& mask);
					StringBuilder b = new StringBuilder(Math.max(inner, outer));
					b.append(new char[outer]);
					int outerExtent = Math.max(0,
							Math.min("0".length(), outer - 1));
					b.replace(0, outerExtent, "0".substring(0, outerExtent));
					String outerExpect = b.toString();
					b.delete(0, b.length());
					int innerExtent = Math.max(0,
							Math.min("-400".length(), inner - 1));
					b.append(new char[inner]);
					b.replace(0, innerExtent, "-400".substring(0, innerExtent));
					String innerExpect = b.toString();
					assertEquals(outerBuffer, outerExpect);
					assertEquals(innerBuffer, innerExpect);
				}
			}
		}
		//
		// TEST 5: Note here we are passing the SAME Buffer reference as both
		// the inner and outer buffer. This should not crash the JVM.
		// On the other hand, the marshaller can't promise that both
		// references to the Buffer will translate into the same pointer
		// on the C++ side, so we can't make any assertions about the
		// content of the buffer except that the second character is
		// for sure a 0.
		{
			final int NREPS = 100;
			final Buffer buffer = new Buffer(new byte[] { '1', '2' }, 0, 2);
			for (int k = 0; k < NREPS; ++k) {
				final Marshaller m = TestCall.marshall(
						new TestCall.Recursive_StringSum(Integer.toString(k),
								buffer, k, k, k, k, k, k, k, k),
						new TestCall.Recursive_StringSum(Integer.toString(-k),
								buffer, -k, -k, -k, -k, -k, -k, -k, -k));
				assertEquals(0L, nativecall.invoke(testcall.ptr, sizeDirect, m)
						& mask);
				assertEquals('\u0000', buffer.toString().charAt(1));
			}
		}
	}

	@Test
	public void testVariableIndirectSumResource() {
		final TestCall testcall = TestCall.SUM_RESOURCE;
		final NativeCall nativecall = NativeCall.VARIABLE_INDIRECT_RETURN_INT64;
		final int sizeDirect = testcall.plan.getSizeDirect();
		final long mask = testcall.returnValueMask.value;
		//
		// TEST 1
		//
		{
			final Marshaller m = testcall.plan.makeMarshaller();
			m.putINTRESOURCE((short) 1);
			m.putNullPtr();
			m.putINTRESOURCE((short) 0);
			assertEquals(1L, nativecall.invoke(testcall.ptr, sizeDirect, m)
					& mask);
			m.rewind();
			assertEquals(1, m.getResource());
			assertTrue(m.isPtrNull());
			assertEquals(0, m.getResource());
		}
		//
		// TEST 2
		//
		{
			final Marshaller m = testcall.plan.makeMarshaller();
			m.putINTRESOURCE((short) 10001);
			m.putPtr();
			m.putINTRESOURCE((short) 29999);
			assertEquals(40000L, nativecall.invoke(testcall.ptr, sizeDirect, m)
					& mask);
			m.rewind();
			assertEquals(10001, m.getResource());
			assertFalse(m.isPtrNull());
			assertEquals(40000, m.getResource());
		}
		//
		// TEST 3
		//
		{
			final Marshaller m = testcall.plan.makeMarshaller();
			m.putStringPtr(Integer.toString(0xffff),
					VariableIndirectInstruction.RETURN_RESOURCE);
			m.putPtr();
			m.putINTRESOURCE((short) 1);
			assertEquals(0x10000L, nativecall.invoke(testcall.ptr, sizeDirect, m)
					& mask);
			m.rewind();
			assertEquals(Integer.toString(0xffff), m.getResource());
			assertFalse(m.isPtrNull());
			assertEquals("sum is not an INTRESOURCE", m.getResource());
		}
	}

	@Test
	public void testSwap() {
		// 20130807: The purpose of this test is to catch/test a bug in
		//           Marshaller in which it wasn't advancing the position and
		//           pointer indices in the get[VariableIndirect] calls.
		final TestCall testcall = TestCall.SWAP;
		final NativeCall nativecall = NativeCall.VARIABLE_INDIRECT_RETURN_INT64;
		final int sizeDirect = testcall.plan.getSizeDirect();
		final long mask = testcall.returnValueMask.value;
		{
			final Marshaller m = testcall.plan.makeMarshaller();
			m.putPtr();
			m.putNullStringPtr(RETURN_JAVA_STRING);
			m.putLong(1);
			m.putLong(2);
			assertEquals(0, nativecall.invoke(testcall.ptr, sizeDirect, m) & mask);
			m.rewind();
			assertFalse(m.isPtrNull());
			assertEquals("!=", m.getStringPtr());
			assertEquals(2, m.getLong());
			assertEquals(1, m.getLong());
		}
	}

	@Test
	public void testReturnString_HelloWorld() {
		TestCall testcall = TestCall.HELLO_WORLD_RETURN;
		int sizeDirect = testcall.plan.getSizeDirect();
		boolean[] flag = { false, true };
		Object[] expected = { Boolean.FALSE, "hello world" };
		for (int k = 0; k < 2; ++k) {
			for (NativeCall nativecall : VI_RETVI) {
				final Marshaller m = testcall.plan.makeMarshaller();
				m.putBool(flag[k]);
				m.putNullStringPtr(RETURN_JAVA_STRING);
				nativecall.invoke(testcall.ptr, sizeDirect, m);
				assertThrew(
					new Runnable() { public void run() { m.getBool(); } },
					ArrayIndexOutOfBoundsException.class
				);
				assertThrew(
					new Runnable() { public void run() { m.getStringPtr(); } },
					ArrayIndexOutOfBoundsException.class
				);
				m.rewind();
				assertEquals(flag[k], m.getBool());
				assertEquals(expected[k], m.getStringPtr());
			}
		}
	}

	@Test
	public void testReturnString_PassedIn() {
		TestCall testcall = TestCall.RETURN_STRING;
		int sizeDirect = testcall.plan.getSizeDirect();
		String[] values = { null, "", "1", "22", "thrice is nice!" };
		for (String value : values) {
			for (NativeCall nativecall : VI_RETVI) {
				Marshaller m = testcall.plan.makeMarshaller();
				// Argument
				if (null == value) {
					m.putNullStringPtr(NO_ACTION);
				} else {
					m.putStringPtr(value, NO_ACTION);
				}
				// Return value placeholder
				m.putNullStringPtr(RETURN_JAVA_STRING);
				nativecall.invoke(testcall.ptr, sizeDirect, m);
				m.rewind();
				if (null == value) {
					assertEquals(Boolean.FALSE, m.getStringPtr());
					assertEquals(Boolean.FALSE, m.getStringPtr());
				} else {
					m.getStringPtr();
					assertEquals(value, m.getStringPtr());
				}
			}
		}
	}

	@Test
	public void testReturnString_Ptr() {
		TestCall testcall = TestCall.RETURN_PTR_STRING;
		int sizeDirect = testcall.plan.getSizeDirect();
		String[] values = { null, "", "1", "22", "the third non-null string!" };
		for (String value : values) {
			for (NativeCall nativecall : VI_RETVI) {
				Marshaller m = testcall.plan.makeMarshaller();
				// Arguments
				m.putPtr();
				if (null == value) {
					m.putNullStringPtr(NO_ACTION);
				} else {
					m.putStringPtr(value, NO_ACTION);
				}
				// Return value placeholder
				m.putNullStringPtr(RETURN_JAVA_STRING);
				nativecall.invoke(testcall.ptr, sizeDirect, m);
				m.rewind();
				assertFalse(m.isPtrNull());
				if (null == value) {
					assertEquals(Boolean.FALSE, m.getStringPtr());
					assertEquals(Boolean.FALSE, m.getStringPtr());
				} else {
					m.getStringPtr();
					assertEquals(value, m.getStringPtr());
				}
			}
		}
	}

	@Test
	public void testReturnString_Buffer() {
		TestCall testcall = TestCall.RETURN_STRING_OUT_BUFFER;
		int sizeDirect = testcall.plan.getSizeDirect();
		String[] values = { null, "", "1", "22", "thrice is nice!" };
		int[] bufferSizes = { 0, 1, 2, 4, 1000 };
		for (String value : values) {
			for (int bufferSize : bufferSizes) {
				for (NativeCall nativecall : VI_RETVI) {
					Marshaller m = testcall.plan.makeMarshaller();
					// Arguments
					if (null == value) {
						m.putNullStringPtr(NO_ACTION);
					} else {
						m.putStringPtr(value, NO_ACTION);
					}
					Buffer buffer = new Buffer(bufferSize, "");
					m.putStringPtr(buffer, NO_ACTION);
					m.putLong(bufferSize);
					// Return value placeholder
					m.putNullStringPtr(RETURN_JAVA_STRING);
					nativecall.invoke(testcall.ptr, sizeDirect, m);
					m.rewind();
					if (null == value) {
						assertEquals(Boolean.FALSE, m.getStringPtr());
						assertSame(buffer, m.getStringPtrAlwaysByteArray(buffer));
						assertEquals(Boolean.FALSE, m.getStringPtr());
					} else if (0 == bufferSize) {
						m.getStringPtr();
						assertSame(buffer, m.getStringPtrAlwaysByteArray(buffer));
						assertEquals(Boolean.FALSE, m.getStringPtr());
					} else {
						m.getStringPtr();
						assertSame(buffer, m.getStringPtrAlwaysByteArray(buffer));
						String value2 = value;
						if (bufferSize <= value2.length()) {
							value2 = value2.substring(0, Math.max(0, bufferSize - 1));
						}
						assertEquals(value2, m.getStringPtr());
						assertEquals(buffer.toStringNoZeroes(), value2);
					}
				} // for (nativecall ...)
			} // for (bufferSize ...)
		} // for (value ...)
	}
}
