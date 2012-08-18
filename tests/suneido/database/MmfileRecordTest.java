package suneido.database;

import static org.junit.Assert.assertEquals;

import java.io.File;
import java.nio.ByteBuffer;

import org.junit.Test;

import suneido.util.ByteBuf;

public class MmfileRecordTest {
	private static final String file = "MmfileRecordTest";
	@Test
	public void test() {
		String data1 = "hello";
		String data2 = "world";
		new File(file).delete();
		Mmfile mmf = new Mmfile(file, Mode.CREATE);
		try {
			Record mr = new Record(100);
			mr.add(data1);
			mr.add(data2);
			long offset = mmf.alloc(mr.packSize(), (byte) 1);
			ByteBuf b = mmf.adr(offset);
			ByteBuffer bb = b.getByteBuffer(0);
			mr.pack(bb);
		} finally {
			mmf.close();
		}
		mmf = new Mmfile(file, Mode.OPEN);
		try {
			ByteBuf buf = mmf.adr(mmf.first());
			Record br = new Record(buf);
			assertEquals(data1, br.getString(0));
			assertEquals(data2, br.getString(1));
		} finally {
			mmf.close();
			new File(file).delete();
		}
	}
}
