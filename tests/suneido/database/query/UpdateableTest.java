package suneido.database.query;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static suneido.database.Database.theDB;

import org.junit.Test;

import suneido.database.Transaction;


public class UpdateableTest extends TestBase {
	@Test
	public void test() {
		updateable("tables");
		updateable("tables extend xyz = 123");
		updateable("tables project table");
		not_updateable("columns project table");
		updateable("tables sort totalsize");
		updateable("tables sort reverse totalsize");
		updateable("tables rename totalsize to bytes");
		updateable("tables where totalsize > 1000");
		not_updateable("tables summarize count");
		not_updateable("tables join columns");
		not_updateable("tables union columns");
		not_updateable("tables union columns extend xyz = 123");
	}

	private void updateable(String q) {
		assertTrue(q + "should be updateable",
				CompileQuery.query(new Transaction(theDB.tabledataMaster),
						serverData, q).updateable());
	}

	private void not_updateable(String q) {
		assertFalse(q + "should NOT be updateable",
				CompileQuery.query(new Transaction(theDB.tabledataMaster),
						serverData, q).updateable());
	}
}
