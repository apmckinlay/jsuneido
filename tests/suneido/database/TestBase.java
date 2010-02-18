package suneido.database;

import static suneido.database.Database.theDB;

import org.junit.After;
import org.junit.Before;

public class TestBase extends TestBaseBase {
	@Before
	public void create() {
		dest = new DestMem();
		theDB = db = new Database(dest, Mode.CREATE);
	}

	@After
	public void close() {
		db.close();
	}

}