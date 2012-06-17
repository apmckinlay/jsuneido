package suneido.database;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class DestMemTest {
	@Test
	public void first() {
		DestMem dm = new DestMem();
		long adr = dm.alloc(10, Mmfile.OTHER);
		assertEquals(adr, dm.first());
	}
}
