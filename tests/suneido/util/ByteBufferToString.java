package suneido.util;

import java.nio.ByteBuffer;
import java.util.Date;

import suneido.util.Util;

public class ByteBufferToString {

	public static void main(String[] args) {
		Date start = new Date();

		ByteBuffer buf = Util.stringToBuffer(
				"now is the time for all good men to come to the aid of their party");

		for (int i = 0; i < 10000000; ++i)
			Util.bufferToString(buf);

		Date end = new Date();
		System.out.println(end.getTime() - start.getTime());
	}

}
