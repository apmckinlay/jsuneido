package suneido.database.query;

import static org.junit.Assert.assertEquals;
import static suneido.database.Database.theDB;

import org.junit.Test;

public class TransformTest extends TestBase {

	@Test
	public void test() {
		for (String[] c : transforms) {
			// System.out.println("CASE " + c[0]);
			Query q = CompileQuery.parse(theDB.cursorTran(),
					serverData, c[0]).transform();
			assertEquals(c[1], q.toString());
		}
	}

	private static String[][] transforms = {
			// combine extend's
			{ "customer extend a = 5 extend b = 6",
					"customer EXTEND a = 5, b = 6" },
			// combine project's
			{ "customer project id, name project id", "customer PROJECT id" },
			// combine rename's
			{ "customer rename id to x rename name to y",
					"customer RENAME id to x, name to y" },
			// combine where's
			{ "customer where id = 5 where city = 6",
					"customer WHERE ((id is 5) and (city is 6))" },

			// remove projects of all fields
			{ "customer project id, city, name", "customer" },
			// remove disjoint difference
			{ "(customer where id = 3) minus (customer where id = 5)",
					"customer WHERE (id is 3)" },
			// remove empty extends
			{ "customer extend zone = 3 project id, city",
					"customer PROJECT id,city" },
			// remove empty renames
			{ "customer rename name to nom project id, city",
					"customer PROJECT id,city" },

			// move project before rename
			{ "customer rename id to num, name to nom project num, city",
					"customer PROJECT id,city RENAME id to num" },
			// move project before rename & remove empty rename
			{ "customer rename id to num, name to nom project city",
					"customer PROJECT city" },
			// move project before extend
			{ "customer extend a = 5, b = 6 project id, a, name",
					"customer PROJECT id,name EXTEND a = 5" },
			// ... but not if extend uses fields not in project
			{ "customer extend a = city, b = 6 project id, a, name",
					"customer EXTEND a = city, b = 6 PROJECT id,a,name" },
			// move project before extend & remove empty extend
			{ "customer extend a = 5, b = 6 project id, name",
					"customer PROJECT id,name" },
			// move where before project
			{ "trans project id,cost where id = 5",
					"trans WHERE (id is 5) PROJECT id,cost" },
			// move where before rename
			{ "trans where cost = 200 rename cost to x where id is 5",
					"trans WHERE ((cost is 200) and (id is 5)) RENAME cost to x" },
			// move where before extend
			{ "trans where cost = 200 extend x = 1 where id is 5",
					"trans WHERE ((cost is 200) and (id is 5)) EXTEND x = 1" },
			// move where before summarize
			{ "hist summarize id, total cost where id = 3 and total_cost > 10",
					"hist WHERE (id is 3) SUMMARIZE (id) total_cost = total cost "
							+ "WHERE (total_cost > 10)" },

			// distribute where over intersect
			{ "(hist intersect trans) where cost > 10",
					"(hist WHERE (cost > 10) INTERSECT trans WHERE (cost > 10))" },
			// distribute where over difference
			{ "(hist minus trans) where cost > 10",
					"(hist WHERE (cost > 10) MINUS trans WHERE (cost > 10))" },
			// distribute where over union
			{ "(hist union trans) where cost > 10",
					"(hist WHERE (cost > 10) UNION trans WHERE (cost > 10))" },
			// distribute where over leftjoin
			{ "(customer leftjoin trans) where id = 5",
					"(customer WHERE (id is 5) LEFTJOIN 1:n on (id) trans)" },
			// distribute where over leftjoin
			{ "(customer leftjoin trans) where id = 5 and item = 3",
					"(customer WHERE (id is 5) LEFTJOIN 1:n on (id) trans) WHERE (item is 3)" },
			// distribute where over join
			{ "(customer join trans) where cost > 10 and city =~ 'toon'",
					"(customer WHERE (city =~ 'toon') JOIN 1:n on (id) trans WHERE (cost > 10))" },
			// distribute where over product
			{ "(customer times inven) where qty > 10 and city =~ 'toon'",
					"(customer WHERE (city =~ 'toon') TIMES inven WHERE (qty > 10))" },

			// distribute project over union
			{ "(hist union trans) project item, cost",
					"(hist PROJECT item,cost UNION trans PROJECT item,cost)" },
			// split project over product
			{ "(customer times inven) project city, item, id",
					"(customer PROJECT city,id TIMES inven PROJECT item)" },
			// split project over join
			{ "(trans join customer) project city, item, id",
					"(trans PROJECT item,id JOIN n:1 on (id) customer PROJECT city,id)" },
			// ... but only if project includes join fields
			{ "(trans join customer) project city, item",
					"(trans JOIN n:1 on (id) customer) PROJECT city,item" }, };

}
