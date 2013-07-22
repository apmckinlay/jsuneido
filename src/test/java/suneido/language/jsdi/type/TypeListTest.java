package suneido.language.jsdi.type;

import static org.junit.Assert.assertArrayEquals;
import static suneido.language.jsdi.type.BasicType.*;

import java.util.Arrays;

import org.junit.BeforeClass;
import org.junit.Test;

import suneido.SuContainer;
import suneido.language.ContextLayered;
import suneido.language.Numbers;
import suneido.language.jsdi.Buffer;
import suneido.language.jsdi.Marshaller;
import suneido.language.jsdi.StorageType;
import suneido.util.testing.Assumption;

public class TypeListTest {

	@BeforeClass
	public static void setUpBeforeClass() throws Exception {
		Assumption.jvmIs32BitOnWindows();
		CONTEXT = new SimpleContext(NAMED_VALUES);
	}

	private static ContextLayered CONTEXT;
	private static final String[] NAMED_VALUES = {
		"CIRCLE", "struct { double x; double y; double r; }"
	};

	private static TypeList makeParams(Object... tuples) {
		final int N = tuples.length;
		assert 0 == N % 2;
		final TypeList.Args args = new TypeList.Args("params", N / 2);
		int k = 0;
		while (k < N) {
			args.add((String)tuples[k++], (Type)tuples[k++]); 
		}
		return new TypeList(args);
	}

	private static Type bv(BasicType basicType) {
		return new BasicValue(basicType);
	}

	private static Type ba2(BasicType basicType) {
		return new BasicArray(new BasicValue(basicType), 2);
	}

	private static SuContainer ca(Object... elements) {
		return new SuContainer(Arrays.asList(elements));
	}

	private static Proxy proxy(String typeName, StorageType storageType,
			int numElems) {
		return new Proxy(CONTEXT, CONTEXT.slotForName(typeName), storageType,
				numElems);
	}

	private static SuContainer CIRCLE(double x, double y, double r) {
		return SuContainer.fromKVPairs("x", Numbers.toBigDecimal(x), "y",
				Numbers.toBigDecimal(y), "r", Numbers.toBigDecimal(r));
	}

	private static SuContainer CIRCLE_Radius(double r) {
		return SuContainer.fromKVPairs("r", Numbers.toBigDecimal(r));
	}

	//
	// Test marshalling 
	//

	@Test
	public void marshallParamsEmpty() {
		TypeList tl = makeParams();
		Object[] args = new Object[0];
		Marshaller m = tl.getMarshallPlan().makeMarshaller();
		tl.marshallInParams(m, args);
		m.rewind();
		tl.marshallOutParams(m, args);
	}

	@Test
	public void marshallParamsBasicValue() {
		TypeList tl = makeParams("bool_", bv(BOOL), "char_", bv(CHAR),
				"short_", bv(SHORT), "long_", bv(LONG), "int64_", bv(INT64),
				"float_", bv(FLOAT), "double_", bv(DOUBLE), "handle_",
				bv(HANDLE), "gdiobj_", bv(GDIOBJ));
		Object[] args1 = new Object[] { Boolean.TRUE, -99, 9999, 0x19830206,
				0x8080808080808080L, -99.5f, 111111.5, 0x19900606, 0x19840209 };
		Object[] args2 = new Object[args1.length];
		Marshaller m = tl.getMarshallPlan().makeMarshaller();
		tl.marshallInParams(m, args1);
		m.rewind();
		tl.marshallOutParams(m, args2);
		// Since these are value-type parameters, calling marshallOutParams
		// should be a no-op.
		assertArrayEquals(new Object[args2.length], args2);
	}

	@Test
	public void marshallParamsBasicArray() {
		TypeList tl = makeParams("bool_", ba2(BOOL), "char_", ba2(CHAR),
				"short_", ba2(SHORT), "long_", ba2(LONG), "int64_", ba2(INT64),
				"float_", ba2(FLOAT), "double_", ba2(DOUBLE), "handle_",
				ba2(HANDLE), "gdiobj_", ba2(GDIOBJ));
		Object[] args1 = new Object[] { ca(Boolean.TRUE, Boolean.FALSE),
			ca(0xf0, 0x0f), ca(0xff00, 0x00ff), ca(1, 2), ca(3L, 4L),
			ca(99999f, -99999f), ca(-300, 300.0), ca(5, 6), ca(7, 8)
		};
		Object[] args2 = new Object[args1.length];
		Marshaller m = tl.getMarshallPlan().makeMarshaller();
		tl.marshallInParams(m, args1);
		m.rewind();
		tl.marshallOutParams(m, args2);
		// Since these are array-type parameters, calling marshallOutParams
		// should be a no-op.
		assertArrayEquals(new Object[args2.length], args2);
	}

