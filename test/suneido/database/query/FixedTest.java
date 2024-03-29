/* Copyright 2009 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido.database.query;

import static java.util.Arrays.asList;
import static org.junit.Assert.assertEquals;

import java.util.List;

import org.junit.Test;

public class FixedTest extends TestBase {

	@Test
	public void extend() {
		makeTable();
		for (String[] c : cases) {
			assertEquals(c[1],
					CompileQuery.parse(db, serverData, c[0]).fixed().toString());
		}
	}
	private static String[][] cases = {
		{ "test", "[]" },

		{ "test extend f=1", "[f=(1)]" },
		{ "test extend f=1, g='s'", "[f=(1), g=('s')]" },
		{ "test extend f=1 extend g=2", "[g=(2), f=(1)]" },
		{ "test extend f=1 where f=2", "[f=(2)]" },
		{ "test extend f=1, g=2 where f=3", "[f=(3), g=(2)]" },

		{ "test where a=1", "[a=(1)]" },
		{ "test where a=1 and b='s' and a = b", "[a=(1), b=('s')]" },

		{ "test union (test extend f=1)", "[f=(1,'')]" },
		{ "(test extend f=2) union (test extend f=1)", "[f=(2,1)]" },

		{ "(test extend f=1, g=2) project a,b", "[]" },
		{ "(test extend f=1, g=2) project a,g", "[g=(2)]" },
		{ "(test extend f=1, g=2) project a,f,g", "[f=(1), g=(2)]" },

		{ "(test extend f=1) join (test extend f=1, g=2)", "[f=(1), g=(2)]" },

		{ "test extend f=1, g=2 rename g to h", "[f=(1), h=(2)]" },
		{ "test leftjoin (test extend f=1, g=2)", "[]" },
		{ "test leftjoin (test extend f=1)", "[f=(1,'')]" },
		{ "(test extend x=1, y=2) leftjoin (test extend f=1)",
			"[x=(1), y=(2), f=(1,'')]" },
		{ "(test extend x=1, y=2) leftjoin (test extend f=1, g=2)",
			"[x=(1), y=(2)]" },
	};

	@Test
	public void combine() {
		List<Fixed> f =
				Fixed.combine(asList(new Fixed("f", 0)), asList(new Fixed("g", 1)));
		assertEquals("[f=(0), g=(1)]", f.toString());
		f = Fixed.combine(asList(new Fixed("f", 0)), asList(new Fixed("f", 1)));
		assertEquals("[f=(0)]", f.toString());
	}
}
