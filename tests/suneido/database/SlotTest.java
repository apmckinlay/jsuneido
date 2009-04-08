package suneido.database;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;

import java.nio.ByteBuffer;

import org.junit.Test;

public class SlotTest {
	@Test
	public void test() {
		Slot slot;
		Record r = new Record(100);
		r.add("hello");

		slot = new Slot(r);
		assertEquals(r.packSize(), slot.packSize());

		slot = new Slot(r, 1200);
		assertEquals(r.packSize() + 4, slot.packSize());

		slot = new Slot(r, 1200, 3400);
		assertEquals(r.packSize() + 8, slot.packSize());

		ByteBuffer buf = ByteBuffer.allocate(slot.packSize());
		slot.pack(buf);
		Slot slot2 = Slot.unpack(buf);
		assertEquals(slot.key, slot2.key);
		assertArrayEquals(slot.adrs, slot2.adrs);
		}

}
