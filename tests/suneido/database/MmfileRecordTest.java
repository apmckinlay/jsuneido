package suneido.database;

import java.io.File;
import java.nio.ByteBuffer;
import org.junit.Test;

import suneido.database.BufRecord;
import suneido.database.MemRecord;
import suneido.database.Mmfile;
import static org.junit.Assert.*;

public class MmfileRecordTest {
	@Test
	public void test() {
		new File("tmp1").delete();
		Mmfile mmf = new Mmfile("tmp1", Mode.CREATE);
		try {
			MemRecord mr = new MemRecord();
			byte[] data1 = new byte[] { 1, 2, 3, 4 };
			mr.add(data1);
			byte[] data2 = new byte[] { 4, 3, 2, 1 };
			mr.add(data2);
			long offset = mmf.alloc(mr.bufsize(), (byte) 1);
			ByteBuffer bb = mmf.adr(offset);
			mr.store(bb);
			mmf.close();
			
			mmf = new Mmfile("tmp1", Mode.OPEN);
			bb = mmf.iterator().next();
			BufRecord br = new BufRecord(bb);
			assertArrayEquals(data1, br.getBytes(0));
			assertArrayEquals(data2, br.getBytes(1));
		} finally {
			mmf.close();
			new File("tmp1").delete();
		}
	}
}
