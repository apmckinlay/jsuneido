package suneido.database;

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

		ByteBuffer buf = ByteBuffer.allocate(slot.packSize());
		slot.pack(buf);
		buf.rewind();
		Slot slot2 = Slot.unpack(buf);
		assertEquals(slot.key, slot2.key);
		assertEquals(slot.adr, slot2.adr);
		}

}
