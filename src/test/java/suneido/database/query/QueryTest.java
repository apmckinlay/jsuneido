/* Copyright 2008 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido.database.query;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.not;
import static org.junit.Assert.assertEquals;
import static suneido.database.query.Query.Dir.NEXT;

import java.util.Collections;
import java.util.List;

import org.junit.Test;

import suneido.database.query.Query.Dir;
import suneido.intfc.database.Transaction;
import suneido.language.Ops;

import com.google.common.collect.Lists;

public class QueryTest extends TestBase {

	@Test
	public void address() {
		makeTable("test", 1);
		Transaction t = db.readTransaction();
		try {
			Query q = CompileQuery.query(t, serverData, "test");
			Row row = q.get(NEXT);
			assertThat(row.address(), not(equalTo(0)));
			t.complete();
		} finally {
			t.abortIfNotComplete();
		}
	}

	@Test
	public void tests() {
		makeDB();

		test1("customer",
			"id	name	city\n" +
			"'a'	'axon'	'saskatoon'\n" +
			"'c'	'calac'	'calgary'\n" +
			"'e'	'emerald'	'vancouver'\n" +
			"'i'	'intercon'	'saskatoon'\n");
		test1("customer where id = 'e'",
			"id	name	city\n" +
			"'e'	'emerald'	'vancouver'\n");
		test1("hist",
			"date	item	id	cost\n" +
			"970101	'disk'	'a'	100\n" +
			"970101	'disk'	'e'	200\n" +
			"970102	'mouse'	'c'	200\n" +
			"970103	'pencil'	'e'	300\n");
		test1("trans",
			"item	id	cost	date\n" +
			"'disk'	'a'	100	970101\n" +
			"'eraser'	'c'	150	970201\n" +
			"'mouse'	'e'	200	960204\n" +
			"'mouse'	'c'	200	970101\n");
		test1("trans rename id to code, date to when",
			"item	code	cost	when\n" +
			"'disk'	'a'	100	970101\n" +
			"'eraser'	'c'	150	970201\n" +
			"'mouse'	'e'	200	960204\n" +
			"'mouse'	'c'	200	970101\n");
		test1("customer sort city", // tempindex1
			"id	name	city\n" +
			"'c'	'calac'	'calgary'\n" +
			"'a'	'axon'	'saskatoon'\n" +
			"'i'	'intercon'	'saskatoon'\n" +
			"'e'	'emerald'	'vancouver'\n");
		test1("customer sort reverse city", // tempindex1
			"id	name	city\n" +
			"'e'	'emerald'	'vancouver'\n" +
			"'i'	'intercon'	'saskatoon'\n" +
			"'a'	'axon'	'saskatoon'\n" +
			"'c'	'calac'	'calgary'\n");
		test1("task sort cnum, tnum",
			"tnum	cnum\n" +
			"100	1\n" +
			"104	1\n" +
			"101	2\n" +
			"105	2\n" +
			"102	3\n" +
			"106	3\n" +
			"103	4\n" +
			"107	4\n");
		test1("trans project item", // sequential
			"item\n" +
			"'disk'\n" +
			"'eraser'\n" +
			"'mouse'\n");
		test1("customer project city", // lookup
			"city\n" +
			"'saskatoon'\n" +
			"'calgary'\n" +
			"'vancouver'\n");
		test1("trans extend newcost = cost * 1.1",
			"item	id	cost	date	newcost\n" +
			"'disk'	'a'	100	970101	110\n" +
			"'eraser'	'c'	150	970201	165\n" +
			"'mouse'	'e'	200	960204	220\n" +
			"'mouse'	'c'	200	970101	220\n");
		test1("trans extend x = cost * 1.1, y = x $ '*'",
			"item	id	cost	date	x	y\n" +
			"'disk'	'a'	100	970101	110	'110*'\n" +
			"'eraser'	'c'	150	970201	165	'165*'\n" +
			"'mouse'	'e'	200	960204	220	'220*'\n" +
			"'mouse'	'c'	200	970101	220	'220*'\n");
		test1("customer times inven",
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
			"'i'	'intercon'	'saskatoon'	'pencil'	7\n");
		test1("trans intersect hist",
			"item	id	cost	date\n" +
			"'disk'	'a'	100	970101\n");
		test1("trans minus hist sort date",
			"item	id	cost	date\n" +
			"'mouse'	'e'	200	960204\n" +
			"'mouse'	'c'	200	970101\n" +
			"'eraser'	'c'	150	970201\n");
		test1("(trans minus hist) where id = 9",
			"item	id	cost	date\n");
		test1("trans union hist", // merge
			"item	id	cost	date\n" +
			"'mouse'	'e'	200	960204\n" +
			"'disk'	'a'	100	970101\n" +
			"'disk'	'e'	200	970101\n" +
			"'mouse'	'c'	200	970101\n" +
			"'mouse'	'c'	200	970102\n" +
			"'pencil'	'e'	300	970103\n" +
			"'eraser'	'c'	150	970201\n");
		test1("hist2 union hist", // lookup
			"date	item	id	cost\n" +
			"970102	'disk'	'e'	200\n" +
			"970101	'disk'	'a'	100\n" +
			"970101	'disk'	'e'	200\n" +
			"970102	'mouse'	'c'	200\n" +
			"970103	'pencil'	'e'	300\n");
		test1("hist join customer",
			"date	item	id	cost	name	city\n" +
			"970101	'disk'	'a'	100	'axon'	'saskatoon'\n" +
			"970101	'disk'	'e'	200	'emerald'	'vancouver'\n" +
			"970102	'mouse'	'c'	200	'calac'	'calgary'\n" +
			"970103	'pencil'	'e'	300	'emerald'	'vancouver'\n");
		test1("trans join inven",
			"item	qty	id	cost	date\n" +
			"'disk'	5	'a'	100	970101\n" +
			"'mouse'	2	'e'	200	960204\n" +
			"'mouse'	2	'c'	200	970101\n");
		test1("customer join alias",
			"id	name2	name	city\n" +
			"'a'	'abc'	'axon'	'saskatoon'\n" +
			"'c'	'trical'	'calac'	'calgary'\n");
		test1("customer join supplier",
			"supplier	name	city	id\n");
		test1("inven leftjoin trans",
			"item	qty	id	cost	date\n" +
			"'disk'	5	'a'	100	970101\n" +
			"'mouse'	2	'e'	200	960204\n" +
			"'mouse'	2	'c'	200	970101\n" +
			"'pencil'	7	''	''	''\n");
		test1("customer leftjoin hist2",
			"id	name	city	date	item	cost\n" +
			"'a'	'axon'	'saskatoon'	970101	'disk'	100\n" +
			"'c'	'calac'	'calgary'	''	''	''\n" +
			"'e'	'emerald'	'vancouver'	970102	'disk'	200\n" +
			"'e'	'emerald'	'vancouver'	970103	'pencil'	300\n" +
			"'i'	'intercon'	'saskatoon'	''	''	''\n");
		test1("customer where id > 'd'",
			"id	name	city\n" +
			"'e'	'emerald'	'vancouver'\n" +
			"'i'	'intercon'	'saskatoon'\n");
		test1("customer where id > 'd' and id < 'j'",
			"id	name	city\n" +
			"'e'	'emerald'	'vancouver'\n" +
			"'i'	'intercon'	'saskatoon'\n");
		test1("customer where id = 'e'",
			"id	name	city\n" +
			"'e'	'emerald'	'vancouver'\n");
		test1("customer where id = 'd'",
			"id	name	city\n");
		test1("inven where qty > 0",
			"item	qty\n" +
			"'disk'	5\n" +
			"'mouse'	2\n" +
			"'pencil'	7\n");
		test1("inven where item =~ 'i'",
			"item	qty\n" +
			"'disk'	5\n" +
			"'pencil'	7\n");
		test1("inven where item =~ 'i'",
			"item	qty\n" +
			"'disk'	5\n" +
			"'pencil'	7\n");
		test1("inven where item in ('disk', 'mouse', 'pencil')",
			"item	qty\n" +
			"'disk'	5\n" +
			"'mouse'	2\n" +
			"'pencil'	7\n");
		test1("inven where item <= 'e' or item >= 'p'",
			"item	qty\n" +
			"'disk'	5\n" +
			"'pencil'	7\n");
		test1("cus where cnum = 2 and abbrev = 'b'",
			"cnum	abbrev	name\n" +
			"2	'b'	'bill'\n");
		test1("cus where cnum = 2 and abbrev >= 'b' and abbrev < 'c'",
			"cnum	abbrev	name\n" +
			"2	'b'	'bill'\n");
		test1("hist summarize count",
			"count\n" +
			"4\n");
		test1("hist summarize min cost, average cost, max cost, sum = total cost",
			"min_cost	average_cost	max_cost	sum\n" +
			"100	200	300	800\n");
		test1("hist summarize item, total cost",
			"item	total_cost\n" +
			"'disk'	300\n" +
			"'mouse'	200\n" +
			"'pencil'	300\n");
		test1("hist summarize date, list id",
			"date	list_id\n" +
			"970101	#('a', 'e')\n" +
			"970102	#('c')\n" +
			"970103	#('e')\n");
		test1("hist summarize list id",
			"list_id\n" +
			"#('a', 'c', 'e')\n");

		test1("customer where !(id in ())",
				"id	name	city\n" +
				"'a'	'axon'	'saskatoon'\n" +
				"'c'	'calac'	'calgary'\n" +
				"'e'	'emerald'	'vancouver'\n" +
				"'i'	'intercon'	'saskatoon'\n");
	}

	private void test1(String query, String result) {
		one_way(Dir.NEXT, query, result);
		one_way(Dir.PREV, query, result);
	}

	private void one_way(Dir dir, String query, String result) {
		Transaction t = db.readTransaction();
		try {
			Query q = CompileQuery.query(t, serverData, query);
			assertEquals(q.toString(), result, execute(dir, q));
			t.complete();
		} finally {
			t.abortIfNotComplete();
		}
	}

	private static Object execute(Dir dir, Query q) {
		StringBuilder sb = new StringBuilder();
		Header hdr = q.header();
		List<String> columns = hdr.columns();
		for (String f : columns)
			sb.append(f).append("\t");
		sb.deleteCharAt(sb.length() - 1);
		sb.append("\n");
		List<Row> rows = Lists.newArrayList();
		Row row;
		while (null != (row = q.get(dir)))
			rows.add(row);
		if (dir == Dir.PREV)
			Collections.reverse(rows);
		for (Row r : rows) {
			for (String f : columns)
				sb.append(Ops.display(r.getval(hdr, f))).append("\t");
			sb.deleteCharAt(sb.length() - 1);
			sb.append("\n");
		}
		return sb.toString();
	};
}
