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
			// System.out.println("CASE " + c[0]);
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
		{
					"customer", // table
			"id	name	city\n" +
			"'a'	'axon'	'saskatoon'\n" +
			"'c'	'calac'	'calgary'\n" +
			"'e'	'emerald'	'vancouver'\n" +
			"'i'	'intercon'	'saskatoon'\n" },
		{
					"customer sort city", // tempindex1
			"id	name	city\n" +
			"'c'	'calac'	'calgary'\n" +
			"'a'	'axon'	'saskatoon'\n" +
			"'i'	'intercon'	'saskatoon'\n" +
			"'e'	'emerald'	'vancouver'\n" },
		{
					"customer project id,city", // copy
			"id	city\n" +
			"'a'	'saskatoon'\n" +
			"'c'	'calgary'\n" +
			"'e'	'vancouver'\n" +
			"'i'	'saskatoon'\n" },
		{ "trans project item", // sequential
			"item\n" +
			"'disk'\n" +
			"'eraser'\n" +
			"'mouse'\n" },
	};
}
