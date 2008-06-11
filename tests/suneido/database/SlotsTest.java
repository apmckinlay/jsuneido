package suneido.database;

import java.nio.ByteBuffer;

import org.junit.Test;
import static org.junit.Assert.*;

public class SlotsTest {
	@Test
	public void test() {
		Slots slots = new Slots(ByteBuffer.allocate(Btree.NODESIZE));
		
		assertEquals(0, slots.size());
		slots.setNext(1234);
		slots.setPrev(5678);
		assertEquals(1234, slots.next());
		assertEquals(5678, slots.prev());
	}
}
