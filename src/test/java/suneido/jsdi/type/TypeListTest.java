/* Copyright 2013 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido.jsdi.type;

import static org.junit.Assert.assertArrayEquals;
import static suneido.jsdi.type.BasicType.*;
import static suneido.util.testing.Throwing.assertThrew;

import java.util.Arrays;

import org.junit.BeforeClass;
import org.junit.Test;

import suneido.SuContainer;
import suneido.jsdi.Buffer;
import suneido.jsdi.DllInterface;
import suneido.jsdi.JSDI;
import suneido.jsdi.SimpleContext;
import suneido.jsdi.StorageType;
import suneido.jsdi.marshall.Marshaller;
import suneido.jsdi.type.BasicArray;
import suneido.jsdi.type.BasicType;
import suneido.jsdi.type.BasicValue;
import suneido.jsdi.type.InOutString;
import suneido.jsdi.type.InString;
import suneido.jsdi.type.Proxy;
import suneido.jsdi.type.ResourceType;
import suneido.jsdi.type.Type;
import suneido.jsdi.type.TypeList;
import suneido.language.ContextLayered;
import suneido.language.Numbers;
import suneido.util.testing.Assumption;

/**
 * Test for {@link TypeList}.
 *
 * @author Victor Schappert
 * @since 20130722
 */
