package suneido.database.query;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class QueryTest extends TestBase {
	@Test
	public void test() {
		for (String[] c : cases) {
			System.out.println("CASE " + c[0]);
			Query q = ParseQuery.parse(c[0]).setup();
			assertEquals(c[0], c[1], q.toString());
		}
	}

	private static String[][] cases = {
		{ "task",
			"task^(tnum)", "" },
		{ "task join co",
			"(co^(tnum) JOIN 1:1 on (tnum) task^(tnum))", "" },
		{ "cus where abbrev = 'a'",
			"cus^(abbrev) WHERE^(abbrev)", "" },
		{ "(((task join co)) join (cus where abbrev = 'a'))",
			"((co^(tnum) JOIN 1:1 on (tnum) task^(tnum)) JOIN n:1 on (cnum) cus^(cnum) WHERE^(cnum))",
			"tnum	signed	cnum	abbrev	name\n"
					+ "100	990101	1	\"a\"	\"axon\"\n"
					+ "104	990103	1	\"a\"	\"axon\"\n" },
		{ "((task join (co where signed = 990103)) join (cus where abbrev = 'a'))",
			"((co^(tnum) WHERE^(tnum) JOIN 1:1 on (tnum) task^(tnum)) JOIN n:1 on (cnum) cus^(cnum) WHERE^(cnum))",
			"tnum	signed	cnum	abbrev	name\n"
					+ "104	990103	1	\"a\"	\"axon\"\n" },
	};

}
