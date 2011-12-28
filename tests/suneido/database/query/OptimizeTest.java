package suneido.database.query;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static suneido.Suneido.dbpkg;

import org.junit.Test;

import suneido.intfc.database.Transaction;

public class OptimizeTest extends TestBase {

	@Test
	public void table_index_selection() {
		adm("create test (a,b,c) index(a) key(b) index(c)");
		String big = "now is the time for all good men";
		Transaction t = db.readwriteTran();
		for (int i = 0; i < 100; ++i)
			t.addRecord("test", dbpkg.recordBuilder().add(big).add(i).add(big).build());
		t.ck_complete();
		test1("test", "test^(b)");
	}

	@Test
	public void test() {
		makeDB();
		adm("create test_minus1 (a, b, c) key(a)");
		adm("create test_minus2 (b, c, d) key(d)");

		test1("customer",
			"customer^(id)");

		test1("inven",
			"inven^(item)");

		test1("trans",
			"trans^(item)");

		test1( "hist",
			"hist^(date)");

		test1( "task",
			"task^(tnum)");

		test1( "alias",
			"alias^(id)");

		test1( "supplier",
			"supplier^(city)",
			"supplier^(supplier)");

		test1( "customer project id,name",
			"customer^(id) PROJECT-COPY (id,name)");

		test1( "trans project item",
			"trans^(item) PROJECT-SEQ^(item) (item)");

		test1( "trans project item,id,cost,date project item",
			"trans^(item) PROJECT-SEQ^(item) (item)");

		test1( "trans project item,id,cost project item,id project item",
			"trans^(item) PROJECT-SEQ^(item) (item)");

		test1( "hist project date,item",
			"hist^(date,item,id) PROJECT-SEQ^(date,item,id) (date,item)");

		test1( "customer project city",
			"customer^(id) PROJECT-LOOKUP (city)");

		test1( "customer project id,city project city",
			"customer^(id) PROJECT-LOOKUP (city)");

		test1( "customer rename city to location",
			"customer^(id) RENAME city to location");

		test1( "customer rename city to location rename location to loc",
			"customer^(id) RENAME city to loc");

		test1( "customer rename id to i rename name to n rename city to c",
			"customer^(id) RENAME id to i, name to n, city to c");

		test1( "inven rename item to part, qty to onhand",
			"inven^(item) RENAME item to part, qty to onhand");

		test1( "inven rename item to part rename qty to onhand",
			"inven^(item) RENAME item to part, qty to onhand");

		test1( "inven rename qty to x where x > 4",
			"inven^(item) WHERE^(item) RENAME qty to x");

		test1( "hist union trans",
			"(hist^(date,item,id) UNION-MERGE^(date,item,id) trans^(date,item,id))");

		test1( "hist union hist",
			"(hist^(date,item,id) UNION-MERGE^(date,item,id) hist^(date,item,id))");

		test1( "trans union hist",
			"(trans^(date,item,id) UNION-MERGE^(date,item,id) hist^(date,item,id))");

		test1( "hist union hist2",
			"(hist2^(date) UNION-LOOKUP^(date,item,id) hist^(date,item,id))",
			"(hist^(date) UNION-LOOKUP^(date) hist2^(date))");

		test1( "(trans where cost=100) union (trans where cost=200)",
			"(trans^(item) WHERE^(item) UNION-DISJOINT(cost) trans^(item) WHERE^(item))");

		test1( "hist minus trans",
			"(hist^(date,item,id) MINUS^(date,item,id) trans^(date,item,id))",
			"(hist^(date) MINUS^(date,item,id) trans^(date,item,id))");

		test1( "trans minus hist",
			"(trans^(date,item,id) MINUS^(date,item,id) hist^(date,item,id))",
			"(trans^(item) MINUS^(date,item,id) hist^(date,item,id))");

		test1( "(test_minus1 minus test_minus2) where a is 1",
			"(test_minus1^(a) WHERE^(a) MINUS^(d) test_minus2 WHERE nothing)");

		test1( "trans intersect hist",
			"(trans^(date,item,id) INTERSECT^(date,item,id) hist^(date,item,id))",
			"(trans^(item) INTERSECT^(date,item,id) hist^(date,item,id))");

		test1( "hist2 intersect trans",
			"(trans^(date,item,id) INTERSECT^(date) hist2^(date))",
			"(trans^(item) INTERSECT^(date) hist2^(date))");

		test1( "(hist where item = 1) intersect (trans where item = 2)",
			"(hist WHERE (item is 1) INTERSECT-DISJOINT(item) trans WHERE (item is 2))");

		test1( "cus where cnum = 2 and abbrev = 'c'",
			"cus^(abbrev) WHERE^(abbrev)");

		test1( "(trans union trans) intersect (hist union hist)",
			"((trans^(date,item,id) " +
				"UNION-MERGE^(date,item,id) trans^(date,item,id)) " +
				"INTERSECT^(date,item,id,cost) (hist^(date,item,id) " +
				"UNION-MERGE^(date,item,id) hist^(date,item,id)) " +
				"TEMPINDEX(date,item,id,cost) unique)");

		test1( "(trans minus hist) union (trans intersect hist)",
				"((trans^(date,item,id) " +
				"MINUS^(date,item,id) hist^(date,item,id)) " +
				"UNION-MERGE^(date,item,id) (trans^(date,item,id) " +
				"INTERSECT^(date,item,id) hist^(date,item,id)))");

		test1( "customer times inven",
			"(customer^(id) TIMES inven^(item))");

		test1( "inven times customer",
			"(inven^(item) TIMES customer^(id))");

		test1( "(customer times inven) join trans",
			"((customer^(id) TIMES inven^(item)) JOIN 1:n on (id,item) " +
				"trans^(date,item,id) TEMPINDEX(id,item))",
			"((customer^(id) TIMES inven^(item)) JOIN 1:n on (id,item) " +
				"trans^(item) TEMPINDEX(id,item))");

		test1( "hist join customer",
			"(hist^(date,item,id) JOIN n:1 on (id) customer^(id))",
			"(hist^(date) JOIN n:1 on (id) customer^(id))");

		test1( "customer join hist",
			"(hist^(date,item,id) JOIN n:1 on (id) customer^(id))",
			"(hist^(date) JOIN n:1 on (id) customer^(id))");

		test1( "trans join inven",
			"(inven^(item) JOIN 1:n on (item) trans^(item))");

		test1( "task join co",
			"(co^(tnum) JOIN 1:1 on (tnum) task^(tnum))");

		test1( "(trans union trans) join (inven union inven)",
			"((trans^(date,item,id) " +
				"UNION-MERGE^(date,item,id) trans^(date,item,id)) " +
				"JOIN n:n on (item) (inven^(item) " +
				"UNION-MERGE^(item) inven^(item)))");

		test1( "customer join alias",
			"(alias^(id) JOIN 1:1 on (id) customer^(id))");

		test1( "customer join supplier",
			"(supplier^(city) JOIN n:n on (name,city) customer^(id) " +
				"TEMPINDEX(name,city))",
			"(supplier^(supplier) JOIN n:n on (name,city) customer^(id) " +
				"TEMPINDEX(name,city))");

		test1( "trans join customer join inven",
			"((trans^(date,item,id) JOIN n:1 on (id) customer^(id)) " +
				"JOIN n:1 on (item) inven^(item))",
			"((trans^(item) JOIN n:1 on (id) customer^(id)) " +
				"JOIN n:1 on (item) inven^(item))");

		test1( "(trans join customer) union (hist join customer)",
			"((trans^(date,item,id) JOIN n:1 on (id) customer^(id)) " +
				"UNION-MERGE^(date,item,id) (hist^(date,item,id) " +
				"JOIN n:1 on (id) customer^(id)))");

		test1( "(trans join customer) intersect (hist join customer)",
			"((trans^(date,item,id) JOIN n:1 on (id) customer^(id)) " +
				"INTERSECT^(date,item,id) (hist^(date,item,id) " +
				"JOIN n:1 on (id) customer^(id)))",
			"((trans^(item) JOIN n:1 on (id) customer^(id)) " +
				"INTERSECT^(date,item,id) (hist^(date,item,id) " +
				"JOIN n:1 on (id) customer^(id)))");

		test1( "(((task join co)) join (cus where abbrev = 'a'))",
			"((co^(tnum) JOIN 1:1 on (tnum) task^(tnum)) "
				+ "JOIN n:1 on (cnum) cus^(cnum) WHERE^(cnum))");

		test1( "((task join (co where signed = 990103)) join (cus where abbrev = 'a'))",
			"((co^(tnum) WHERE^(tnum) JOIN 1:1 on (tnum) task^(tnum)) "
				+ "JOIN n:1 on (cnum) cus^(cnum) WHERE^(cnum))");

		test1( "inven leftjoin trans",
			"(inven^(item) LEFTJOIN 1:n on (item) trans^(item))");

		test1( "customer leftjoin hist2",
					"(customer^(id) LEFTJOIN 1:n on (id) hist2^(id))");

		test1( "customer leftjoin hist2 sort date",
					"(customer^(id) LEFTJOIN 1:n on (id) hist2^(id)) TEMPINDEX(date)");

		test1( "inven where qty + 1 > 5",
			"inven^(item) WHERE^(item) ((qty + 1) > 5)");

		test1( "trans where \"mousee\" = item $ id project date, item, id",
			"trans^(date,item,id) " +
				"WHERE^(date,item,id) ('mousee' is (item $ id)) PROJECT-COPY (date,item,id)");

		test1( "inven where qty + 1 in (3,8)",
			"inven^(item) WHERE^(item) (qty + 1) in (3,8)");

		test1( "inven where qty + 1 in (33)",
			"inven^(item) WHERE^(item) (qty + 1) in (33)");

		test1( "trans where cost=100",
			"trans^(item) WHERE^(item)");

		test1( "cus where abbrev = 'a'",
			"cus^(abbrev) WHERE^(abbrev)");

		test1( "hist where item =~ 'a'",
			"hist^(date,item,id) WHERE^(date,item,id) (item =~ 'a')");

		test1( "hist where cost =~ 5",
			"hist^(date) WHERE^(date) (cost =~ 5)");

		test1( "trans extend x = cost * 1.1",
			"trans^(item) EXTEND x = (cost * 1.1)");

		test1( "trans extend x = 1 extend y = 2",
			"trans^(item) EXTEND x = 1, y = 2");

		test1( "trans extend x = 1 extend y = 2 extend z = 3",
			"trans^(item) EXTEND x = 1, y = 2, z = 3");

		test1( "hist extend x = item $ id",
			"hist^(date) EXTEND x = (item $ id)");

		test1( "inven extend x = -qty sort x",
			"inven^(item) EXTEND x = - qty TEMPINDEX(x)");

		test1( "inven extend x = (qty = 2 ? 222 : qty)",
			"inven^(item) EXTEND x = ((qty is 2) ? 222 : qty)");

		test1( "inven extend x = qty where x > 4",
			"inven^(item) WHERE^(item) EXTEND x = qty");

		test1( "trans summarize item, total cost",
			"trans^(item) SUMMARIZE-SEQ ^(item) (item) total_cost = total cost");

		test1( "trans summarize item, x = total cost",
			"trans^(item) SUMMARIZE-SEQ ^(item) (item) x = total cost");

		test1( "trans summarize total cost",
			"trans^(item) SUMMARIZE-COPY total_cost = total cost");

		test1( "(inven leftjoin trans) where date = 960204",
			"(inven^(item) LEFTJOIN 1:n on (item) trans^(item)) " +
				"WHERE (date is 960204)");
	}

	private void test1(String query, String strategy) {
		Query q = CompileQuery.query(db, serverData, query);
		assertEquals(strategy, q.toString());
	}

	private void test1(String query, String strategy1, String strategy2) {
		Query q = CompileQuery.query(db, serverData, query);
		String actual = q.toString();
		assertTrue(actual, actual.equals(strategy1) || actual.equals(strategy2));
	}

}
