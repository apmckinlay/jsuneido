package suneido.database;

import static org.junit.Assert.assertEquals;

import java.nio.ByteBuffer;

import org.junit.Test;

import suneido.SuString;

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
		Slot slot = make();
		slots.add(slot);

		assertEquals(slot, slots.get(0));
	}

	@Test
	public void lower_bound() {
		Slots slots = new Slots(ByteBuffer.allocate(Btree.NODESIZE), Mode.CREATE);
		assertEquals(0, slots.lower_bound(make("m")));
		slots.add(make("m"));
		assertEquals(0, slots.lower_bound(make("a")));
		assertEquals(0, slots.lower_bound(make("m")));
		assertEquals(1, slots.lower_bound(make("z")));
		slots.insert(0, make("c"));
		slots.add(make("m"));
		slots.add(make("x"));
		// now have c, m, m, x
		assertEquals(0, slots.lower_bound(make("a")));
		assertEquals(0, slots.lower_bound(make("c")));
		assertEquals(1, slots.lower_bound(make("e")));
		assertEquals(1, slots.lower_bound(make("m")));
		assertEquals(3, slots.lower_bound(make("o")));
		assertEquals(3, slots.lower_bound(make("x")));
		assertEquals(4, slots.lower_bound(make("z")));
	}

	@Test
	public void upper_bound() {
		Slots slots = new Slots(ByteBuffer.allocate(Btree.NODESIZE), Mode.CREATE);
		assertEquals(0, slots.upper_bound(make("m")));
		slots.add(make("m"));
		assertEquals(0, slots.upper_bound(make("a")));
		assertEquals(1, slots.upper_bound(make("m")));
		assertEquals(1, slots.upper_bound(make("z")));
		slots.insert(0, make("c"));
		slots.add(make("m"));
		slots.add(make("x"));
		// now have c, m, m, x
		assertEquals(0, slots.upper_bound(make("a")));
		assertEquals(1, slots.upper_bound(make("c")));
		assertEquals(1, slots.upper_bound(make("e")));
		assertEquals(3, slots.upper_bound(make("m")));
		assertEquals(3, slots.upper_bound(make("o")));
		assertEquals(4, slots.upper_bound(make("x")));
		assertEquals(4, slots.upper_bound(make("z")));
	}

	public static Slot make(String... args) {
		if (args.length == 0)
			args = new String[] { "hello" };
		Record r = new Record(100);
		for (String s : args)
			r.add(new SuString(s));
		return new Slot(r);
	}
}
