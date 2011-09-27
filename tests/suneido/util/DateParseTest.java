package suneido.util;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.text.SimpleDateFormat;
import java.util.Date;

import org.junit.Test;

public class DateParseTest {

	@Test
	public void test() {
		d("090625", "yMd", "2009 Jun 25");
		d("20090625", "yMd", "2009 Jun 25");
		d("June 25, 2009", "yMd", "2009 Jun 25");
		d("020304", "yMd", "2002 Mar 4");
		d("020304", "Mdy", "2004 Feb 3");
		d("032299", "yMd", "1999 Mar 22");
		d("2009-06-25", "yMd", "2009 Jun 25");
		d("Wed. 25 June '09", "yMd", "2009 Jun 25");

		bad("25252525", "yMd");
	}

	private static void d(String ds, String fmt, String result) {
		Date d = DateParse.parse(ds, fmt);
		assertNotNull(d);
		assertEquals(result, new SimpleDateFormat("yyyy MMM d").format(d));
	}

	private static void bad(String ds, String fmt) {
		Date d = DateParse.parse(ds, fmt);
		assertEquals(null, d);
	}

}
