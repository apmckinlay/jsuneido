package suneido;

import static org.junit.Assert.assertEquals;

import java.math.BigDecimal;
import java.nio.ByteBuffer;

import org.junit.Test;

import suneido.Symbols.Num;
import suneido.Symbols.Sym;

public class SuValueTest {
	@Test
	public void compareTo() {
		SuContainer c1 = new SuContainer();
		c1.append(SuInteger.ZERO);
		SuContainer c2 = new SuContainer();
		c2.append(SuInteger.ZERO);
		c2.append(SuInteger.ONE);
		SuContainer c3 = new SuContainer();
		c3.append(SuInteger.ONE);
		SuValue[] values = {
			SuBoolean.FALSE, SuBoolean.TRUE,
			SuInteger.ZERO, new SuDecimal(123), SuInteger.valueOf(456), new SuDecimal(789),
			SuString.EMPTY, new SuString("abc"), new SuString("def"),
			new SuDate("#20080514.143622123"), new SuDate("#20080522.143622123"),
			new SuDate("#20081216.152744828"), new SuDate("#20081216.153244828"), 
			new SuContainer(), c1, c2, c3, new SuClass() };
		for (int i = 0; i < values.length; ++i)
			for (int j = 0; j < values.length; ++j)
				assertEquals(Integer.signum(i - j), Integer.signum(values[i].compareTo(values[j])));
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
			values[3 * i] = SuInteger.valueOf(ints[i]);
			values[3 * i + 1] = new SuDecimal(ints[i]);
			values[3 * i + 2] = new SuString(Integer.toString(ints[i]));
		}
		for (SuValue x : values)
			for (SuValue y : values)
				math1(x, y);
	}

	private void math1(SuValue x, SuValue y) {
		// System.out.println(x.typeName() + " " + x + " " + y.typeName() + " " + y);
		int i = x.integer();
		int j = y.integer();
		SuValue z;
		z = x.add(y);
		assertEquals(new SuDecimal(i + j), z);
		z = x.sub(y);
		assertEquals(new SuDecimal(i - j), z);
		z = x.mul(y);
		assertEquals(new SuDecimal(i * j), z);
		if (j == 0)
			return ; // skip divide by zero
		z = x.div(y);
		assertEquals(new SuDecimal(new BigDecimal(i).divide(new BigDecimal(j), SuDecimal.mc)), z);
	}

	@Test(expected=SuException.class)
	public void call1() {
		SuValue x = SuInteger.ZERO;
		x.invoke(Num.CALL);
	}
	@Test(expected=SuException.class)
	public void call2() {
		SuValue x = SuInteger.ZERO;
		x.invoke(Num.EACH);
	}
	@Test(expected=SuException.class)
	public void getdata() {
		SuInteger.ZERO.getdata(Sym.CALL);
	}
	@Test(expected=SuException.class)
	public void putdata() {
		SuString.EMPTY.putdata(Sym.CALL, SuInteger.ZERO);
	}

	@Test
	public void pack() {
		SuValue[] values = {
			SuBoolean.FALSE, SuBoolean.TRUE,
			SuInteger.ZERO, SuDecimal.ZERO, SuInteger.ONE, SuInteger.valueOf(123),
			SuInteger.valueOf(-1),
			SuString.EMPTY, new SuString("abc") };
		for (SuValue x : values) {
			ByteBuffer buf = ByteBuffer.allocate(x.packSize());
			x.pack(buf);
			buf.position(0);
			SuValue y = SuValue.unpack(buf);
			assertEquals(x, y);
		}
	}
}
