package suneido.database.query;

import static org.junit.Assert.assertEquals;
import static suneido.database.Database.theDB;
import static suneido.database.query.Query.Dir.NEXT;

import java.util.List;

import org.junit.Test;

import suneido.database.Transaction;

public class ExecuteTest extends TestBase {
	@Test
	public void test() {
		for (String[] c : cases) {
			System.out.println("CASE " + c[0]);
			Query q = ParseQuery.parse(c[0]).setup();
			Transaction t = theDB.readonlyTran();
			try {
				q.setTransaction(t);
				assertEquals(c[0], c[1], execute(q));
			} finally {
				t.complete();
			}
		}
	}

	private Object execute(Query q) {
		StringBuilder sb = new StringBuilder();
		Header hdr = q.header();
		List<String> columns = hdr.columns();
		for (String f : columns)
			sb.append(f).append("\t");
		sb.deleteCharAt(sb.length() - 1);
		sb.append("\n");
		Row row;
		while (null != (row = q.get(NEXT))) {
			for (String f : columns)
				sb.append(row.getval(hdr, f)).append("\t");
			sb.deleteCharAt(sb.length() - 1);
			sb.append("\n");
		}
		return sb.toString();
	}

	private static String[][] cases = {
		{ "customer",
			"id	name	city\n" +
			"'a'	'axon'	'saskatoon'\n" +
			"'c'	'calac'	'calgary'\n" +
			"'e'	'emerald'	'vancouver'\n" +
			"'i'	'intercon'	'saskatoon'\n" },
		{ "hist",
			"date	item	id	cost\n" +
			"970101	'disk'	'a'	100\n" +
			"970101	'disk'	'e'	200\n" +
			"970102	'mouse'	'c'	200\n" +
			"970103	'pencil'	'e'	300\n" },
		{ "trans",
			"item	id	cost	date\n" +
			"'disk'	'a'	100	970101\n" +
			"'eraser'	'c'	150	970201\n" +
			"'mouse'	'e'	200	960204\n" +
			"'mouse'	'c'	200	970101\n" },
		{ "customer sort city", // tempindex1
			"id	name	city\n" +
			"'c'	'calac'	'calgary'\n" +
			"'a'	'axon'	'saskatoon'\n" +
			"'i'	'intercon'	'saskatoon'\n" +
			"'e'	'emerald'	'vancouver'\n" },
		{ "trans project item", // sequential
			"item\n" +
			"'disk'\n" +
			"'eraser'\n" +
			"'mouse'\n" },
		{ "customer project city", // lookup
			"city\n" +
			"'saskatoon'\n" +
			"'calgary'\n" +
			"'vancouver'\n" },
		{ "trans extend newcost = cost * 1.1",
			"item	id	cost	date	newcost\n" +
			"'disk'	'a'	100	970101	110\n" +
			"'eraser'	'c'	150	970201	165\n" +
			"'mouse'	'e'	200	960204	220\n" +
			"'mouse'	'c'	200	970101	220\n" },
		{ "trans extend x = cost * 1.1, y = x $ '*'",
			"item	id	cost	date	x	y\n" +
			"'disk'	'a'	100	970101	110	'110*'\n" +
			"'eraser'	'c'	150	970201	165	'165*'\n" +
			"'mouse'	'e'	200	960204	220	'220*'\n" +
			"'mouse'	'c'	200	970101	220	'220*'\n" },
		{ "customer times inven",
			"id	name	city	item	qty\n" +
			"'a'	'axon'	'saskatoon'	'disk'	5\n" +
			"'a'	'axon'	'saskatoon'	'mouse'	2\n" +
			"'a'	'axon'	'saskatoon'	'pencil'	7\n" +
			"'c'	'calac'	'calgary'	'disk'	5\n" +
			"'c'	'calac'	'calgary'	'mouse'	2\n" +
			"'c'	'calac'	'calgary'	'pencil'	7\n" +
			"'e'	'emerald'	'vancouver'	'disk'	5\n" +
			"'e'	'emerald'	'vancouver'	'mouse'	2\n" +
			"'e'	'emerald'	'vancouver'	'pencil'	7\n" +
			"'i'	'intercon'	'saskatoon'	'disk'	5\n" +
			"'i'	'intercon'	'saskatoon'	'mouse'	2\n" +
			"'i'	'intercon'	'saskatoon'	'pencil'	7\n" },
		{ "trans intersect hist",
			"item	id	cost	date\n" +
			"'disk'	'a'	100	970101\n" },
		{ "trans minus hist",
			"item	id	cost	date\n" +
			"'mouse'	'e'	200	960204\n" +
			"'mouse'	'c'	200	970101\n" +
			"'eraser'	'c'	150	970201\n" },
		{ "trans union hist", // merge
			"item	id	cost	date\n" +
			"'mouse'	'e'	200	960204\n" +
			"'disk'	'a'	100	970101\n" +
			"'disk'	'e'	200	970101\n" +
			"'mouse'	'c'	200	970101\n" +
			"'mouse'	'c'	200	970102\n" +
			"'pencil'	'e'	300	970103\n" +
			"'eraser'	'c'	150	970201\n" },
		{ "hist union hist2", // lookup
			"date	item	id	cost\n" +
			"970102	'disk'	'e'	200\n" +
			"970101	'disk'	'a'	100\n" +
			"970101	'disk'	'e'	200\n" +
			"970102	'mouse'	'c'	200\n" +
			"970103	'pencil'	'e'	300\n" },
		{ "hist join customer",
			"date	item	id	cost	name	city\n" +
			"970101	'disk'	'a'	100	'axon'	'saskatoon'\n" +
			"970101	'disk'	'e'	200	'emerald'	'vancouver'\n" +
			"970102	'mouse'	'c'	200	'calac'	'calgary'\n" +
			"970103	'pencil'	'e'	300	'emerald'	'vancouver'\n" },
		{ "trans join inven",
			"item	qty	id	cost	date\n" +
			"'disk'	5	'a'	100	970101\n" +
			"'mouse'	2	'e'	200	960204\n" +
			"'mouse'	2	'c'	200	970101\n" },
		{ "customer join alias",
			"id	name2	name	city\n" +
			"'a'	'abc'	'axon'	'saskatoon'\n" +
			"'c'	'trical'	'calac'	'calgary'\n" },
		{ "customer join supplier",
			"id	name	city	supplier\n" },
		{ "inven leftjoin trans",
			"item	qty	id	cost	date\n" +
			"'disk'	5	'a'	100	970101\n" +
			"'mouse'	2	'e'	200	960204\n" +
			"'mouse'	2	'c'	200	970101\n" +
			"'pencil'	7	''	''	''\n" },
		{ "customer leftjoin hist2",
			"id	name	city	date	item	cost\n" +
			"'a'	'axon'	'saskatoon'	970101	'disk'	100\n" +
			"'c'	'calac'	'calgary'	''	''	''\n" +
			"'e'	'emerald'	'vancouver'	970102	'disk'	200\n" +
			"'e'	'emerald'	'vancouver'	970103	'pencil'	300\n" +
			"'i'	'intercon'	'saskatoon'	''	''	''\n" },
		{ "customer where id > 'd'",
			"id	name	city\n" +
			"'e'	'emerald'	'vancouver'\n" +
			"'i'	'intercon'	'saskatoon'\n" },
	};
}
