package suneido.database;

import java.nio.ByteBuffer;

import org.junit.Test;
import static org.junit.Assert.*;

public class SlotsTest {
	@Test
	public void test() {
		Slots slots = new Slots(ByteBuffer.allocate(Btree.NODESIZE), Slots.Mode.INIT);
		
		assertEquals(0, slots.size());
		slots.setNext(1234);
		slots.setPrev(5678);
		assertEquals(1234, slots.next());
		assertEquals(5678, slots.prev());
	}
	
	@Test
	public void test2() {
		Slots slots = new Slots(ByteBuffer.allocate(Btree.NODESIZE), Slots.Mode.INIT);
		Slot slot = SlotTest.make1();
		slots.add(slot);

		assertEquals(slot, slots.get(0));
	}
	
	@Test
	public void lower_bound() {
		Slots slots = new Slots(ByteBuffer.allocate(Btree.NODESIZE), Slots.Mode.INIT);
		assertEquals(0, slots.lower_bound(SlotTest.make1("m")));
		slots.add(SlotTest.make1("m"));
		assertEquals(0, slots.lower_bound(SlotTest.make1("a")));
		assertEquals(0, slots.lower_bound(SlotTest.make1("m")));
		assertEquals(1, slots.lower_bound(SlotTest.make1("z")));
		slots.insert(0, SlotTest.make1("c"));
		slots.add(SlotTest.make1("m"));
		slots.add(SlotTest.make1("x"));
		// now have c, m, m, x
		assertEquals(0, slots.lower_bound(SlotTest.make1("a")));
		assertEquals(0, slots.lower_bound(SlotTest.make1("c")));
		assertEquals(1, slots.lower_bound(SlotTest.make1("e")));
		assertEquals(1, slots.lower_bound(SlotTest.make1("m")));
		assertEquals(3, slots.lower_bound(SlotTest.make1("o")));
		assertEquals(3, slots.lower_bound(SlotTest.make1("x")));
		assertEquals(4, slots.lower_bound(SlotTest.make1("z")));
	}
	
	@Test
	public void upper_bound() {
		Slots slots = new Slots(ByteBuffer.allocate(Btree.NODESIZE), Slots.Mode.INIT);
		assertEquals(0, slots.upper_bound(SlotTest.make1("m")));
		slots.add(SlotTest.make1("m"));
		assertEquals(0, slots.upper_bound(SlotTest.make1("a")));
		assertEquals(1, slots.upper_bound(SlotTest.make1("m")));
		assertEquals(1, slots.upper_bound(SlotTest.make1("z")));
		slots.insert(0, SlotTest.make1("c"));
		slots.add(SlotTest.make1("m"));
		slots.add(SlotTest.make1("x"));
		// now have c, m, m, x
		assertEquals(0, slots.upper_bound(SlotTest.make1("a")));
		assertEquals(1, slots.upper_bound(SlotTest.make1("c")));
		assertEquals(1, slots.upper_bound(SlotTest.make1("e")));
		assertEquals(3, slots.upper_bound(SlotTest.make1("m")));
		assertEquals(3, slots.upper_bound(SlotTest.make1("o")));
		assertEquals(4, slots.upper_bound(SlotTest.make1("x")));
		assertEquals(4, slots.upper_bound(SlotTest.make1("z")));
	}
}
