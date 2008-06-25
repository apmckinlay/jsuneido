package suneido.database;

import java.io.File;
import java.nio.ByteBuffer;

import org.junit.AfterClass;
import org.junit.Test;
import static org.junit.Assert.*;

public class DatabaseTest {
	@Test
	public void create_open() {
		new File("tmp1").delete();
		Database db = new Database("tmp1", Mode.CREATE);
		Record r = new Record(100);
		byte[] b = new byte[] { 1, 2, 3, 4 };
		r.add(b);
		long offset = db.output(1234, r);
		db.close();
		
		db = new Database("tmp1", Mode.OPEN);
		ByteBuffer bb = db.adr(offset);
		assertEquals(1234, bb.getInt());
		bb.position(4);
		Record br = new Record(bb.slice());
		assertArrayEquals(b, br.getBytes(0));
	}
	
	@AfterClass
	public static void cleanup() {
		for (int i = 1; i <= 1; ++i)
			new File("tmp" + i).delete();
	}
}
