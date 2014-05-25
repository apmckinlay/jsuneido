/* Copyright 2010 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido.database.query;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertEquals;
import static suneido.Suneido.dbpkg;
import static suneido.database.query.Query.Dir.NEXT;
import static suneido.database.query.Query.Dir.PREV;

import java.util.Arrays;
import java.util.List;

import org.junit.Test;

import suneido.Suneido;
import suneido.database.server.ServerData;
import suneido.intfc.database.Database;
import suneido.intfc.database.Transaction;
import suneido.language.Compiler;

public class ProjectTest {
	private final ServerData serverData = new ServerData();
	private final Database db = dbpkg.testdb();

	@Test
	public void project_extend_1() {
		Request.execute(db, "create tmp (a,b,c) key(a) index(b)");
		test("tmp extend r project b", list("b"), "tmp^(b) PROJECT-SEQ^(b) (b)");
		test("tmp extend r project c", list("c"), "tmp^(a) PROJECT-LOOKUP (c)");
		test("tmp extend d=1 project a", list("a"), "tmp^(a) PROJECT-COPY (a)");
		test("tmp extend d=1,r project a,d", list("a", "d"), "tmp^(a) PROJECT-COPY (a) EXTEND d = 1"); // moved
		test("tmp extend r project c,r", list("c", "r"), "tmp^(a) EXTEND r PROJECT-LOOKUP (c,r)"); // NOT moved
	}

	private void test(String query, List<String> cols, String strategy) {
		Transaction t = db.readTransaction();
		Query q = CompileQuery.query(t, serverData, query);
		assertThat(q.columns(), equalTo(cols));
		assertThat(q.toString(), equalTo(strategy));
	}

	@Test
	public void project_extend_2() {
		Request.execute(db, "create tmp (a,b,c) key(a) index(b)");
		req("insert {a: 1, b: 11, c: 111 } into tmp");
		req("insert {a: 2, b: 22, c: 222 } into tmp");
		req("insert {a: 3, b: 33, c: 333 } into tmp");
		Suneido.context.set("Rule_r",
				Compiler.compile("Rule_r", "function () { .a + .b + .c }"));

		test("tmp extend r project a, r", "Row{a: 1, r: 1}"); // fails for copy
		test("tmp extend r project b, r", "Row{b: 11, r: 123}");
		test("tmp extend r project c, r", "Row{c: 111, r: 123}");
	}

	private void test(String query, String result) {
		Transaction t = db.readTransaction();
		Query q = CompileQuery.query(t, serverData, query);
		Header hdr = q.header();
		Row row = q.get(NEXT);
		assertThat(row.toString(hdr), equalTo(result));
	}

	@Test
	public void lookup_prev() {
		Request.execute(db, "create tmp (a,b) key(a)");
		req("insert { a: 1 } into tmp");
		req("insert { a: 2, b: 1 } into tmp");
		Transaction t = db.readTransaction();
		Query q = CompileQuery.query(t, serverData, "tmp project b");
		Header hdr = q.header();
		Row row;
		row = q.get(PREV);
		assertEquals(1, row.getval(hdr, "b"));
		row = q.get(PREV);
		assertEquals("", row.getval(hdr, "b"));
		assertEquals(null, q.get(PREV));
	}

	protected int req(String s) {
		Transaction tran = db.updateTransaction();
		try {
			Query q = CompileQuery.parse(tran, serverData, s);
			int n = ((QueryAction) q).execute();
			tran.ck_complete();
			return n;
		} finally {
			tran.abortIfNotComplete();
		}
	}

	List<String> list(String... values) {
		return Arrays.asList(values);
	}

}
