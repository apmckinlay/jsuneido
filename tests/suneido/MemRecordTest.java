package suneido;

import java.nio.ByteBuffer;

import org.junit.Test;
import static org.junit.Assert.*;

public class MemRecordTest {
	@Test
	public void test() {
		MemRecord r = new MemRecord();
		assertEquals(4, r.bufsize());
		
		assertArrayEquals(new byte[0], r.get(0));
		
		byte[] b = new byte[] { 1, 2, 3, 4 };
		r.add(b);
		assertArrayEquals(b, r.get(0));
		
		assertArrayEquals(new byte[0], r.get(1));
		
		assertEquals(9, r.bufsize());
		
		ByteBuffer buf = ByteBuffer.allocate(r.bufsize());
		BufRecord br = r.store(buf);
		ByteBuffer bb = br.get(0);
		assertEquals(4, bb.limit());
		byte[] b2 = new byte[4];
		bb.get(b2);
		assertArrayEquals(b, b2);
	}
}
