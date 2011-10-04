package suneido.database;

import static suneido.util.Verify.verifyEquals;

import java.io.File;

import suneido.util.ByteBuf;

public class TestMmfile64Bit {

	public static void main(String[] args) {
		File file = new File("testmmfile64bit");
		file.deleteOnExit();
		Mmfile mmf = new Mmfile(file, Mode.CREATE);
		// create 8 gb file
		for (int i = 0; i < 8000; ++i)
			mmf.alloc(1024 * 1024 - 100, Mmfile.OTHER);
		ByteBuf buf = mmf.adr(mmf.alloc(10, Mmfile.OTHER));
		buf.putInt(0, 1234567890);
		verifyEquals(1234567890, buf.getInt(0));
		mmf.close();
	}

}
