package suneido.database.query;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class QueryTest extends suneido.database.TestBase {
	@Test
	public void test() {
		makeDB();
		for (String[] c : transforms) {
			System.out.println(c[0]);
			Query q = ParseQuery.parse(c[0]).transform();
			assertEquals(c[1], q.toString());
		}
	}

	private void makeDB() {
		adm("create stdlib (group, name, text) key(name,group)");

		// create customer file
		adm("create customer (id, name, city) key(id)");
		req("insert{id: \"a\", name: \"axon\", city: \"saskatoon\"} into customer");
		req("insert{id: \"c\", name: \"calac\", city: \"calgary\"} into customer");
		req("insert{id: \"e\", name: \"emerald\", city: \"vancouver\"} into customer");
		req("insert{id: \"i\", name: \"intercon\", city: \"saskatoon\"} into customer");

		// create hist file
		adm("create hist (date, item, id, cost) index(date) key(date,item,id)");
		req("insert{date: 970101, item: \"disk\", id: \"a\", cost: 100} into hist");
		req("insert{date: 970101, item: \"disk\", id: \"e\", cost: 200} into hist");
		req("insert{date: 970102, item: \"mouse\", id: \"c\", cost: 200} into hist");
		req("insert{date: 970103, item: \"pencil\", id: \"e\", cost: 300} into hist");

		// create hist2 file - for leftjoin test
		adm("create hist2 (date, item, id, cost) key(date) index(id)");
		req("insert{date: 970101, item: \"disk\", id: \"a\", cost: 100} into hist2");
		req("insert{date: 970102, item: \"disk\", id: \"e\", cost: 200} into hist2");
		req("insert{date: 970103, item: \"pencil\", id: \"e\", cost: 300} into hist2");

		// create trans file
		adm("create trans (item, id, cost, date) index(item) key(date,item,id)");
		req("insert{item: \"mouse\", id: \"e\", cost: 200, date: 960204} into trans");
		req("insert{item: \"disk\", id: \"a\", cost: 100, date: 970101} into trans");
		req("insert{item: \"mouse\", id: \"c\", cost: 200, date: 970101} into trans");
		req("insert{item: \"eraser\", id: \"c\", cost: 150, date: 970201} into trans");

		// create supplier file
		adm("create supplier (supplier, name, city) index(city) key(supplier)");
		req("insert{supplier: \"mec\", name: \"mtnequipcoop\", city: \"calgary\"} into supplier");
		req("insert{supplier: \"hobo\", name: \"hoboshop\", city: \"saskatoon\"} into supplier");
		req("insert{supplier: \"ebs\", name: \"ebssail&sport\", city: \"saskatoon\"} into supplier");
		req("insert{supplier: \"taiga\", name: \"taigaworks\", city: \"vancouver\"} into supplier");

		// create inven file
		adm("create inven (item, qty) key(item)");
		req("insert{item: \"disk\", qty: 5} into inven");
		req("insert{item: \"mouse\", qty:2} into inven");
		req("insert{item: \"pencil\", qty: 7} into inven");

		// create alias file
		adm("create alias(id, name2) key(id)");
		req("insert{id: \"a\", name2: \"abc\"} into alias");
		req("insert{id: \"c\", name2: \"trical\"} into alias");

		// create cus, task, co tables
		adm("create cus(cnum, abbrev, name) key(cnum) key(abbrev)");
		req("insert { cnum: 1, abbrev: 'a', name: 'axon' } into cus");
		req("insert { cnum: 2, abbrev: 'b', name: 'bill' } into cus");
		req("insert { cnum: 3, abbrev: 'c', name: 'cron' } into cus");
		req("insert { cnum: 4, abbrev: 'd', name: 'dick' } into cus");
		adm("create task(tnum, cnum) key(tnum)");
		req("insert { tnum: 100, cnum: 1 } into task");
		req("insert { tnum: 101, cnum: 2 } into task");
		req("insert { tnum: 102, cnum: 3 } into task");
		req("insert { tnum: 103, cnum: 4 } into task");
		req("insert { tnum: 104, cnum: 1 } into task");
		req("insert { tnum: 105, cnum: 2 } into task");
		req("insert { tnum: 106, cnum: 3 } into task");
		req("insert { tnum: 107, cnum: 4 } into task");
		adm("create co(tnum, signed) key(tnum)");
		req("insert { tnum: 100, signed: 990101 } into co");
		req("insert { tnum: 102, signed: 990102 } into co");
		req("insert { tnum: 104, signed: 990103 } into co");
		req("insert { tnum: 106, signed: 990104 } into co");

		adm("create dates(date) key(date)");
		req("insert { date: #20010101 } into dates");
		req("insert { date: #20010102 } into dates");
		req("insert { date: #20010301 } into dates");
		req("insert { date: #20010401 } into dates");
	}

	private void adm(String s) {
		Request.execute(s);
	}

	private void req(String s) {
		QueryAction q = (QueryAction) ParseQuery.parse(s);
		q.execute();
	}

	private static String[][] transforms = {
		// combine extend's
		{ "customer extend a = 5 extend b = 6",
			"customer EXTEND a = 5, b = 6" },
		// combine project's
		{ "customer project id, name project id",
			"customer PROJECT id" },
		// combine rename's
		{ "customer rename id to x rename name to y",
			"customer RENAME id to x, name to y" },
		// combine where's
		{ "customer where a = 5 where b = 6",
			"customer WHERE ((a = 5) and (b = 6))" },

		// remove projects of all fields
		{ "customer project id, city, name",
			"customer" },
		// remove disjoint difference
		{ "(customer where id = 3) minus (customer where id = 5)",
			"customer WHERE (id = 3)" },

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
		// move where before rename
		{ "trans where cost = 200 rename cost to x where id is 5",
			"trans WHERE ((cost = 200) and (id = 5)) RENAME cost to x" },
		// move where before extend
		{ "trans where cost = 200 extend x = 1 where id is 5",
			"trans WHERE ((cost = 200) and (id = 5)) EXTEND x = 1" },

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
			"(trans JOIN n:1 on (id) customer) PROJECT city,item" },
	};

	private static String[][] cases = {
	// 0
			{
					"(((task join co)) join (cus where abbrev = 'a'))",
					"((co^(tnum)) JOIN 1:1 on (tnum) (task^(tnum))) JOIN n:1 on (cnum) (cus^(cnum) WHERE^(cnum))",
					"tnum	signed	cnum	abbrev	name\n"
							+ "100	990101	1	\"a\"	\"axon\"\n"
							+ "104	990103	1	\"a\"	\"axon\"\n" },

			// 1
			{
					"((task join (co where signed = 990103)) join (cus where abbrev = 'a'))",
					"((co^(tnum) WHERE^(tnum)) JOIN 1:1 on (tnum) (task^(tnum))) JOIN n:1 on (cnum) (cus^(cnum) WHERE^(cnum))",
					"tnum	signed	cnum	abbrev	name\n"
							+ "104	990103	1	\"a\"	\"axon\"\n" }, };

}
