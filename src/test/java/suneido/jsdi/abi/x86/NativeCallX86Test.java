/* Copyright 2013 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido.jsdi.abi.x86;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static suneido.jsdi.abi.x86.MarshallTestUtilX86.pointerPlan;
import static suneido.jsdi.marshall.MarshallPlan.StorageCategory;
import static suneido.jsdi.marshall.VariableIndirectInstruction.NO_ACTION;
import static suneido.jsdi.marshall.VariableIndirectInstruction.RETURN_JAVA_STRING;
import static suneido.util.testing.Throwing.assertThrew;

import java.util.ArrayList;

import org.junit.BeforeClass;
import org.junit.Test;

import suneido.jsdi.Buffer;
import suneido.jsdi.DllInterface;
import suneido.jsdi.JSDI;
import suneido.jsdi.TestCall;
import suneido.jsdi.abi.x86.MarshallPlanX86;
import suneido.jsdi.abi.x86.MarshallerX86;
import suneido.jsdi.abi.x86.NativeCallX86;
import suneido.jsdi.marshall.MarshallTestUtil;
import suneido.jsdi.marshall.Marshaller;
import suneido.jsdi.marshall.PrimitiveSize;
import suneido.jsdi.marshall.ReturnTypeGroup;
import suneido.jsdi.marshall.VariableIndirectInstruction;
import suneido.util.testing.Assumption;

/**
 * Test for the native calling mechanism using the <code>Test*</code> functions
 * exported from the JSDI DLL.
 *
 * @author Victor Schappert
 * @since 20130723
 * @see NativeCallX86
 */
@DllInterface
public class NativeCallX86Test {

	@BeforeClass
	public static void setUpBeforeClass() throws Exception {
		Assumption.jvmIs32BitOnWindows();
		// Trigger load of JSDI DLL. Otherwise, LoadLibrary() might fail if
		// JSDI DLL isn't in a Windows standard DLL search path, and you'll get
		// an UnsatisfiedLinkError.
		JSDI.getInstance();
	}

	private static final NativeCallX86[] DOF_NORET_VI_OR_FLOAT;
	private static final NativeCallX86[] IND_NORET_VI_OR_FLOAT;
	private static final NativeCallX86[] VI_NORET_VI_OR_FLOAT;
	private static final NativeCallX86[] VI_RETVI;
	static {
		ArrayList<NativeCallX86> dof_noret_vi_or_float = new ArrayList<>();
		ArrayList<NativeCallX86> ind_noret_vi_or_float = new ArrayList<>();
		ArrayList<NativeCallX86> vi_noret_vi_or_float = new ArrayList<>();
		ArrayList<NativeCallX86> vi_retvi = new ArrayList<>();
		for (NativeCallX86 nativecall : NativeCallX86.values()) {
			if (ReturnTypeGroup.DOUBLE == nativecall.returnTypeGroup) {
				continue;
			} else if (ReturnTypeGroup.VARIABLE_INDIRECT == nativecall.returnTypeGroup) {
				if (StorageCategory.VARIABLE_INDIRECT == nativecall.storageCategory) {
					vi_retvi.add(nativecall);
				} else {
					continue;
				}
			} else {
				if (StorageCategory.DIRECT == nativecall.storageCategory) {
					dof_noret_vi_or_float.add(nativecall);
				} else if (StorageCategory.INDIRECT == nativecall.storageCategory) {
					ind_noret_vi_or_float.add(nativecall);
				} else {
					assert StorageCategory.VARIABLE_INDIRECT == nativecall.storageCategory;
					vi_noret_vi_or_float.add(nativecall);
				}
			}
		}
		DOF_NORET_VI_OR_FLOAT = dof_noret_vi_or_float.toArray(new NativeCallX86[dof_noret_vi_or_float.size()]);
		IND_NORET_VI_OR_FLOAT = ind_noret_vi_or_float.toArray(new NativeCallX86[ind_noret_vi_or_float.size()]);
		VI_NORET_VI_OR_FLOAT = vi_noret_vi_or_float.toArray(new NativeCallX86[vi_noret_vi_or_float.size()]);
		VI_RETVI = vi_retvi.toArray(new NativeCallX86[vi_retvi.size()]);
	}

	private static String toStringNoZeroes(Buffer buffer) {
		final int N = buffer.length();
		for (int k = 0; k < N; ++k) {
			if ((byte) 0 == buffer.getInternalData()[k])
				return new Buffer(buffer.getInternalData(), 0, k).toString();
		}
		return buffer.toString();
	}

