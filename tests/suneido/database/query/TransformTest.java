package suneido.database.query;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class TransformTest extends TestBase {

	@Test
	public void test_transform() {
		// combine extend's
		test("customer extend a = 5 extend b = 6",
				"customer EXTEND a = 5, b = 6");
		// combine project's
		test("customer project id, name project id", "customer PROJECT-COPY (id)");
		// combine rename's
		test("customer rename id to x rename name to y",
				"customer RENAME id to x, name to y");
		// combine where's
		test("customer where id = 5 where city = 6",
				"customer WHERE ((id is 5) and (city is 6))");

		// remove projects of all fields
		test("customer project id, city, name", "customer");
		// remove disjoint difference
		test("(customer where id = 3) minus (customer where id = 5)",
				"customer WHERE (id is 3)");
		// remove empty extends
		test("customer extend zone = 3 project id, city",
				"customer PROJECT-COPY (id,city)");
		// remove empty renames
		test("customer rename name to nom project id, city",
				"customer PROJECT-COPY (id,city)");

		// move project before rename
		test("customer rename id to num, name to nom project num, city",
				"customer PROJECT-COPY (id,city) RENAME id to num");
		// move project before rename & remove empty rename
		test("customer rename id to num, name to nom project city",
				"customer PROJECT (city)");
		// move project before extend
		test("customer extend a = 5, b = 6 project id, a, name",
				"customer PROJECT-COPY (id,name) EXTEND a = 5");
		// ... but not if extend uses fields not in project
		test("customer extend a = city, b = 6 project id, a, name",
				"customer EXTEND a = city, b = 6 PROJECT-COPY (id,a,name)");
		// move project before extend & remove empty extend
		test("customer extend a = 5, b = 6 project id, name",
				"customer PROJECT-COPY (id,name)");
		// move where before project
		test("trans project id,cost where id = 5",
				"trans WHERE (id is 5) PROJECT (id,cost)");
		// move where before rename
		test("trans where cost = 200 rename cost to x where id is 5",
				"trans WHERE ((cost is 200) and (id is 5)) RENAME cost to x");
		// move where before extend
		test("trans where cost = 200 extend x = 1 where id is 5",
				"trans WHERE ((cost is 200) and (id is 5)) EXTEND x = 1");
		// move where before summarize
		test("hist summarize id, total cost where id = 3 and total_cost > 10",
				"hist WHERE (id is 3) SUMMARIZE (id) total_cost = total cost "
						+ "WHERE (total_cost > 10)");

		// distribute where over intersect
		test("(hist intersect trans) where cost > 10",
				"(hist WHERE (cost > 10) INTERSECT trans WHERE (cost > 10))");
		// distribute where over difference
		test("(hist minus trans) where cost > 10",
				"(hist WHERE (cost > 10) MINUS trans WHERE (cost > 10))");
		// distribute where over union
		test("(hist union trans) where cost > 10",
				"(hist WHERE (cost > 10) UNION trans WHERE (cost > 10))");
		// distribute where over leftjoin
		test("(customer leftjoin trans) where id = 5",
				"(customer WHERE (id is 5) LEFTJOIN 1:n on (id) trans)");
		// distribute where over leftjoin
		test("(customer leftjoin trans) where id = 5 and item = 3",
				"(customer WHERE (id is 5) LEFTJOIN 1:n on (id) trans) WHERE (item is 3)");
		// distribute where over join
		test("(customer join trans) where cost > 10 and city =~ 'toon'",
				"(customer WHERE (city =~ 'toon') JOIN 1:n on (id) trans WHERE (cost > 10))");
		// distribute where over product
		test("(customer times inven) where qty > 10 and city =~ 'toon'",
				"(customer WHERE (city =~ 'toon') TIMES inven WHERE (qty > 10))");

		// distribute project over union
		test("(hist union trans) project item, cost",
				"(hist PROJECT (item,cost) UNION trans PROJECT (item,cost))");
		// split project over product
		test("(customer times inven) project city, item, id",
				"(customer PROJECT-COPY (city,id) TIMES inven PROJECT-COPY (item))");
		// split project over join
		test("(trans join customer) project city, item, id",
				"(trans PROJECT (item,id) JOIN n:1 on (id) customer PROJECT-COPY (city,id))");
		// ... but only if project includes join fields
		test("(trans join customer) project city, item",
				"(trans JOIN n:1 on (id) customer) PROJECT (city,item)");
	}

	private void test(String from, String to) {
		Query q = CompileQuery.parse(serverData, from);
		q = q.transform();
		assertEquals(to, q.toString());
	}

}
