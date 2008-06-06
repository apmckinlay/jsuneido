package suneido.database;

import java.io.File;
import java.nio.ByteBuffer;
import java.util.Iterator;
import org.junit.Test;
import static org.junit.Assert.*;
import org.junit.AfterClass;
import suneido.*;
import suneido.database.Mmfile;

// use different file names in case of delayed finalization
public class MmfileTest {
	@Test(expected=SuException.class)
	public void cantopen() {
		new File("tmp1").delete();
		new Mmfile("tmp1");
	}
	
	@Test
	public void create_open() {
		new File("tmp2").delete();
		Mmfile mmf = new Mmfile("tmp2", true);
		assertEquals(8, mmf.size());
		mmf.close();
		
		mmf = new Mmfile("tmp2");
		assertEquals(8, mmf.size());
		mmf.close();
	}
	
	@Test
	public void read_write() {
		new File("tmp3").delete();
		Mmfile mmf = new Mmfile("tmp3", true);
		try {
			long offset[] = new long[2];
			offset[0] = mmf.alloc(16, (byte) 1);
			assertEquals(8 + 4, offset[0]);
			
			offset[1] = mmf.alloc(8, (byte) 1);
			assertEquals(8 + 4 + 16 + 4 + 4, offset[1]);
			
			assertEquals(48, mmf.size());
			
			byte[][] data = new byte[2][];
	
			data[0] = new byte[] { 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16 };
			mmf.adr(offset[0]).put(data[0]);
			
			data[1] = new byte[] { 8, 7, 6, 5, 4, 3, 2, 1 };
			mmf.adr(offset[1]).put(data[1]);
			
			int i = 0;
			for (ByteBuffer b : mmf) {
				byte[] x = new byte[data[i].length];
				b.get(x);
				assertArrayEquals(data[i], x);
				++i;
			}
			
			i = 1;
			for (Iterator<ByteBuffer> iter = mmf.reverse_iterator(); iter.hasNext(); --i) {
				ByteBuffer b = iter.next();
				byte[] x = new byte[data[i].length];
				b.get(x);
				assertArrayEquals(data[i], x);
			}
		} finally {
			mmf.close();
		}
	}
	
	@AfterClass
	public static void cleanup() {
		for (int i = 1; i <= 3; ++i)
			new File("tmp" + i).delete();
	}
}