	private static Marshaller makeMarshaller(TestCall testcall) {
		final MarshallPlanX86 plan = (MarshallPlanX86)testcall.plan;
		return plan.makeMarshaller();
	}

	@Test
	public void testDirect_AllZeroes() {
		// THE TEST FUNCTIONS ARE DESIGNED TO BEHAVE NICELY IF THEY GET ALL
		// ZEROS for arguments (even if they expect pointers). So as long as we
		// send the proper amount of zeroes, all should be well.
		for (TestCall testcall : TestCall.values()) {
			if (Mask.DOUBLE == testcall.returnValueMask) continue;
			for (NativeCallX86 nativecall : DOF_NORET_VI_OR_FLOAT) {
				{
					if (TestCall.DIVIDE_TWO_INT32 == testcall) {
						continue; // Will raise a divide-by-zero exception
					}
					Marshaller m = makeMarshaller(testcall);
					long result = nativecall.invoke(testcall.ptr,
							testcall.plan.getSizeDirect(), m)
							& testcall.returnValueMask.value;
					assertEquals(0L, result);
				}
			}
		}
	}

	@Test
	public void testDirect_Misc() {
		// functions that return the same value
		{
			final TestCall[] f = new TestCall[] { TestCall.INT8,
					TestCall.INT16, TestCall.INT32, TestCall.INT64 };
			final long[] x = new long[] { 27L, 0x2728L, 0x18120207L,
					0xdeadbeef19820207L };
			for (int k = 0; k < f.length; ++k) {
				Marshaller m = makeMarshaller(f[k]);
				if (PrimitiveSize.INT32 == f[k].plan.getSizeDirect())
					m.putInt32((int) x[k]);
				else if (PrimitiveSize.INT64 == f[k].plan.getSizeDirect())
					m.putInt64(x[k]);
				else
					throw new RuntimeException("This test is busted");
				for (NativeCallX86 nativecall : DOF_NORET_VI_OR_FLOAT) {
					assertEquals(
							x[k],
							nativecall.invoke(f[k].ptr,
									f[k].plan.getSizeDirect(), m)
									& f[k].returnValueMask.value);
				}
			}
		}
		// sum functions
		{
			Marshaller m = makeMarshaller(TestCall.SUM_TWO_INT32);
			m.putInt32(1);
			m.putInt32(2);
			for (NativeCallX86 nativecall : DOF_NORET_VI_OR_FLOAT) {
				assertEquals(
						3,
						nativecall.invoke(TestCall.SUM_TWO_INT32.ptr,
								TestCall.SUM_TWO_INT32.plan.getSizeDirect(), m)
								& TestCall.SUM_TWO_INT32.returnValueMask.value);
			}
		}
		{
			Marshaller m = makeMarshaller(TestCall.SUM_THREE_INT32);
			m.putInt32(3);
			m.putInt32(2);
			m.putInt32(1);
			for (NativeCallX86 nativecall : DOF_NORET_VI_OR_FLOAT) {
				assertEquals(
						6,
						nativecall.invoke(TestCall.SUM_THREE_INT32.ptr,
								TestCall.SUM_THREE_INT32.plan.getSizeDirect(),
								m)
								& TestCall.SUM_THREE_INT32.returnValueMask.value);
			}
		}
		{
			Marshaller m = makeMarshaller(TestCall.SUM_FOUR_INT32);
			m.putInt32(-100);
			m.putInt32(99);
			m.putInt32(-200);
			m.putInt32(199);
			assertEquals(
					-2,
					(int) (NativeCallX86.DIRECT_RETURN_INT64.invoke(
							TestCall.SUM_FOUR_INT32.ptr,
							TestCall.SUM_FOUR_INT32.plan.getSizeDirect(), m) & TestCall.SUM_FOUR_INT32.returnValueMask.value));
		}
		{
			Marshaller m = makeMarshaller(TestCall.SUM_PACKED_INT8_INT8_INT16_INT32);
			m.putInt8(Byte.MIN_VALUE);
			m.putInt8(Byte.MAX_VALUE);
			m.putInt16(Short.MIN_VALUE);
			m.putInt32(Integer.MAX_VALUE);
			assertEquals(
					Byte.MIN_VALUE + Byte.MAX_VALUE
							+ Short.MIN_VALUE + Integer.MAX_VALUE,
					(int) (NativeCallX86.DIRECT_RETURN_INT64.invoke(
							TestCall.SUM_PACKED_INT8_INT8_INT16_INT32.ptr,
							TestCall.SUM_PACKED_INT8_INT8_INT16_INT32.plan
									.getSizeDirect(), m) & TestCall.SUM_PACKED_INT8_INT8_INT16_INT32.returnValueMask.value));
		}
	}

