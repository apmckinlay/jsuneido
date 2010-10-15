package suneido.database;

import static org.junit.Assert.*;

import java.io.File;

import org.junit.AfterClass;
import org.junit.Test;

import suneido.SuException;
import suneido.util.ByteBuf;

// use different file names in case of delayed finalization
public class MmfileTest {
	@Test(expected=SuException.class)
	public void cantopen() {
		new File("tmp1").delete();
		new Mmfile("tmp1", Mode.OPEN);
	}

	@Test
	public void create_open() {
		new File("tmp2").delete();
		Mmfile mmf = new Mmfile("tmp2", Mode.CREATE);
		assertEquals(8, mmf.size());
		mmf.close();

		mmf = new Mmfile("tmp2", Mode.OPEN);
		assertEquals(8, mmf.size());
		mmf.close();
	}

	@Test
	public void read_write() {
		new File("tmp3").delete();
		Mmfile mmf = new Mmfile("tmp3", Mode.CREATE);
		try {
			long offset[] = new long[2];
			offset[0] = mmf.alloc(16, (byte) 1);
			assertEquals(8 + 4, offset[0]);

			offset[1] = mmf.alloc(8, (byte) 1);
			assertEquals(8 + 4 + 16 + 4 + 4, offset[1]);

			assertEquals(48, mmf.size());

			byte[][] data = new byte[2][];

			data[0] = new byte[] { 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16 };
			mmf.adr(offset[0]).put(0, data[0]);

			data[1] = new byte[] { 8, 7, 6, 5, 4, 3, 2, 1 };
			mmf.adr(offset[1]).put(0, data[1]);

			int i = 0;
			Mmfile.Iter iter = mmf.iterator();
			while (iter.next()) {
				ByteBuf b = iter.current();
				byte[] x = new byte[data[i].length];
				b.get(0, x);
				assertArrayEquals(data[i], x);
				++i;
			}

		} finally {
			mmf.close();
		}
	}

	@Test
	public void offset_conversions() {
		long offset = 10L * 1024 * 1024 * 1024;
		int i = Mmfile.offsetToInt(offset);
		assertTrue(i < 0);
		assertEquals(offset, Mmfile.intToOffset(i));
	}

	@AfterClass
	public static void cleanup() {
		for (int i = 1; i <= 3; ++i)
			new File("tmp" + i).delete();
	}
}
