package suneido.database.query;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;


public class UpdateableTest extends TestBase {
	@Test
	public void test() {
		updateable("tables");
		updateable("tables extend xyz = 123");
		updateable("tables project table");
		not_updateable("columns project table");
		updateable("indexes sort columns");
		updateable("indexes sort reverse columns");
		updateable("tables rename tablename to name");
		updateable("tables where tablename > 1");
		not_updateable("tables summarize count");
		not_updateable("tables join columns");
		not_updateable("tables union columns");
		not_updateable("tables union columns extend xyz = 123");
	}

	private void updateable(String q) {
		assertTrue(q + "should be updateable",
				CompileQuery.query(db, serverData, q).updateable());
	}

	private void not_updateable(String q) {
		assertFalse(q + "should NOT be updateable",
				CompileQuery.query(db, serverData, q).updateable());
	}
}
