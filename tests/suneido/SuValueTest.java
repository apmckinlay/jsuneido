package suneido;

import java.math.BigDecimal;

import org.junit.Test;
import static org.junit.Assert.*;

public class SuValueTest {
	@Test
	public void compareTo() {
		SuValue[] values = {
				SuBoolean.FALSE, SuBoolean.TRUE, 
				SuInteger.ZERO, new SuNumber(123), new SuInteger(456), new SuNumber(789),
				SuString.EMPTY, new SuString("abc"), new SuString("def"),
				new SuDate("#20080514.143622123"), new SuDate("#20080522.143622123"),
				new SuContainer(), new SuClass() };
		for (int i = 0; i < values.length; ++i)
			for (int j = 0; j < values.length; ++j)
				assertEquals(Integer.signum(i - j), values[i].compareTo(values[j]));
		
		SuValue x = new SuClass();
		SuValue y = new SuClass();
		assertEquals(Integer.signum(x.hashCode() - y.hashCode()), x.compareTo(y));
		assertEquals(Integer.signum(y.hashCode() - x.hashCode()), y.compareTo(x));
	}
	
	@Test
	public void math() {
		int[] ints = { 0, 1, -1, 123, -123 };
		SuValue[] values = new SuValue[ints.length * 3];
		for (int i = 0; i < ints.length; ++i) {
			values[3 * i] = new SuInteger(ints[i]);
			values[3 * i + 1] = new SuNumber(ints[i]);
			values[3 * i + 2] = new SuString(Integer.toString(ints[i]));
		}
		for (SuValue x : values) {
			for (SuValue y : values)
				math1(x, y);
		}
	}

	private void math1(SuValue x, SuValue y) {
		// System.out.println(x.typeName() + " " + x + " " + y.typeName() + " " + y);
		int i = x.integer();
		int j = y.integer();
		SuValue z;
		z = x.add(y);
		assertEquals(new SuNumber(i + j), z);
		z = x.sub(y);
		assertEquals(new SuNumber(i - j), z);
		z = x.mul(y);
		assertEquals(new SuNumber(i * j), z);
		if (j == 0)
			return ; // skip divide by zero
		z = x.div(y);
		assertEquals(new SuNumber(new BigDecimal(i).divide(new BigDecimal(j), SuNumber.mc)), z);
	}
	
	@Test(expected=SuException.class)
	public void call1() {
		SuValue x = SuInteger.ZERO;
		x.invoke(SuSymbol.CALLi);
	}
	@Test(expected=SuException.class)
	public void call2() {
		SuValue x = SuInteger.ZERO;
		x.invoke(SuSymbol.EACHi);
	}
	@Test(expected=SuException.class)
	public void getdata() {
		SuInteger.ZERO.getdata(SuSymbol.CALL);
	}
	@Test(expected=SuException.class)
	public void putdata() {
		SuString.EMPTY.putdata(SuSymbol.CALL, SuInteger.ZERO);
	}
}