	@Test
	public void testDirect_ReturnFloat() {
		// Functions which return 1.0
		final NativeCallX86 n = NativeCallX86.DIRECT_RETURN_DOUBLE;
		{
			final TestCall[] f = { TestCall.RETURN1_0FLOAT,
					TestCall.RETURN1_0DOUBLE };
			for (TestCall testcall : f) {
				Marshaller m = makeMarshaller(testcall);
				assertEquals(1.0, Double.longBitsToDouble(n.invoke(
						testcall.ptr, testcall.plan.getSizeDirect(), m)), 0.0);
			}
		}
		// Functions which return the input value
		{
			{
				TestCall testcall = TestCall.FLOAT;
				Marshaller m = makeMarshaller(testcall);
				m.putFloat(33.5f);
				assertEquals(33.5, Double.longBitsToDouble(n.invoke(
						testcall.ptr, testcall.plan.getSizeDirect(), m)), 0.0);
			}
			{
				TestCall testcall = TestCall.DOUBLE;
				Marshaller m = makeMarshaller(testcall);
				m.putDouble(-3333333.5f);
				assertEquals(-3333333.5, Double.longBitsToDouble(n.invoke(
						testcall.ptr, testcall.plan.getSizeDirect(), m)), 0.0);
			}
		}
		// Sum functions
		{
			{
				TestCall testcall = TestCall.SUM_TWO_FLOATS;
				Marshaller m = makeMarshaller(testcall);
				m.putFloat(.75f);
				m.putFloat(-.50f);
				assertEquals(.25, Double.longBitsToDouble(n.invoke(
						testcall.ptr, testcall.plan.getSizeDirect(), m)), 0.0);
			}
			{
				TestCall testcall = TestCall.SUM_TWO_DOUBLES;
				Marshaller m = makeMarshaller(testcall);
				m.putDouble(.5);
				m.putDouble(-.25);
				assertEquals(.25, Double.longBitsToDouble(n.invoke(
						testcall.ptr, testcall.plan.getSizeDirect(), m)), 0.0);
			}
		}
	}

	@Test
	public void testSignedIntAsPointer() {
		// This test asserts that we can send 32-bit unsigned pointer values to
		// the native side packed into 32-bit Java ints.
		TestCall testcall = TestCall.REMOVE_SIGN_FROM_INT32;
		for (NativeCallX86 nativecall : DOF_NORET_VI_OR_FLOAT) {
			Marshaller m = makeMarshaller(testcall);
			int x = 0xffffffff;
			assertTrue(x < 0);
			m.putInt32(x);
			long y = nativecall.invoke(testcall.ptr, testcall.plan.getSizeDirect(), m);
			assertTrue(0 < y);
			assertEquals(0xffffffffL, y);
		}
	}

	@Test
	public void testIndirectOnlyNullPointers() {
		MarshallPlanX86 plan = pointerPlan(PrimitiveSize.POINTER);
		{
			Marshaller m = plan.makeMarshaller();
			m.putNullPtr();
			for (TestCall testcall : new TestCall[] {
					TestCall.HELLO_WORLD_OUT_PARAM, TestCall.NULL_PTR_OUT_PARAM }) {
				for (NativeCallX86 nativecall : IND_NORET_VI_OR_FLOAT) {
					nativecall.invoke(testcall.ptr, plan.getSizeDirect(), m);
				}
			}
		}
	}

	@Test
	public void testIndirectOnlyReceiveValue() {
		MarshallPlanX86 plan = pointerPlan(PrimitiveSize.POINTER);
		{
			for (NativeCallX86 nativecall : IND_NORET_VI_OR_FLOAT) {
				Marshaller m = plan.makeMarshaller();
				m.putPtr();
				nativecall.invoke(TestCall.HELLO_WORLD_OUT_PARAM.ptr,
						plan.getSizeDirect(), m);
				m.rewind();
				assertFalse(m.isPtrNull());
				// The function called should have returned a non-zero value.
				assertFalse(0 == m.getInt32());
			}
		}
		{
			for (NativeCallX86 nativecall : IND_NORET_VI_OR_FLOAT) {
				Marshaller m = plan.makeMarshaller();
				m.putPtr();
				m.putInt32(100); // This will be replaced by 0
				nativecall.invoke(TestCall.NULL_PTR_OUT_PARAM.ptr,
						plan.getSizeDirect(), m);
				m.rewind();
				assertFalse(m.isPtrNull());
				// The function called should have returned a non-zero value.
				assertEquals(0, m.getInt32());
			}
		}
	}

