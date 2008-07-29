package suneido.database.query;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class OptimizeTest extends TestBase {
	@Test
	public void test() {
		for (String[] c : cases) {
			System.out.println("CASE " + c[0]);
			Query q = ParseQuery.parse(c[0]).setup();
			assertEquals(c[0], c[1], q.toString());
		}
	}

	private static String[][] cases = {
		{"customer",
			"customer^(id)" },

		{"inven",
			"inven^(item)" },

		{"trans",
			"trans^(item)" },

		{ "hist",
			"hist^(date)" },

		{ "task",
			"task^(tnum)", "" },

		{ "alias",
			"alias^(id)" },

		{ "supplier",
			"supplier^(city)" },

		{ "customer project id,name",
			"customer^(id) PROJECT-COPY id,name" },

		{ "trans project item",
			"trans^(item) PROJECT-SEQ^(item) item" },

		{ "trans project item,id,cost,date project item",
			"trans^(item) PROJECT-SEQ^(item) item" },

		{ "trans project item,id,cost project item,id project item",
			"trans^(item) PROJECT-SEQ^(item) item" },

		{ "hist project date,item",
			"hist^(date,item,id) PROJECT-SEQ^(date,item,id) date,item" },

//		{ "customer project city",
//			"customer^(id) PROJECT-LOOKUP city" },
//
//		{ "customer project id,city project city",
//			"customer^(id) PROJECT-LOOKUP city" },

		{ "task join co",
			"(co^(tnum) JOIN 1:1 on (tnum) task^(tnum))", "" },

		{ "cus where abbrev = 'a'",
			"cus^(abbrev) WHERE^(abbrev)", "" },

		{ "hist where item =~ 'a'",
			"hist^(date,item,id) WHERE^(date,item,id) (item =~ 'a')", "" },

		{ "hist where cost =~ 5",
			"hist^(date) WHERE^(date) (cost =~ 5)", "" },

		{ "(((task join co)) join (cus where abbrev = 'a'))",
			"((co^(tnum) JOIN 1:1 on (tnum) task^(tnum)) "
				+ "JOIN n:1 on (cnum) cus^(cnum) WHERE^(cnum))" },

		{ "((task join (co where signed = 990103)) join (cus where abbrev = 'a'))",
			"((co^(tnum) WHERE^(tnum) JOIN 1:1 on (tnum) task^(tnum)) "
				+ "JOIN n:1 on (cnum) cus^(cnum) WHERE^(cnum))" },
	};

}
