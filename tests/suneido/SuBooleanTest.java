package suneido;

import org.junit.Test;
import static org.junit.Assert.*;

public class SuBooleanTest {
	@Test
	public void toString_test() {
		assertEquals("true", SuBoolean.TRUE.toString());
		assertEquals("false", SuBoolean.FALSE.toString());
	}
	
	@Test
	public void integer_test() {
		assertEquals(1, SuBoolean.TRUE.integer());
		assertEquals(0, SuBoolean.FALSE.integer());
	}
	
	@Test
	public void number_test() {
		assertEquals(SuNumber.ONE, SuBoolean.TRUE.number());
		assertEquals(SuNumber.ZERO, SuBoolean.FALSE.number());
	}
	
	@Test
	public void compareTo_test() {
		assertEquals(0, SuBoolean.TRUE.compareTo(SuBoolean.TRUE));
		assertEquals(0, SuBoolean.FALSE.compareTo(SuBoolean.FALSE));
		assertEquals(-1, SuBoolean.FALSE.compareTo(SuBoolean.TRUE));
		assertEquals(+1, SuBoolean.TRUE.compareTo(SuBoolean.FALSE));
		assertEquals(-1, SuBoolean.TRUE.compareTo(SuString.EMPTY));
		assertEquals(-1, SuBoolean.FALSE.compareTo(SuString.EMPTY));
	}
}
