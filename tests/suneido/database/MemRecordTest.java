package suneido.database;

import java.nio.ByteBuffer;

import org.junit.Test;

import suneido.database.BufRecord;
import suneido.database.MemRecord;
import static org.junit.Assert.*;

public class MemRecordTest {
	@Test
	public void test() {
		MemRecord r = new MemRecord();
		assertEquals(4, r.packSize());
		
		assertArrayEquals(new byte[0], r.get(0));
		
		byte[] b = new byte[] { 1, 2, 3, 4 };
		r.add(b);
		assertArrayEquals(b, r.get(0));
		
		assertArrayEquals(new byte[0], r.get(1));
		
		assertEquals(9, r.packSize());
		
		ByteBuffer buf = ByteBuffer.allocate(r.packSize());
		r.pack(buf);
		BufRecord br = new BufRecord(buf);
		ByteBuffer bb = br.get(0);
		assertEquals(4, bb.limit());
		byte[] b2 = new byte[4];
		bb.get(b2);
		assertArrayEquals(b, b2);
	}
}
