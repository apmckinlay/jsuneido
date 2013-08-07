package suneido.language.jsdi.type;

import static org.junit.Assert.*;

import org.junit.BeforeClass;
import org.junit.Test;

import suneido.SuContainer;
import suneido.language.Compiler;
import suneido.language.ContextLayered;
import suneido.util.testing.Assumption;

public class CallbackTest {

	@BeforeClass
	public static void setUpBeforeClass() throws Exception {
		Assumption.jvmIs32BitOnWindows();
		CONTEXT = new SimpleContext(NAMED_TYPES);
	}

	private static ContextLayered CONTEXT;
	private static final String[] NAMED_TYPES = {
		"TestCallback_Long1", "callback(long a)",
		"TestCallback_Long2", "callback(long a, long b)",
		"TestCallback_Packed_CharCharShortLong", "callback(Packed_CharCharShortLong a)",
		"TestCallback_Recursive_StringSum", "callback(Recursive_StringSum1 * ptr)",
		"Packed_CharCharShortLong", "struct { char a; char b; short c; long d; }",
		"Recursive_StringSum1",
			"struct\n" +
			"\t{\n" +
			"\tPacked_CharCharShortLong[2] x\n" +
			"\tstring str\n" +
			"\tbuffer buffer_\n" +
			"\tlong len\n" +
			"\tRecursive_StringSum2 * inner\n" +
			"\t}",
		"Recursive_StringSum2",
			"struct\n" +
			"\t{\n" +
			"\tPacked_CharCharShortLong[2] x\n" +
			"\tstring str\n" +
			"\tbuffer buffer_\n" +
			"\tlong len\n" +
			"\tlong inner\n" +
			"\t}",
		"TestInvokeCallback_Long1",
			"dll long jsdi:_TestInvokeCallback_Long1@8(TestCallback_Long1 f, long a)",
		"TestInvokeCallback_Long2",
			"dll long jsdi:_TestInvokeCallback_Long2@12(TestCallback_Long2 f, long a, long b)",
		"TestInvokeCallback_Packed_CharCharShortLong",
			"dll long jsdi:_TestInvokeCallback_Packed_CharCharShortLong@12(" +
				"TestCallback_Packed_CharCharShortLong f, Packed_CharCharShortLong a)",
		"TestInvokeCallback_Recursive_StringSum",
			"dll long jsdi:_TestInvokeCallback_Recursive_StringSum@8(" +
				"TestCallback_Recursive_StringSum f, Recursive_StringSum1 * ptr)"
	};

	private static Object eval(String src) {
		return Compiler.eval(src, CONTEXT);
	}

	//
	// BASIC FUNCTIONALITY TESTS
	//

	@Test
	public void testCallbacksEmpty() {
		//assertEquals(eval("Callbacks()"), new SuContainer());
	}

	@Test
	public void testReturnBasicValue1Function() {
		assertEquals(11, eval("TestInvokeCallback_Long1( function(a) { return a }, 11)"));
		//assertEquals(eval("Callbacks()"), )
	}

	@Test
	public void testReturnBasicValue1Method() {
		assertEquals(21, eval(
			"x = class { New(.value) { } Method(a, b = -1) { return .value + a - b } };" +
			"TestInvokeCallback_Long1((new x(30)).Method, -10)"
		));
	}

	@Test
	public void testReturnBasicValue1Call() {
		assertEquals(
			0x1 << 18,
			eval(
				"x = class { Call(y) { return y * y } } ; " +
				"TestInvokeCallback_Long1(x(), 0x1 << 9)"
			)
		);
	}

	@Test
	public void testReturnBasicValue1CallClass() {
		assertEquals(
			0x1 << 16,
			eval(
				"x = class { CallClass(y) { return y / 2 } } ; " +
				"TestInvokeCallback_Long1(x, 0x1 << 17)"
			)
		);
	}

	@Test
	public void testReturnBasicValue2Function() {
		assertEquals(-1, eval("TestInvokeCallback_Long2( function(a, b) { return a + b }, 1000, -1001)"));
	}

	//
	// PARAMETER MISMATCHES
	//

	//
	// BAD RETURN VALUES
	//

	//
	// EXCEPTION PROPAGATION
	//
}
