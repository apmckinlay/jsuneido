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
			String data1 = "hello";
			mr.add(data1);
			String data2 = "world";
			mr.add(data2);
			long offset = mmf.alloc(mr.packSize(), (byte) 1);
			ByteBuffer bb = mmf.adr(offset);
			mr.pack(bb);
			mmf.close();
			
			mmf = new Mmfile(file, Mode.OPEN);
			bb = mmf.iterator().next();
			Record br = new Record(bb);
			assertEquals(data1, br.getString(0));
			assertEquals(data2, br.getString(1));
		} finally {
			mmf.close();
			new File(file).delete();
		}
	}
}
