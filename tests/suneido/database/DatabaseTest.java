package suneido.database;

import static org.junit.Assert.assertEquals;

import java.io.File;
import java.nio.ByteBuffer;

import org.junit.AfterClass;
import org.junit.Test;

public class DatabaseTest {
	@Test
	public void create_open() {
		new File("tmp1").delete();
		Database db = new Database("tmp1", Mode.CREATE);
		String b = "hello";
		Record r = new Record().add(b);
		long offset = db.output(1234, r);
		// db.add_any_record(0, tbl, r);
		db.close();

		db = new Database("tmp1", Mode.OPEN);
		Record r2 = db.input(offset);
		assertEquals(r, r2);

		ByteBuffer bb = db.adr(offset - 4);
		assertEquals(1234, bb.getInt());
	}

	@AfterClass
	public static void cleanup() {
		for (int i = 1; i <= 1; ++i)
			new File("tmp" + i).delete();
	}

	// public static void main(String args[]) {
	// new DatabaseTest().create_open();
	// }
}
