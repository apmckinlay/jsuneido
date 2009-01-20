package suneido.database.query;

import static org.junit.Assert.assertEquals;
import static suneido.Util.list;

import java.util.List;

import org.junit.Test;

import suneido.SuInteger;
import suneido.database.TestBase;

public class FixedTest extends TestBase {
	@Test
	public void extend() {
		makeTable();
		for (String[] c : cases)
			assertEquals(c[1], ParseQuery.parse(serverData, c[0]).fixed().toString());
	}
	private static String[][] cases = {
		{ "test", "[]" },

		{ "test extend f=1", "[f=(1)]" },
		{ "test extend f=1, g='s'", "[f=(1), g=('s')]" },
		{ "test extend f=1 extend g=2", "[g=(2), f=(1)]" },
		{ "test extend f=1 where f=2", "[f=(2)]" },
		{ "test extend f=1, g=2 where f=3", "[f=(3), g=(2)]" },

		{ "test where a=1", "[a=(1)]" },
		{ "test where a=1 and b='s', c=a", "[a=(1), b=('s')]" },

		{ "test union (test extend f=1)", "[f=(1,'')]" },
		{ "(test extend f=2) union (test extend f=1)", "[f=(2,1)]" },

		{ "(test extend f=1, g=2) project a,b", "[]" },
		{ "(test extend f=1, g=2) project a,g", "[g=(2)]" },
		{ "(test extend f=1, g=2) project a,f,g", "[f=(1), g=(2)]" },

		{ "(test extend f=1) join (test extend f=1, g=2)", "[f=(1), g=(2)]" },
	};

	@Test
	public void combine() {
		List<Fixed> f = Fixed.combine(list(new Fixed("f", SuInteger.ZERO)),
				list(new Fixed("g", SuInteger.ONE)));
		assertEquals("[f=(0), g=(1)]", f.toString());
		f = Fixed.combine(list(new Fixed("f", SuInteger.ZERO)),
				list(new Fixed("f", SuInteger.ONE)));
		assertEquals("[f=(0)]", f.toString());
	}
}
