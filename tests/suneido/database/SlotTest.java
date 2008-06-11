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
}