@DllInterface
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

	private static final boolean[] FALSETRUE = { true, false };

	private static TypeList makeParams(Object... tuples) {
		final int N = tuples.length;
		assert 0 == N % 2;
		final TypeList.Args args = new TypeList.Args("params", N / 2);
		int k = 0;
		while (k < N) {
			args.add((String)tuples[k++], (Type)tuples[k++]);
		}
		return JSDI.getInstance().getFactory().makeTypeList(args);
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
		for (boolean isCallbackPlan : FALSETRUE) {
			TypeList tl = makeParams();
			Object[] args = new Object[0];
			Marshaller m = tl.makeParamsMarshallPlan(isCallbackPlan, false)
					.makeMarshaller();
			tl.marshallInParams(m, args);
			m.rewind();
			tl.marshallOutParams(m, args);
		}
	}

	@Test
	public void marshallParamsBasicValue() {
		for (boolean isCallbackPlan : FALSETRUE) {
			TypeList tl = makeParams("bool_", bv(BOOL), "char_", bv(INT8),
					"short_", bv(INT16), "long_", bv(INT32), "int64_",
					bv(INT64), "pointer_", bv(OPAQUE_POINTER), "float_",
					bv(FLOAT), "double_", bv(DOUBLE), "handle_", bv(HANDLE),
					"gdiobj_", bv(GDIOBJ));
			Object[] args1 = new Object[] { Boolean.TRUE, -99, 9999,
					0x19830206, 0x8080808080808080L, 0x19800725, -99.5f,
					111111.5, 0x19900606, 0x19840209 };
			Object[] args2 = new Object[args1.length];
			Marshaller m = tl.makeParamsMarshallPlan(isCallbackPlan, false)
					.makeMarshaller();
			tl.marshallInParams(m, args1);
			m.rewind();
			tl.marshallOutParams(m, args2);
			// Since these are value-type parameters, calling marshallOutParams
			// should be a no-op.
			assertArrayEquals(new Object[args2.length], args2);
		}
	}

	@Test
	public void marshallParamsBasicArray() {
		for (boolean isCallbackPlan : FALSETRUE) {
			TypeList tl = makeParams("bool_", ba2(BOOL), "char_", ba2(INT8),
					"short_", ba2(INT16), "long_", ba2(INT32), "int64_",
					ba2(INT64), "pointer_", ba2(OPAQUE_POINTER), "float_",
					ba2(FLOAT), "double_", ba2(DOUBLE), "handle_", ba2(HANDLE),
					"gdiobj_", ba2(GDIOBJ));
			Object[] args1 = new Object[] { ca(Boolean.TRUE, Boolean.FALSE),
					ca(0xf0, 0x0f), ca(0xff00, 0x00ff), ca(1, 2), ca(3L, 4L),
					ca(5, 6), ca(99999f, -99999f), ca(-300, 300.0), ca(5, 6),
					ca(7, 8) };
			Object[] args2 = new Object[args1.length];
			Marshaller m = tl.makeParamsMarshallPlan(isCallbackPlan, false)
					.makeMarshaller();
			tl.marshallInParams(m, args1);
			m.rewind();
			tl.marshallOutParams(m, args2);
			// Since these are array-type parameters, calling marshallOutParams
			// should be a no-op.
			assertArrayEquals(new Object[args2.length], args2);
		}
	}

	@Test
	public void marshallParamsStructValue() throws Exception {
		for (boolean isCallbackPlan : FALSETRUE) {
			TypeList tl = makeParams("c", proxy("CIRCLE", StorageType.VALUE, 1));
			Object[] args1 = new Object[] { new SuContainer() };
			Object[] args2 = new Object[] { new SuContainer() };
			tl.resolve(0);
			Marshaller m = tl.makeParamsMarshallPlan(isCallbackPlan, false)
					.makeMarshaller();
			tl.marshallInParams(m, args1);
			m.rewind();
			tl.marshallOutParams(m, args2);
			// Unlike with basic types, with structures that are passed by
			// value, they *might* contain a pointer which has to be marshalled
			// out, so we do marshall out structures even though the native side
			// couldn't have changed anything that was copied on the stack.
			assertArrayEquals(new Object[] { CIRCLE(0.0, 0.0, 0.0) }, args2);
		}
	}

	@Test
	public void marshallParamsStructArray() throws Exception {
		for (boolean isCallbackPlan : FALSETRUE) {
			TypeList tl = makeParams("c", proxy("CIRCLE", StorageType.ARRAY, 2));
			Object[] args1 = new Object[] { ca(CIRCLE_Radius(19),
					CIRCLE(-1.0, -2.0, 123456.125)) };
			Object[] args2 = new Object[] { new SuContainer() };
			tl.resolve(0);
			Marshaller m = tl.makeParamsMarshallPlan(isCallbackPlan, false)
					.makeMarshaller();
			tl.marshallInParams(m, args1);
			m.rewind();
			tl.marshallOutParams(m, args2);
			// Unlike with basic types, with structures that are passed as
			// arrays, they *might* contain a pointer which has to be marshalled
			// out, so we do marshall out structures even though the native side
			// couldn't have changed anything that was copied on the stack.
			assertArrayEquals(
					new Object[] { ca(CIRCLE(0, 0, 19),
							CIRCLE(-1.0, -2.0, 123456.125)) }, args2);
		}
	}

	@Test
	public void marshallParamsStructPointer() throws Exception {
		for (boolean isCallbackPlan : FALSETRUE) {
			TypeList tl = makeParams("c",
					proxy("CIRCLE", StorageType.POINTER, 1));
			Object[] args1 = new Object[] { CIRCLE(-1.0, -2.0, 123456.125) };
			Object[] args2 = new Object[] { new SuContainer() };
			tl.resolve(0);
			Marshaller m = tl.makeParamsMarshallPlan(isCallbackPlan, false)
					.makeMarshaller();
			tl.marshallInParams(m, args1);
			m.rewind();
			// Simulate native side storing a non-null pointer
			m.putInt8((byte) 1);
			m.rewind();
			tl.marshallOutParams(m, args2);
			assertArrayEquals(new Object[] { CIRCLE(-1.0, -2.0, 123456.125) },
					args2);
		}
	}

	@Test
	public void marshallParamsInString_NoCallback() {
		TypeList tl = makeParams("str", InString.INSTANCE);
		Object[] args1 = new Object[] { "antidisestablishmentarianism" };
		Object[] args2 = new Object[] { null };
		Marshaller m = tl.makeParamsMarshallPlan(false, false).makeMarshaller();
		tl.marshallInParams(m, args1);
		m.rewind();
		tl.marshallOutParams(m, args2);
		assertArrayEquals(new Object[] { null }, args2);
	}

	@Test
	public void marshallParamsInOutStringNoBuffer() {
		for (boolean isCallbackPlan : FALSETRUE) {
			TypeList tl = makeParams("str", InOutString.INSTANCE);
			Object[] args1 = new Object[] { "Freedom granted only when it is known beforehand that its effects will be beneficial is not freedom." };
			Object[] args2 = new Object[] { null };
			Marshaller m = tl.makeParamsMarshallPlan(isCallbackPlan, false)
					.makeMarshaller();
			tl.marshallInParams(m, args1);
			m.rewind();
			// Simulate native side storing a String into the viArray. The
			// native side is required to return a String when an InOutString is
			// marshalled in...
			m.getViArray()[0] = "--F.A. Hayek";
			tl.marshallOutParams(m, args2);
			assertArrayEquals(new Object[] { null }, args2);
		}
	}

	@Test
	public void marshallParamsInOutStringIntoBuffer() {
		for (boolean isCallbackPlan : FALSETRUE) {
			TypeList tl = makeParams("str", InOutString.INSTANCE);
			final String str = "Freedom granted only when it is known beforehand that its effects will be beneficial is not freedom.";
			Object[] args1 = new Object[] { str };
			Object[] args2 = new Object[] { new Buffer(str.length(), "") };
			Marshaller m = tl.makeParamsMarshallPlan(isCallbackPlan, false)
					.makeMarshaller();
			tl.marshallInParams(m, args1);
			m.rewind();
			// Simulate native side storing a String into the viArray. The
			// native side is required to return a String when an InOutString is
			// marshalled in...
			m.getViArray()[0] = "--F.A. Hayek";
			tl.marshallOutParams(m, args2);
			// Nothing gets marshalled out because the code didn't have a Buffer
			// it could pass to the native side at the time the value was
			// marshalled in.
			assertArrayEquals(new Object[] { new Buffer(str.length(), "") },
					args2);
		}
	}

	@Test
	public void marshallParamsInOutStringFromAndIntoBuffer() {
		for (boolean isCallbackPlan : FALSETRUE) {
			TypeList tl = makeParams("str", InOutString.INSTANCE);
			final String str = "The quick brown fox jumped over the lazy dog.";
			Object[] args1 = new Object[] { new Buffer(str.length() + 1, str) };
			Object[] args2 = new Object[] { new Buffer(str.length(), "") };
			Marshaller m = tl.makeParamsMarshallPlan(isCallbackPlan, false)
					.makeMarshaller();
			tl.marshallInParams(m, args1);
			m.rewind();
			tl.marshallOutParams(m, args2);
			// Nothing gets marshalled out because (A) this test doesn't
			// actually cause any native code to be invoked and (B) even if it
			// did, any changes caused by the native side would be made to the
			// buffer originally passed to the marshall in code, not a new buffer
			// passed to the marshall out code. **HOWEVER**, marshalling an
			// InOutString type into an instance of Buffer results in the
			// Buffer being truncated at the first NULL!
			assertArrayEquals(new Object[] { new Buffer(0, "") }, args2);
		}
	}

	@Test
	public void marshallParamsResourceFromToIntResource_NoCallback() {
		final TypeList tl = makeParams("res", ResourceType.INSTANCE);
		int intResource = Short.MAX_VALUE + 1;
		Object[] args1 = new Object[] { intResource };
		final Marshaller m = tl.makeParamsMarshallPlan(false, false)
				.makeMarshaller();
		tl.marshallInParams(m, args1);
		m.rewind();
		// This will throw because the native side is expected to place
		// either a String or an Integer into variable indirect storage.
		assertThrew(() -> { tl.marshallOutParams(m, new Object[1]); });
		// Simulate native side putting an Integer into the viArray.
		m.rewind();
		m.getViArray()[0] = new Integer(intResource);
		Object[] args2 = new Object[1];
		tl.marshallOutParams(m, args2);
		// Nothing gets marshalled out because parameter was passed by
		// value.
		assertArrayEquals(new Object[1], args2);
	}

	@Test
	public void marshallParamsResourceFromIntResourceToString_NoCallback() {
		final TypeList tl = makeParams("res", ResourceType.INSTANCE);
		int intResource = Short.MAX_VALUE + 1;
		Object[] args1 = new Object[] { intResource };
		final Marshaller m = tl.makeParamsMarshallPlan(false, false).makeMarshaller();
		tl.marshallInParams(m, args1);
		m.rewind();
		// This will throw because the native side is expected to place
		// either a String or an Integer into variable indirect storage.
		assertThrew(() -> { tl.marshallOutParams(m, new Object[1]); });
		// Simulate native side putting an Integer into the viArray.
		m.rewind();
		m.getViArray()[0] = "simulation of native side returning a string";
		Object[] args2 = new Object[1];
		tl.marshallOutParams(m, args2);
		// Nothing gets marshalled out because parameter was passed by
		// value.
		assertArrayEquals(new Object[1], args2);
	}
}
