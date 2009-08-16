package suneido;

import static org.junit.Assert.assertEquals;
import static suneido.language.Ops.*;

import java.math.BigDecimal;

import org.junit.Test;

public class SuValueTest {
	@Test
	public void compareTo() {
		SuContainer c1 = new SuContainer();
		c1.append(0);
		SuContainer c2 = new SuContainer();
		c2.append(0);
		c2.append(1);
		SuContainer c3 = new SuContainer();
		c3.append(1);
		Object[] values = {
			false, true,
			0, 123, 456, 789,
			"", "abc", "def",
			stringToDate("20080514.143622123"), stringToDate("20080522.143622123"),
			stringToDate("20081216.152744828"), stringToDate("20081216.153244828"),
			new SuContainer(), c1, c2, c3 };
		for (int i = 0; i < values.length; ++i)
			for (int j = 0; j < values.length; ++j)
				assertEquals(display(values[i]) + " cmp " + display(values[j]),
						Integer.signum(i - j),
						Integer.signum(cmp(values[i], values[j])));
	}

	@Test
	public void math() {
		int[] ints = { 0, 1, -1, 123, -123 };
		Object[] values = new Object[ints.length * 3];
		for (int i = 0; i < ints.length; ++i) {
			values[3 * i] = ints[i];
			values[3 * i + 1] = new BigDecimal(ints[i]);
			values[3 * i + 2] = Integer.toString(ints[i]);
		}
		for (Object x : values)
			for (Object y : values)
				math1(x, y);
	}

	private void math1(Object x, Object y) {
		//System.out.println(Ops.typeName(x) + " " + x + " " + Ops.typeName(y) + " " + y);
		int i = toInt(x);
		int j = toInt(y);
		Object z;
		z = add(x, y);
		assertEquals(BigDecimal.valueOf(i + j), z);
		z = sub(x, y);
		assertEquals(BigDecimal.valueOf(i - j), z);
		z = mul(x, y);
		assertEquals(BigDecimal.valueOf(i * j), z);
		if (j == 0)
			return ; // skip divide by zero
		z = div(x, y);
		assertEquals(BigDecimal.valueOf(i).divide(BigDecimal.valueOf(j), mc), z);
	}

}
