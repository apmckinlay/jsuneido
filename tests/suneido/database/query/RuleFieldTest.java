package suneido.database.query;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class RuleFieldTest extends TestBase {

	@Override
	protected void makeDB() {
	}

	@Test
	public void rule_fields_not_saved() {
		adm("create withrule (a,B) key(a)");
		req("insert { a: 1, b: 2 } into withrule");

		Query q = CompileQuery.query(db, serverData, "withrule");
		Header hdr = q.header();
		assertEquals("[b, a]", hdr.columns().toString());
		assertEquals("[a]", hdr.fields().toString());
		assertEquals("[b]", hdr.rules().toString());
	}

}
