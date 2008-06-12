package suneido.database;

import java.nio.ByteBuffer;

import org.junit.Test;
import static org.junit.Assert.*;

import suneido.SuString;

public class SlotTest {
	@Test
	public void test() {
		Slot slot;
		BufRecord r = new BufRecord(100);
		r.add(new SuString("hello"));
		
		slot = new Slot(r);
		assertEquals(r.packSize(), slot.packSize());
		
		slot = new Slot(r, 123);
		assertEquals(r.packSize() + 8, slot.packSize());

		slot = new Slot(r, 123, 456);
		assertEquals(r.packSize() + 16, slot.packSize());

		ByteBuffer buf = ByteBuffer.allocate(slot.packSize());
		slot.pack(buf);
		Slot slot2 = Slot.unpack(buf);
		assertEquals(slot.key, slot2.key);
		assertArrayEquals(slot.adrs, slot2.adrs);
		}
	
	public static Slot make1(String ... args) {
		if (args.length == 0)
			args = new String[] { "hello" };
		BufRecord r = new BufRecord(100);
		for (String s : args)
			r.add(new SuString(s));
		return new Slot(r);
	}
	
	public static void printBuf(ByteBuffer buf) {
		String s = "";
		for (int j = 0; j < buf.limit(); ++j)
			s += " " + buf.get(j);
		System.out.println("limit " + buf.limit() + " buf" + s);
	}
}
