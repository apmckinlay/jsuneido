package suneido.database;

import java.nio.ByteBuffer;

import org.junit.Test;

import suneido.SuString;
import suneido.SuValue;
import suneido.database.BufRecord;
import static org.junit.Assert.*;

public class BufRecordTest {
	final byte[] data = new byte[] { 1, 2, 3, 4 };
	final byte[] data2 = new byte[] { 5, 6 };
	
	@Test
	public void test() {
		
		for (int sz : new int[] { 100, 1000, 100000 }) {
			BufRecord r = new BufRecord(sz);
			assertEquals(sz, r.bufSize());
			assertEquals(0, r.size());
			
			assertEquals(0, r.fieldSize(0));
			ByteBuffer bb = r.get(0);
			assertEquals(0, bb.limit());
			assertEquals(0, r.getBytes(0).length);
			
			r.add(data);
			assertEquals(1, r.size());
			assertEquals(4, r.fieldSize(0));
			bb = r.get(0);
			assertEquals(4, bb.limit());
			byte[] b = new byte[4];
			bb.get(b);
			assertArrayEquals(data, b);
			assertArrayEquals(data, r.getBytes(0));
			
			assertEquals(4, r.fieldSize(0));
			assertArrayEquals(data, r.getBytes(0));
		}
	}
	
	@Test
	public void bufsize() {
		assertEquals(4, BufRecord.bufSize(0, 0));
		assertEquals(10, BufRecord.bufSize(1, 5));
		assertEquals(1205, BufRecord.bufSize(100, 1000));
		assertEquals(104007, BufRecord.bufSize(1000, 100000));
	}
	
	@Test
	public void addPackable() {
		SuString s = new SuString("hello");
		
		BufRecord r = new BufRecord(500);
		r.add(s);
		
		SuValue x = SuValue.unpack(r.get(0));
		assertEquals(s, x);
	}
	
	@Test
	public void packBufRecord() {
		BufRecord r = new BufRecord(1000);
		assertEquals(1000, r.bufSize());
		assertEquals(4, r.packSize());
		
		ByteBuffer buf = ByteBuffer.allocate(r.packSize());
		r.pack(buf);
		
		r.add(data);
		r.add(data2);
		assertEquals(2, r.size());
		buf = ByteBuffer.allocate(r.packSize());
		r.pack(buf);
		BufRecord r2 = new BufRecord(buf);
		assertEquals(r.packSize(), r2.packSize());
		assertEquals(r2.bufSize(), r2.packSize());
		assertArrayEquals(data, r2.getBytes(0));
		
		ByteBuffer buf2 = ByteBuffer.allocate(r2.packSize());
		r2.pack(buf2);
		assertEquals(buf, buf2);
	}
}
