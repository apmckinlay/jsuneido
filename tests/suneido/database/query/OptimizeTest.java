package suneido.database.query;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class OptimizeTest extends TestBase {
	@Test
	public void test() {
		adm("create test_minus1 (a, b, c) key(a)");
		adm("create test_minus2 (b, c, d) key(d)");
		for (String[] c : cases) {
			//System.out.println("CASE " + c[0]);
			Query q = CompileQuery.parse(serverData, c[0]).setup();
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

		{ "inven rename qty to x where x > 4",
			"inven^(item) WHERE^(item) RENAME qty to x" },

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

		{ "(test_minus1 minus test_minus2) where a is 1",
			"(test_minus1^(a) WHERE^(a) MINUS^(d) test_minus2 WHERE nothing)" },

		{ "trans intersect hist",
			"(trans^(date,item,id) INTERSECT^(date,item,id) hist^(date,item,id))" },

		{ "hist2 intersect trans",
			"(trans^(date,item,id) INTERSECT^(date) hist2^(date))" },

		{ "(hist where item = 1) intersect (trans where item = 2)",
			"(hist WHERE (item is 1) INTERSECT-DISJOINT(item) trans WHERE (item is 2))" },

		{ "cus where cnum = 2 and abbrev = 'c'",
			"cus^(abbrev) WHERE^(abbrev)" },

		{ "(trans union trans) intersect (hist union hist)",
			"((trans^(date,item,id) " +
				"UNION-MERGE^(date,item,id) trans^(date,item,id)) " +
				"INTERSECT^(date,item,id,cost) (hist^(date,item,id) " +
				"UNION-MERGE^(date,item,id) hist^(date,item,id)) " +
				"TEMPINDEX(date,item,id,cost) unique)" },

		{ "(trans minus hist) union (trans intersect hist)",
				"((trans^(date,item,id) " +
				"MINUS^(date,item,id) hist^(date,item,id)) " +
				"UNION-MERGE^(date,item,id) (trans^(date,item,id) " +
				"INTERSECT^(date,item,id) hist^(date,item,id)))" },

		{ "customer times inven",
			"(customer^(id) TIMES inven^(item))" },

		{ "inven times customer",
			"(inven^(item) TIMES customer^(id))" },

		{ "(customer times inven) join trans",
			"((customer^(id) TIMES inven^(item)) JOIN 1:n on (id,item) " +
				"trans^(date,item,id) TEMPINDEX(id,item))" },

		{ "hist join customer",
			"(hist^(date,item,id) JOIN n:1 on (id) customer^(id))" },

		{ "customer join hist",
			"(hist^(date,item,id) JOIN n:1 on (id) customer^(id))" },

		{ "trans join inven",
			"(inven^(item) JOIN 1:n on (item) trans^(item))" },

		{ "task join co",
			"(co^(tnum) JOIN 1:1 on (tnum) task^(tnum))", "" },

		{ "(trans union trans) join (inven union inven)",
			"((trans^(date,item,id) " +
				"UNION-MERGE^(date,item,id) trans^(date,item,id)) " +
				"JOIN n:n on (item) (inven^(item) " +
				"UNION-MERGE^(item) inven^(item)))" },

		{ "customer join alias",
			"(alias^(id) JOIN 1:1 on (id) customer^(id))" },

		{ "customer join supplier",
			"(customer^(id) JOIN n:n on (name,city) supplier^(city) " +
				"TEMPINDEX(name,city))" },

		{ "trans join customer join inven",
			"((trans^(date,item,id) JOIN n:1 on (id) customer^(id)) " +
				"JOIN n:1 on (item) inven^(item))" },

		{ "(trans join customer) union (hist join customer)",
			"((trans^(date,item,id) JOIN n:1 on (id) customer^(id)) " +
				"UNION-MERGE^(date,item,id) (hist^(date,item,id) " +
				"JOIN n:1 on (id) customer^(id)))" },

		{ "(trans join customer) intersect (hist join customer)",
			"((trans^(date,item,id) JOIN n:1 on (id) customer^(id)) " +
				"INTERSECT^(date,item,id) (hist^(date,item,id) " +
				"JOIN n:1 on (id) customer^(id)))" },

		{ "(((task join co)) join (cus where abbrev = 'a'))",
			"((co^(tnum) JOIN 1:1 on (tnum) task^(tnum)) "
				+ "JOIN n:1 on (cnum) cus^(cnum) WHERE^(cnum))" },

		{ "((task join (co where signed = 990103)) join (cus where abbrev = 'a'))",
			"((co^(tnum) WHERE^(tnum) JOIN 1:1 on (tnum) task^(tnum)) "
				+ "JOIN n:1 on (cnum) cus^(cnum) WHERE^(cnum))" },

		{ "inven leftjoin trans",
			"(inven^(item) LEFTJOIN 1:n on (item) trans^(item))" },

		{ "customer leftjoin hist2",
					"(customer^(id) LEFTJOIN 1:n on (id) hist2^(id))" },

		{ "customer leftjoin hist2 sort date",
					"(customer^(id) LEFTJOIN 1:n on (id) hist2^(id)) TEMPINDEX(date)" },

		{ "inven where qty + 1 > 5",
			"inven^(item) WHERE^(item) ((qty + 1) > 5)" },

		{ "trans where \"mousee\" = item $ id",
			"trans^(date,item,id) " +
				"WHERE^(date,item,id) ('mousee' is (item $ id))" },

		{ "inven where qty + 1 in (3,8)",
			"inven^(item) WHERE^(item) (qty + 1) in (3,8)" },

		{ "inven where qty + 1 in (33)",
			"inven^(item) WHERE^(item) (qty + 1) in (33)" },

		{ "trans where cost=100",
			"trans^(item) WHERE^(item)" },

		{ "cus where abbrev = 'a'",
			"cus^(abbrev) WHERE^(abbrev)", "" },

		{ "hist where item =~ 'a'",
			"hist^(date,item,id) WHERE^(date,item,id) (item =~ 'a')", "" },

		{ "hist where cost =~ 5",
			"hist^(date) WHERE^(date) (cost =~ 5)", "" },

		{ "trans extend x = cost * 1.1",
			"trans^(item) EXTEND x = (cost * 1.1)" },

		{ "trans extend x = 1 extend y = 2",
			"trans^(item) EXTEND x = 1, y = 2" },

		{ "trans extend x = 1 extend y = 2 extend z = 3",
			"trans^(item) EXTEND x = 1, y = 2, z = 3" },

		{ "hist extend x = item $ id",
			"hist^(date) EXTEND x = (item $ id)" },

		{ "inven extend x = -qty sort x",
			"inven^(item) EXTEND x = - qty TEMPINDEX(x)" },

		{ "inven extend x = (qty = 2 ? 222 : qty)",
			"inven^(item) EXTEND x = ((qty is 2) ? 222 : qty)" },

		{ "inven extend x = qty where x > 4",
			"inven^(item) WHERE^(item) EXTEND x = qty" },

		{ "trans summarize item, total cost",
			"trans^(item) SUMMARIZE-SEQ ^(item) (item) total_cost = total cost" },

		{ "trans summarize item, x = total cost",
			"trans^(item) SUMMARIZE-SEQ ^(item) (item) x = total cost" },

		{ "trans summarize total cost",
			"trans^(item) SUMMARIZE-COPY total_cost = total cost" },

		{ "(inven leftjoin trans) where date = 960204",
			"(inven^(item) LEFTJOIN 1:n on (item) trans^(item)) " +
				"WHERE (date is 960204)" },

	};

}
