package suneido.language.jsdi.dll;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static suneido.language.jsdi.MarshallTestUtil.pointerPlan;
import static suneido.language.jsdi.VariableIndirectInstruction.NO_ACTION;
import static suneido.language.jsdi.VariableIndirectInstruction.RETURN_JAVA_STRING;
import static suneido.language.jsdi.dll.ReturnTypeGroup.VOID;
import static suneido.language.jsdi.dll.ReturnTypeGroup._32_BIT;
import static suneido.util.testing.Throwing.assertThrew;

import java.util.ArrayList;

import org.junit.BeforeClass;
import org.junit.Test;

import suneido.language.jsdi.Buffer;
import suneido.language.jsdi.JSDI;
import suneido.language.jsdi.MarshallPlan;
import suneido.language.jsdi.Marshaller;
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

	private static final NativeCall[] DOF_VOID;
	private static final NativeCall[] DOF_NONVOID;
	private static final NativeCall[] DOF_32;
	private static final NativeCall[] DOF_64;
	private static final NativeCall[] IND;
	private static final NativeCall[] VI;
	static {
		ArrayList<NativeCall> dof_void = new ArrayList<NativeCall>();
		ArrayList<NativeCall> dof_nonvoid = new ArrayList<NativeCall>();
		ArrayList<NativeCall> dof_32 = new ArrayList<NativeCall>();
		ArrayList<NativeCall> dof_64 = new ArrayList<NativeCall>();
		ArrayList<NativeCall> ind = new ArrayList<NativeCall>();
		ArrayList<NativeCall> vi = new ArrayList<NativeCall>();
		for (NativeCall nativecall : NativeCall.values()) {
			if (nativecall.isDirectOrFast()) {
				if (VOID == nativecall.getReturnTypeGroup())
					dof_void.add(nativecall);
				else {
					dof_nonvoid.add(nativecall);
					if (_32_BIT == nativecall.getReturnTypeGroup())
						dof_32.add(nativecall);
					else
						dof_64.add(nativecall);
				}
			} else if (CallGroup.INDIRECT == nativecall.getCallGroup()) {
				ind.add(nativecall);
			} else {
				assert CallGroup.VARIABLE_INDIRECT == nativecall.getCallGroup();
				vi.add(nativecall);
			}
		}
		DOF_VOID = dof_void.toArray(new NativeCall[dof_void.size()]);
		DOF_NONVOID = dof_nonvoid.toArray(new NativeCall[dof_nonvoid.size()]);
		DOF_32 = dof_32.toArray(new NativeCall[dof_32.size()]);
		DOF_64 = dof_64.toArray(new NativeCall[dof_64.size()]);
		IND = ind.toArray(new NativeCall[ind.size()]);
		VI = vi.toArray(new NativeCall[vi.size()]);
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
	public void testDirectOnlyReturnV_AllZeroes() {
		// THIS TEST JUST LOOKS TO MAKE SURE NOTHING BLOWS UP -- NO EXCEPTIONS,
		// AND JVM IS STILL RUNNING! The test functions are designed to behave
		// nicely if they get all zeroes for arguments (even if they expect
		// pointers). So as long as we send enough zeroes, all should be well.
		for (TestCall testcall : TestCall.values()) {
			for (NativeCall nativecall : DOF_VOID) {
				Marshaller m = testcall.plan.makeMarshaller();
				nativecall.invoke(testcall.ptr, testcall.plan.getSizeDirect(),
						m);
			}
		}
	}

	@Test
	public void testDirectOnlyReturnV_FastCalling() {
		for (TestCall testcall : TestCall.values()) {
			switch (PrimitiveSize.minWholeWords(testcall.plan.getSizeDirect())) {
			case 0:
				NativeCall.callReturnV(testcall.ptr);
				break;
			case 1:
				NativeCall.callLReturnV(testcall.ptr, 0);
				break;
			case 2:
				NativeCall.callLLReturnV(testcall.ptr, 0, 0);
				break;
			case 3:
				NativeCall.callLLLReturnV(testcall.ptr, 0, 0, 0);
				break;
			}
		}
	}

	@Test
	public void testDirectOnlyReturn32bit_AllZeroes() {
		// THE TEST FUNCTIONS ARE DESIGNED TO BEHAVE NICELY IF THEY GET ALL
		// ZEROS for arguments (even if they expect pointers). So as long as we
		// send the proper amount of zeroes, all should be well.
		for (TestCall testcall : TestCall.values()) {
			for (NativeCall nativecall : DOF_32) {
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
	public void testDirectOnlyReturn32bit_FastCalling() {
		// little-endian
		assertEquals(27, NativeCall.callLReturnL(TestCall.CHAR.ptr, 27));
		assertEquals(0x2728,
				NativeCall.callLReturnL(TestCall.SHORT.ptr, 0x2728));
		assertEquals(0x18120207,
				NativeCall.callLReturnL(TestCall.LONG.ptr, 0x18120207));
		// little-endian, and pushed on the stack in reverse order
		assertEquals(0x19820207, NativeCall.callLLReturnL(TestCall.INT64.ptr,
				0x19820207, 0xdeadbeef));
		// sum functions
		assertEquals(3,
				NativeCall.callLLReturnL(TestCall.SUM_TWO_LONGS.ptr, 1, 2));
		assertEquals(-1, NativeCall.callLLReturnL(TestCall.SUM_TWO_LONGS.ptr,
				Integer.MAX_VALUE, Integer.MIN_VALUE));
		assertEquals(6, NativeCall.callLLLReturnL(TestCall.SUM_THREE_LONGS.ptr,
				3, 2, 1));
		assertEquals(0, NativeCall.callLLLReturnL(TestCall.SUM_THREE_LONGS.ptr,
				1, Integer.MIN_VALUE, Integer.MAX_VALUE));
	}

	@Test
	public void testDirectOnly32bit() {
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
				for (NativeCall nativecall : DOF_NONVOID) {
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
			for (NativeCall nativecall : DOF_NONVOID) {
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
			for (NativeCall nativecall : DOF_NONVOID) {
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
					(int) (NativeCall.DIRECT_ONLY_RETURN_32_BIT.invoke(
							TestCall.SUM_FOUR_LONGS.ptr,
							TestCall.SUM_FOUR_LONGS.plan.getSizeDirect(), m) & TestCall.SUM_FOUR_LONGS.returnValueMask.value));
		}
		{
			Marshaller m = TestCall.SUM_PACKED_CHAR_CHAR_SHORT_LONG.plan.makeMarshaller();
			m.putChar(Byte.MIN_VALUE);
			m.putChar(Byte.MAX_VALUE);
			m.putShort(Short.MIN_VALUE);
			m.putLong(Integer.MAX_VALUE);
			assertEquals(
					(int) Byte.MIN_VALUE + (int) Byte.MAX_VALUE
							+ (int) Short.MIN_VALUE + Integer.MAX_VALUE,
					(int) (NativeCall.DIRECT_ONLY_RETURN_32_BIT.invoke(
							TestCall.SUM_PACKED_CHAR_CHAR_SHORT_LONG.ptr,
							TestCall.SUM_PACKED_CHAR_CHAR_SHORT_LONG.plan
									.getSizeDirect(), m) & TestCall.SUM_PACKED_CHAR_CHAR_SHORT_LONG.returnValueMask.value));
		}
	}

	@Test
	public void testDirectOnlyReturn64bit_AllZeroes() {
		// THE TEST FUNCTIONS ARE DESIGNED TO BEHAVE NICELY IF THEY GET ALL
		// ZEROS for arguments (even if they expect pointers). So as long as we
		// send the proper amount of zeroes, all should be well.
		for (TestCall testcall : TestCall.values()) {
			for (NativeCall nativecall : DOF_64) {
				Marshaller m = testcall.plan.makeMarshaller();
				long result = nativecall.invoke(testcall.ptr,
						testcall.plan.getSizeDirect(), m)
						& testcall.returnValueMask.value;
				assertEquals(0L, result);
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
				for (NativeCall nativecall : IND) {
					nativecall.invoke(testcall.ptr, plan.getSizeDirect(), m);
				}
			}
		}
	}

	@Test
	public void testIndirectOnlyReceiveValue() {
		MarshallPlan plan = pointerPlan(PrimitiveSize.POINTER);
		{
			for (NativeCall nativecall : IND) {
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
			for (NativeCall nativecall : IND) {
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
		final long longValue = NativeCall.INDIRECT_RETURN_64_BIT.invoke(
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
			for (NativeCall nativecall : VI) {
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
			for (NativeCall nativecall : VI) {
				Marshaller m = plan.makeMarshaller();
				m.putNullStringPtr(NO_ACTION);
				m.putLong(0);
				nativecall.invoke(testcall.ptr, plan.getSizeDirect(), m);
			}
		}
		//
		// StringSum
		//
		{
			TestCall testcall = TestCall.SUM_STRING;
			MarshallPlan plan = testcall.plan;
			for (NativeCall nativecall : VI) {
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
					new Runnable() { public void run() { m.putChar((byte)0); } },
					ArrayIndexOutOfBoundsException.class
				);
				long result = nativecall.invoke(testcall.ptr, plan.getSizeDirect(), m);
				assertEquals(0L, result & testcall.returnValueMask.value);
			}
		}
	}

	@Test
	public void testVariableIndirectInString_StrLen()
	{
		final String[] strings = new String[] { "", "1", "22", "333", "4444",
				"55555", "666666", "7777777", "88888888", "999999999",
				"0000000000" };
		TestCall testcall = TestCall.STRLEN;
		MarshallPlan plan = testcall.plan;
		for (NativeCall nativecall : VI) {
			if (nativecall == NativeCall.VARIABLE_INDIRECT_RETURN_V) continue;
			for (int k = 0; k <= 10; ++k) {
				Marshaller m = plan.makeMarshaller();
				m.putStringPtr(strings[k], NO_ACTION);
				long result = nativecall.invoke(testcall.ptr,
						plan.getSizeDirect(), m);
				assertEquals((long)k, result & testcall.returnValueMask.value);
			}
		}
	}

	@Test
	public void testVariableIndirectOutBuffer()
	{
		final int[] sizes = new int[] { 0, 1, 2, 4, "hello world".length(),
				"hello world".length() + 1, 32, 64, 100, 1000 };
		final String hello_world = "hello world\u0000";
		TestCall testcall = TestCall.HELLO_WORLD_OUT_BUFFER;
		MarshallPlan plan = testcall.plan;
		for (NativeCall nativecall : VI) {
			for (int size : sizes) {
				byte[] src = new byte[size];
				Buffer buffer = new Buffer(src, 0, size); // has a copy of src
				Marshaller m = plan.makeMarshaller();
				m.putStringPtr(buffer, NO_ACTION);
				m.putLong(size);
				nativecall.invoke(testcall.ptr, plan.getSizeDirect(), m);
				int endIndex = hello_world.length() < size
					? hello_world.length()
					: size;
				String comparator = hello_world.substring(0, endIndex);
				comparator += new String(new char[size <= endIndex ? 0 : size
						- endIndex]);
				assertEquals(buffer, comparator);
			}
		}
	}

	@Test
	public void testVariableIndirectOutStringSum()
	{
		final TestCall testcall = TestCall.SUM_STRING;
		final NativeCall nativecall = NativeCall.VARIABLE_INDIRECT_RETURN_32_BIT;
		final int sizeDirect = testcall.plan.getSizeDirect(); 
		final long mask = testcall.returnValueMask.value;
		//
		// TEST 1 
		//
		{
			final Marshaller m = TestCall.marshall(
				new TestCall.Recursive_StringSum("12345678", null), null);
			assertThrew(
				new Runnable() { public void run() { m.putChar((byte)0); } },
				ArrayIndexOutOfBoundsException.class
			);
			assertEquals(12345678L, nativecall.invoke(testcall.ptr, sizeDirect, m) & mask);
		}
		//
		// TEST 2
		//
		{
			for (int k = 0; k <= 10; ++k)
			{
				Buffer buffer = new Buffer(new byte[k], 0, k);
				final Marshaller m = TestCall.marshall(
						new TestCall.Recursive_StringSum("987654321", buffer), null);
				assertThrew(
						new Runnable() { public void run() { m.putChar((byte)0); } },
						ArrayIndexOutOfBoundsException.class
					);
				assertEquals(987654321L, nativecall.invoke(testcall.ptr, sizeDirect, m) & mask);
				String got = buffer.toString();
				if (-1 < got.indexOf('\u0000')) got = got.substring(0, got.indexOf('\u0000'));
				assertEquals("987654321".substring(0, Math.max(k - 1, 0)), got);
			}
		}
		//
		// TEST 3
		//
		{
			final Marshaller m = TestCall.marshall(
				new TestCall.Recursive_StringSum("404", null, -2, 1, -4, 3, -6, 5, -8, 7),
				new TestCall.Recursive_StringSum("-200", null, -100, -75, -50, -25, 50, -25, 50, -25)
			);
			assertThrew(
				new Runnable() { public void run() { m.putChar((byte)0); } },
				ArrayIndexOutOfBoundsException.class
			);
			assertEquals(0L, nativecall.invoke(testcall.ptr, sizeDirect, m) & mask);
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
						new TestCall.Recursive_StringSum("404", outerBuffer, -2, 1, -4, 3, -6, 5, -8, 7),
						new TestCall.Recursive_StringSum("-200", innerBuffer, -100, -75, -50, -25, 50, -25, 50, -25)
					);
					assertThrew(
						new Runnable() { public void run() { m.putChar((byte)0); } },
						ArrayIndexOutOfBoundsException.class
					);
					assertEquals(0L, nativecall.invoke(testcall.ptr, sizeDirect, m) & mask);
					StringBuilder b = new StringBuilder(Math.max(inner, outer));
					b.append(new char[outer]);
					int outerExtent = Math.max(0, Math.min("0".length(), outer - 1));
					b.replace(0, outerExtent, "0".substring(0, outerExtent));
					String outerExpect = b.toString();
					b.delete(0, b.length());
					int innerExtent = Math.max(0, Math.min("-400".length(), inner - 1));
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
		//         the inner and outer buffer. This should not crash the JVM.
		//         On the other hand, the marshaller can't promise that both
		//         references to the Buffer will translate into the same pointer
		//         on the C++ side, so we can't make any assertions about the
		//         content of the buffer except that the second character is
		//         for sure a 0.
		{
			final int NREPS = 100;
			final Buffer buffer = new Buffer(new byte[] { '1', '2' }, 0, 2);
			for (int k = 0; k < NREPS; ++k)
			{
				final Marshaller m = TestCall.marshall(
					new TestCall.Recursive_StringSum(Integer.toString(k), buffer, k, k, k, k, k, k, k, k),
					new TestCall.Recursive_StringSum(Integer.toString(-k), buffer, -k, -k, -k, -k, -k, -k, -k, -k)
				);
				assertEquals(0L, nativecall.invoke(testcall.ptr, sizeDirect, m) & mask);
				assertEquals('\u0000', buffer.toString().charAt(1));
			}
		}
	}
}
