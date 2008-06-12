package suneido.database;

import java.nio.ByteBuffer;

import org.junit.Test;
import static org.junit.Assert.*;

public class SlotsTest {
	@Test
	public void test() {
		Slots slots = new Slots(ByteBuffer.allocate(Btree.NODESIZE), Mode.CREATE);
		
		assertEquals(0, slots.size());
		slots.setNext(1200);
		slots.setPrev(3400);
		assertEquals(1200, slots.next());
		assertEquals(3400, slots.prev());
	}
	
	@Test
	public void test2() {
		Slots slots = new Slots(ByteBuffer.allocate(Btree.NODESIZE), Mode.CREATE);
		Slot slot = SlotTest.make();
		slots.add(slot);

		assertEquals(slot, slots.get(0));
	}
	
	@Test
	public void lower_bound() {
		Slots slots = new Slots(ByteBuffer.allocate(Btree.NODESIZE), Mode.CREATE);
		assertEquals(0, slots.lower_bound(SlotTest.make("m")));
		slots.add(SlotTest.make("m"));
		assertEquals(0, slots.lower_bound(SlotTest.make("a")));
		assertEquals(0, slots.lower_bound(SlotTest.make("m")));
		assertEquals(1, slots.lower_bound(SlotTest.make("z")));
		slots.insert(0, SlotTest.make("c"));
		slots.add(SlotTest.make("m"));
		slots.add(SlotTest.make("x"));
		// now have c, m, m, x
		assertEquals(0, slots.lower_bound(SlotTest.make("a")));
		assertEquals(0, slots.lower_bound(SlotTest.make("c")));
		assertEquals(1, slots.lower_bound(SlotTest.make("e")));
		assertEquals(1, slots.lower_bound(SlotTest.make("m")));
		assertEquals(3, slots.lower_bound(SlotTest.make("o")));
		assertEquals(3, slots.lower_bound(SlotTest.make("x")));
		assertEquals(4, slots.lower_bound(SlotTest.make("z")));
	}
	
	@Test
	public void upper_bound() {
		Slots slots = new Slots(ByteBuffer.allocate(Btree.NODESIZE), Mode.CREATE);
		assertEquals(0, slots.upper_bound(SlotTest.make("m")));
		slots.add(SlotTest.make("m"));
		assertEquals(0, slots.upper_bound(SlotTest.make("a")));
		assertEquals(1, slots.upper_bound(SlotTest.make("m")));
		assertEquals(1, slots.upper_bound(SlotTest.make("z")));
		slots.insert(0, SlotTest.make("c"));
		slots.add(SlotTest.make("m"));
		slots.add(SlotTest.make("x"));
		// now have c, m, m, x
		assertEquals(0, slots.upper_bound(SlotTest.make("a")));
		assertEquals(1, slots.upper_bound(SlotTest.make("c")));
		assertEquals(1, slots.upper_bound(SlotTest.make("e")));
		assertEquals(3, slots.upper_bound(SlotTest.make("m")));
		assertEquals(3, slots.upper_bound(SlotTest.make("o")));
		assertEquals(4, slots.upper_bound(SlotTest.make("x")));
		assertEquals(4, slots.upper_bound(SlotTest.make("z")));
	}
}