	@Test
	public void marshallParamsStructValue() throws Exception {
		TypeList tl = makeParams("c", proxy("CIRCLE", StorageType.VALUE, 1));
		Object[] args1 = new Object[] { new SuContainer() };
		Object[] args2 = new Object[] { new SuContainer() };
		tl.resolve(0);
		Marshaller m = tl.getMarshallPlan().makeMarshaller();
		tl.marshallInParams(m, args1);
		m.rewind();
		tl.marshallOutParams(m, args2);
		// Unlike with basic types, with structures that are passed by value,
		// they *might* contain a pointer which has to be marshalled out, so
		// we do marshall out structures even though the native side couldn't
		// have changed anything that was copied on the stack.
		assertArrayEquals(new Object[] { CIRCLE(0.0, 0.0, 0.0) }, args2);
	}

	@Test
	public void marshallParamsStructArray() throws Exception {
		TypeList tl = makeParams("c", proxy("CIRCLE", StorageType.ARRAY, 2));
		Object[] args1 = new Object[] { ca(CIRCLE_Radius(19), CIRCLE(-1.0, -2.0, 123456.125)) };
		Object[] args2 = new Object[] { new SuContainer() };
		tl.resolve(0);
		Marshaller m = tl.getMarshallPlan().makeMarshaller();
		tl.marshallInParams(m, args1);
		m.rewind();
		tl.marshallOutParams(m, args2);
		// Unlike with basic types, with structures that are passed as arrays,
		// they *might* contain a pointer which has to be marshalled out, so
		// we do marshall out structures even though the native side couldn't
		// have changed anything that was copied on the stack.
		assertArrayEquals(
				new Object[] { ca(CIRCLE(0, 0, 19), CIRCLE(-1.0, -2.0, 123456.125)) },
				args2);
	}

	@Test
	public void marshallParamsStructPointer() throws Exception {
		TypeList tl = makeParams("c", proxy("CIRCLE", StorageType.POINTER, 1));
		Object[] args1 = new Object[] { CIRCLE(-1.0, -2.0, 123456.125) };
		Object[] args2 = new Object[] { new SuContainer() };
		tl.resolve(0);
		Marshaller m = tl.getMarshallPlan().makeMarshaller();
		tl.marshallInParams(m, args1);
		m.rewind();
		tl.marshallOutParams(m, args2);
		assertArrayEquals(new Object[] { CIRCLE(-1.0, -2.0, 123456.125) }, args2);
	}

	@Test
	public void marshallParamsInString() {
		TypeList tl = makeParams("str", InString.INSTANCE);
		Object[] args1 = new Object[] { "antidisestablishmentarianism" };
		Object[] args2 = new Object[] { null };
		Marshaller m = tl.getMarshallPlan().makeMarshaller();
		tl.marshallInParams(m, args1);
		m.rewind();
		tl.marshallOutParams(m, args2);
		assertArrayEquals(new Object[] { null }, args2);
	}

	@Test
	public void marshallParamsInOutStringNoBuffer() {
		TypeList tl = makeParams("str", InOutString.INSTANCE);
		Object[] args1 = new Object[] { "Freedom granted only when it is known beforehand that its effects will be beneficial is not freedom." };
		Object[] args2 = new Object[] { null };
		Marshaller m = tl.getMarshallPlan().makeMarshaller();
		tl.marshallInParams(m, args1);
		m.rewind();
		// Simulate native side storing a String into the viArray. The native
		// side is required to return a String when an InOutString is marshalled
		// in...
		m.getViArray()[0] = "--F.A. Hayek";
		tl.marshallOutParams(m, args2);
		assertArrayEquals(new Object[] { null }, args2);
	}

	@Test
	public void marshallParamsInOutStringIntoBuffer() {
		TypeList tl = makeParams("str", InOutString.INSTANCE);
		final String str = "Freedom granted only when it is known beforehand that its effects will be beneficial is not freedom.";
		Object[] args1 = new Object[] { str };
		Object[] args2 = new Object[] { new Buffer(str.length(), "") };
		Marshaller m = tl.getMarshallPlan().makeMarshaller();
		tl.marshallInParams(m, args1);
		m.rewind();
		// Simulate native side storing a String into the viArray. The native
		// side is required to return a String when an InOutString is marshalled
		// in...
		m.getViArray()[0] = "--F.A. Hayek";
		tl.marshallOutParams(m, args2);
		// Nothing gets marshalled out because the code didn't have a Buffer it
		// could pass to the native side at the time the value was marshalled
		// in.
		assertArrayEquals(new Object[] { new Buffer(str.length(), "") }, args2);
	}

	@Test
	public void marshallParamsInOutStringFromAndIntoBuffer() {
		TypeList tl = makeParams("str", InOutString.INSTANCE);
		final String str = "The quick brown fox jumped over the lazy dog.";
		Object[] args1 = new Object[] { new Buffer(str.length(), str) };
		Object[] args2 = new Object[] { new Buffer(str.length(), "") };
		Marshaller m = tl.getMarshallPlan().makeMarshaller();
		tl.marshallInParams(m, args1);
		m.rewind();
		tl.marshallOutParams(m, args2);
		// Nothing gets marshalled out because (A) this test doesn't actually
		// cause any native code to be invoked and (B) even if it did, the
		// any changes caused by the native side would be made to the buffer
		// originally passed to the marshall in code, not a new buffer passed to
		// the marshall out code.
		assertArrayEquals(new Object[] { new Buffer(str.length(), "") }, args2);
	}
}
