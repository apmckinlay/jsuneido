package suneido;

import java.nio.ByteBuffer;

import org.junit.Test;
import static org.junit.Assert.*;

public class BufRecordTest {
	@Test
	public void test() {
		byte[] b = new byte[] { 1, 2, 3, 4 };
		
		for (int sz : new int[] { 100, 1000, 100000 }) {
			BufRecord r = new BufRecord(sz);
			assertEquals(sz, r.getSize());
			
			ByteBuffer bb = r.get(0);
			assertEquals(0, bb.limit());
			
			r.add(b);
			bb = r.get(0);
			assertEquals(4, bb.limit());
			byte[] b2 = new byte[4];
			bb.get(b2);
			assertArrayEquals(b, b2);
			
			ByteBuffer buf = r.getBuf();
			bb = r.get(0);
			assertEquals(4, bb.limit());
			b2 = new byte[4];
			bb.get(b2);
			assertArrayEquals(b, b2);
			r = new BufRecord(buf);
			bb = r.get(1);
			assertEquals(0, bb.limit());
		}
	}
	
	@Test
	public void bufsize() {
		assertEquals(4, BufRecord.bufsize(0, 0));
		assertEquals(10, BufRecord.bufsize(1, 5));
		assertEquals(1205, BufRecord.bufsize(100, 1000));
		assertEquals(104007, BufRecord.bufsize(1000, 100000));
	}
}
