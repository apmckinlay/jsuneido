package suneido;

import java.nio.ByteBuffer;

import org.junit.Test;
import static org.junit.Assert.*;

public class BufRecordTest {
	@Test
	public void test() {
		byte[] data = new byte[] { 1, 2, 3, 4 };
		
		for (int sz : new int[] { 100, 1000, 100000 }) {
			BufRecord r = new BufRecord(sz);
			assertEquals(sz, r.getSize());
			
			assertEquals(0, r.size(0));
			ByteBuffer bb = r.get(0);
			assertEquals(0, bb.limit());
			assertEquals(0, r.getBytes(0).length);
			
			r.add(data);
			assertEquals(4, r.size(0));
			bb = r.get(0);
			assertEquals(4, bb.limit());
			byte[] b = new byte[4];
			bb.get(b);
			assertArrayEquals(data, b);
			assertArrayEquals(data, r.getBytes(0));
			
			ByteBuffer buf = r.getBuf();
			assertEquals(4, r.size(0));
			assertArrayEquals(data, r.getBytes(0));
			r = new BufRecord(buf);
			assertEquals(0, r.size(1));
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
