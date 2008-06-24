package suneido.database;

import java.nio.ByteBuffer;

import org.junit.Test;
import static org.junit.Assert.*;

import suneido.SuInteger;
import suneido.SuString;
import suneido.SuValue;
import suneido.database.BufRecord;

public class BufRecordTest {
	final static byte[] data = new byte[] { 1, 2, 3, 4 };
	final static byte[] data2 = new byte[] { 5, 6 };
	
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
		assertEquals(4, BufRecord.packSize(0, 0));
		assertEquals(10, BufRecord.packSize(1, 5));
		assertEquals(1205, BufRecord.packSize(100, 1000));
		assertEquals(104007, BufRecord.packSize(1000, 100000));
	}
	
	@Test
	public void addPackable() {
		BufRecord r = new BufRecord(500);
		SuString s = new SuString("hello");
		r.add(s);
		assertEquals(s, SuValue.unpack(r.get(0)));
		SuString s2 = new SuString("world");
		r.add(s2);
		assertEquals(s, SuValue.unpack(r.get(0)));
		assertEquals(s2, SuValue.unpack(r.get(1)));

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
	
	@Test
	public void compareTo() {
		BufRecord r = new BufRecord(100);
		BufRecord r2 = new BufRecord(1000);
		assertEquals(r, r);
		assertEquals(r, r2);
		assertEquals(r2, r);
		r.add(data);
		assertFalse(r.equals(r2));
		assertFalse(r2.equals(r));
		assertEquals(+1, Math.signum(r.compareTo(r2)));
		assertEquals(-1, Math.signum(r2.compareTo(r)));
		r2.add(data);
		assertEquals(r, r2);
		assertEquals(r2, r);
		r.add(data);
		r.add(data2);
		r2.add(data2);
		r2.add(data);
		assertEquals(-1, Math.signum(r.compareTo(r2)));
		assertEquals(+1, Math.signum(r2.compareTo(r)));
	}
	
	@Test
	public void insert() {
		BufRecord r = make(data, data2); 
		assertArrayEquals(data, r.getBytes(0));
		assertArrayEquals(data2, r.getBytes(1));
		assertFalse(r.insert(1,
				new SuString("hellooooooooooooooooooooooooooooooooooooooo")));
		SuString s = new SuString("hello");
		assertTrue(r.insert(1, s));
		assertArrayEquals(data, r.getBytes(0));
		assertEquals(s, SuValue.unpack(r.get(1)));
		assertArrayEquals(data2, r.getBytes(2));
		
		r = new BufRecord(100);
		r.insert(0, s); // insert at beginning
		assertEquals(s, SuValue.unpack(r.get(0)));
		r.insert(1, s); // insert at end (same as add)
		assertEquals(s, SuValue.unpack(r.get(0)));
	}
	
	@Test
	public void remove() {
		BufRecord r = make(data2, data2, data, data);
		r.remove(2); // middle
		assertEquals(make(data2, data2, data), r);
		r.remove(0); // first
		assertEquals(make(data2, data), r);
		r.remove(1); // last
		assertEquals(make(data2), r);
		r.remove(0);
		assertTrue(r.isEmpty());
	}
	
	@Test
	public void remove_range() {
		BufRecord r = make(data, data2, data, data2);
		r.remove(1,3);
		assertEquals(make(data, data2), r);
	}
	
	@Test
	public void dup() {
		BufRecord r = make(data, data2);
		assertEquals(r, r.dup());
	}
	
	@Test
	public void unpackLong() {
		BufRecord r = new BufRecord(40);
		r.add(SuInteger.valueOf(0));
		r.add(SuInteger.valueOf(1234));
		assertEquals(0, r.getLong(0));
		assertEquals(1234, r.getLong(1));
	}
	
	public static BufRecord make(byte[] ... args) {
		if (args.length == 0)
			args = new byte[][] { data };
		BufRecord r = new BufRecord(40);
		for (byte[] bs : args)
			r.add(bs);
		return r;
	}
	
	@Test
	public void bytecmp() {
		byte x = 3;
		byte y = (byte) 130;
		assertEquals(130, (y & 0xff));
		assertTrue(x < (y & 0xff));
	}
	
	@Test
	public void order() {
		SuValue values[] = { SuInteger.valueOf(0), SuInteger.valueOf(70), SuInteger.valueOf(140),
				SuInteger.valueOf(9999), SuInteger.valueOf(10001) };
		BufRecord prev = null;
		for (SuValue x : values) {
			BufRecord rec = new BufRecord(100);
			rec.add(x);
			if (prev != null)
				assertTrue(rec.compareTo(prev) > 0);
			prev = rec;
		}
	}
	
	@Test
	public void hasPrefix() {
		BufRecord rec = new BufRecord(100);
		BufRecord pre = new BufRecord(100);
		assertTrue(rec.hasPrefix(pre));
		pre.add(SuInteger.valueOf(6));
		assertFalse(rec.hasPrefix(pre));
		rec.add(SuInteger.valueOf(6));
		assertTrue(rec.hasPrefix(pre));
		rec.add(SuInteger.valueOf(99));
		assertTrue(rec.hasPrefix(pre));
	}

//	public static void main(String args[]) {
//		new BufRecordTest().hasPrefix();
//	}
}
