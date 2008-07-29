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

		{ "customer project city",
			"customer^(id) PROJECT-LOOKUP city" },

		{ "customer project id,city project city",
			"customer^(id) PROJECT-LOOKUP city" },

		{ "customer rename city to location",
			"customer^(id) RENAME city to location" },

		{ "customer rename city to location rename location to loc",
			"customer^(id) RENAME city to loc" },

		{ "customer rename id to i rename name to n rename city to c",
			"customer^(id) RENAME id to i, name to n, city to c" },

		{ "inven rename item to part, qty to onhand",
			"inven^(item) RENAME item to part, qty to onhand" },

		{ "inven rename item to part rename qty to onhand",
			"inven^(item) RENAME item to part, qty to onhand" },

		{ "customer times inven",
			"(customer^(id) TIMES inven^(item))" },

		{ "inven times customer",
			"(inven^(item) TIMES customer^(id))" },

		{ "hist union trans",
			"(hist^(date,item,id) UNION-MERGE^(date,item,id) trans^(date,item,id))" },

		{ "hist union hist",
			"(hist^(date,item,id) UNION-MERGE^(date,item,id) hist^(date,item,id))" },

		{ "trans union hist",
			"(trans^(date,item,id) UNION-MERGE^(date,item,id) hist^(date,item,id))" },

		{ "hist union hist2",
			"(hist2^(date) UNION-LOOKUP^(date,item,id) hist^(date,item,id))" },

		{ "(trans where cost=100) union (trans where cost=200)",
			"(trans^(item) WHERE^(item) UNION-DISJOINT(cost) trans^(item) WHERE^(item))" },

		{ "hist minus trans",
			"(hist^(date,item,id) MINUS^(date,item,id) trans^(date,item,id))" },

		{ "trans minus hist",
			"(trans^(date,item,id) MINUS^(date,item,id) hist^(date,item,id))" },

		{ "hist intersect trans",
			"(hist^(date,item,id) INTERSECT^(date,item,id) trans^(date,item,id))" },

		{ "trans intersect hist",
			"(trans^(date,item,id) INTERSECT^(date,item,id) hist^(date,item,id))" },

		{ "(trans union trans) intersect (hist union hist)", 
			"((trans^(date,item,id) " +
				"UNION-MERGE^(date,item,id) trans^(date,item,id)) " +
				"INTERSECT^(date,item,id,cost) (hist^(date,item,id) " +
				"UNION-MERGE^(date,item,id) hist^(date,item,id)) " +
				"TEMPINDEXN(date,item,id,cost) unique)" },

//		{ "(trans minus hist) union (trans intersect hist)", 
//				"((trans^(date,item,id) " +
//				"MINUS^(date,item,id) hist^(date,item,id)) " +
//				"UNION-MERGE^(date,item,id) (trans^(date,item,id)) " +
//				"INTERSECT^(date,item,id) hist^(date,item,id)))" },
					
		{ "task join co",
			"(co^(tnum) JOIN 1:1 on (tnum) task^(tnum))", "" },

		{ "trans where cost=100",
			"trans^(item) WHERE^(item)" },

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
