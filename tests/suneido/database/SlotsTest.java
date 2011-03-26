package suneido.database;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import suneido.util.ByteBuf;

public class SlotsTest {
	@Test
	public void test() {
		Slots slots = new Slots(ByteBuf.allocate(Btree.NODESIZE), Mode.CREATE);

		assertEquals(0, slots.size());
		slots.setNext(1200);
		slots.setPrev(3400);
		assertEquals(1200, slots.next());
		assertEquals(3400, slots.prev());
	}

	@Test
	public void test2() {
		Slots slots = new Slots(ByteBuf.allocate(Btree.NODESIZE), Mode.CREATE);
		Slot slot = make();
		slots.add(slot);

		assertEquals(slot, slots.get(0));
	}

	public static Slot make(String... args) {
		if (args.length == 0)
			args = new String[] { "hello" };
		Record r = new Record(100);
		for (String s : args)
			r.add(s);
		return new Slot(r);
	}
}
