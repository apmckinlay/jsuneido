package suneido.database;

import java.io.File;
import java.nio.ByteBuffer;
import org.junit.Test;

import suneido.database.Record;
import suneido.database.Mmfile;
import static org.junit.Assert.*;

public class MmfileRecordTest {
	private final static String file = "MmfileRecordTest";
	@Test
	public void test() {
		new File(file).delete();
		Mmfile mmf = new Mmfile(file, Mode.CREATE);
		try {
			Record mr = new Record(100);
			byte[] data1 = new byte[] { 1, 2, 3, 4 };
			mr.add(data1);
			byte[] data2 = new byte[] { 4, 3, 2, 1 };
			mr.add(data2);
			long offset = mmf.alloc(mr.packSize(), (byte) 1);
			ByteBuffer bb = mmf.adr(offset);
			mr.pack(bb);
			mmf.close();
			
			mmf = new Mmfile(file, Mode.OPEN);
			bb = mmf.iterator().next();
			Record br = new Record(bb);
			assertArrayEquals(data1, br.getBytes(0));
			assertArrayEquals(data2, br.getBytes(1));
		} finally {
			mmf.close();
			new File(file).delete();
		}
	}
}