	@Test
	public void testIndirectOnlySendValue() {
		TestCall testcall = TestCall.RETURN_PTR_PTR_PTR_DOUBLE;
		Marshaller m = makeMarshaller(testcall);
		final double doubleValue = 100.0 + 1E-14;
		m.putPtr();
		m.putPtr();
		m.putPtr();
		m.putDouble(doubleValue);
		final long longValue = NativeCallX86.INDIRECT_RETURN_INT64.invoke(
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
			for (NativeCallX86 nativecall : VI_NORET_VI_OR_FLOAT) {
				Marshaller m = makeMarshaller(testcall);
				m.putNullStringPtr(NO_ACTION);
				nativecall.invoke(testcall.ptr, testcall.plan.getSizeDirect(), m);
			}
		}
		//
		// HelloWorldOutBuffer
		//
		{
			TestCall testcall = TestCall.HELLO_WORLD_OUT_BUFFER;
			for (NativeCallX86 nativecall : VI_NORET_VI_OR_FLOAT) {
				Marshaller m = makeMarshaller(testcall);
				m.putNullStringPtr(NO_ACTION);
				m.putInt32(0);
				nativecall.invoke(testcall.ptr, testcall.plan.getSizeDirect(), m);
			}
		}
		//
		// SumString
		//
		{
			TestCall testcall = TestCall.SUM_STRING;
			for (NativeCallX86 nativecall : VI_NORET_VI_OR_FLOAT) {
				final Marshaller m = makeMarshaller(testcall);
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
						() -> { m.putInt8((byte) 0); },
						ArrayIndexOutOfBoundsException.class);
				long result = nativecall.invoke(testcall.ptr,
						testcall.plan.getSizeDirect(), m);
				assertEquals(0L, result & testcall.returnValueMask.value);
			}
		}
		//
		// SumResource
		//
		{
			TestCall testcall = TestCall.SUM_RESOURCE;
			for (NativeCallX86 nativecall : VI_NORET_VI_OR_FLOAT) {
				final Marshaller m = makeMarshaller(testcall);
				m.putINTRESOURCE((short) 0);
				m.putNullPtr();
				m.putINTRESOURCE((short) 0);
				assertThrew( // should be at the end of the marshaller
						() -> { m.putInt8((byte) 0); },
						ArrayIndexOutOfBoundsException.class);
				long result = nativecall.invoke(testcall.ptr,
						testcall.plan.getSizeDirect(), m);
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
		for (NativeCallX86 nativecall : VI_NORET_VI_OR_FLOAT) {
			for (int k = 0; k <= 10; ++k) {
				Marshaller m = makeMarshaller(testcall);
				m.putStringPtr(strings[k], NO_ACTION);
				long result = nativecall.invoke(testcall.ptr,
						testcall.plan.getSizeDirect(), m);
				assertEquals(k, result & testcall.returnValueMask.value);
			}
		}
	}

	@Test
	public void testVariableIndirectOutBuffer() {
		final int[] sizes = new int[] { 0, 1, 2, 4, "hello world".length(),
				"hello world".length() + 1, 32, 64, 100, 1000 };
		final String hello_world = "hello world\u0000";
		TestCall testcall = TestCall.HELLO_WORLD_OUT_BUFFER;
		for (NativeCallX86 nativecall : VI_NORET_VI_OR_FLOAT) {
			for (int size : sizes) {
				byte[] src = new byte[size];
				Buffer buffer = new Buffer(src, 0, size); // has a copy of src
				Marshaller m = makeMarshaller(testcall);
				m.putStringPtr(buffer, NO_ACTION);
				m.putInt32(size);
				nativecall.invoke(testcall.ptr, testcall.plan.getSizeDirect(), m);
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
		final NativeCallX86 nativecall = NativeCallX86.VARIABLE_INDIRECT_RETURN_INT64;
		final int sizeDirect = testcall.plan.getSizeDirect();
		final long mask = testcall.returnValueMask.value;
		//
		// TEST 1
		//
		{
			final MarshallerX86 m = (MarshallerX86)TestCall.marshall(
					new MarshallTestUtil.Recursive_StringSum("12345678", null), null);
			assertThrew(() -> { m.putInt8((byte) 0); },
					ArrayIndexOutOfBoundsException.class);
			assertEquals(12345678L,
					nativecall.invoke(testcall.ptr, sizeDirect, m) & mask);
		}
		//
		// TEST 2
		//
		{
			for (int k = 0; k <= 10; ++k) {
				Buffer buffer = new Buffer(new byte[k], 0, k);
				final MarshallerX86 m = (MarshallerX86)TestCall.marshall(
						new MarshallTestUtil.Recursive_StringSum("987654321", buffer),
						null);
				assertThrew(() -> { m.putInt8((byte) 0); },
						ArrayIndexOutOfBoundsException.class);
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
			final MarshallerX86 m = (MarshallerX86)TestCall.marshall(
					new MarshallTestUtil.Recursive_StringSum("404", null, -2, 1, -4, 3,
							-6, 5, -8, 7),
					new MarshallTestUtil.Recursive_StringSum("-200", null, -100, -75,
							-50, -25, 50, -25, 50, -25));
			assertThrew(() -> { m.putInt8((byte) 0); },
					ArrayIndexOutOfBoundsException.class);
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
					final MarshallerX86 m = (MarshallerX86)TestCall.marshall(
							new MarshallTestUtil.Recursive_StringSum("404",
									outerBuffer, -2, 1, -4, 3, -6, 5, -8, 7),
							new MarshallTestUtil.Recursive_StringSum("-200",
									innerBuffer, -100, -75, -50, -25, 50, -25,
									50, -25));
					assertThrew(() -> { m.putInt8((byte) 0); },
							ArrayIndexOutOfBoundsException.class);
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
				final MarshallerX86 m = (MarshallerX86)TestCall.marshall(
						new MarshallTestUtil.Recursive_StringSum(Integer.toString(k),
								buffer, k, k, k, k, k, k, k, k),
						new MarshallTestUtil.Recursive_StringSum(Integer.toString(-k),
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
		final NativeCallX86 nativecall = NativeCallX86.VARIABLE_INDIRECT_RETURN_INT64;
		final int sizeDirect = testcall.plan.getSizeDirect();
		final long mask = testcall.returnValueMask.value;
		//
		// TEST 1
		//
		{
			final Marshaller m = makeMarshaller(testcall);
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
			final Marshaller m = makeMarshaller(testcall);
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
			final Marshaller m = makeMarshaller(testcall);
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
		final NativeCallX86 nativecall = NativeCallX86.VARIABLE_INDIRECT_RETURN_INT64;
		final int sizeDirect = testcall.plan.getSizeDirect();
		final long mask = testcall.returnValueMask.value;
		{
			final Marshaller m = makeMarshaller(testcall);
			m.putPtr();
			m.putNullStringPtr(RETURN_JAVA_STRING);
			m.putInt32(1);
			m.putInt32(2);
			assertEquals(0, nativecall.invoke(testcall.ptr, sizeDirect, m) & mask);
			m.rewind();
			assertFalse(m.isPtrNull());
			assertEquals("!=", m.getStringPtr());
			assertEquals(2, m.getInt32());
			assertEquals(1, m.getInt32());
		}
	}

	@Test
	public void testReturnString_HelloWorld() {
		TestCall testcall = TestCall.HELLO_WORLD_RETURN;
		int sizeDirect = testcall.plan.getSizeDirect();
		boolean[] flag = { false, true };
		Object[] expected = { Boolean.FALSE, "hello world" };
		for (int k = 0; k < 2; ++k) {
			for (NativeCallX86 nativecall : VI_RETVI) {
				final Marshaller m = makeMarshaller(testcall);
				m.putBool(flag[k]);
				m.putNullStringPtr(RETURN_JAVA_STRING);
				nativecall.invoke(testcall.ptr, sizeDirect, m);
				assertThrew(m::getBool,
					ArrayIndexOutOfBoundsException.class
				);
				assertThrew(m::getStringPtr,
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
			for (NativeCallX86 nativecall : VI_RETVI) {
				Marshaller m = makeMarshaller(testcall);
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
			for (NativeCallX86 nativecall : VI_RETVI) {
				Marshaller m = makeMarshaller(testcall);
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
				for (NativeCallX86 nativecall : VI_RETVI) {
					Marshaller m = makeMarshaller(testcall);
					// Arguments
					if (null == value) {
						m.putNullStringPtr(NO_ACTION);
					} else {
						m.putStringPtr(value, NO_ACTION);
					}
					Buffer buffer = new Buffer(bufferSize, "");
					m.putStringPtr(buffer, NO_ACTION);
					m.putInt32(bufferSize);
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
						assertEquals(toStringNoZeroes(buffer), value2);
					}
				} // for (nativecall ...)
			} // for (bufferSize ...)
		} // for (value ...)
	}
}
