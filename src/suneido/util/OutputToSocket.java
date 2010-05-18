package suneido.util;

import static suneido.Suneido.fatal;

import java.io.IOException;
import java.io.OutputStream;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.util.Arrays;

import javax.annotation.concurrent.NotThreadSafe;

@NotThreadSafe
public class OutputToSocket implements NetworkOutput {
	private final OutputStream out;
	private final int MAXSIZE = 8;
	private final ByteBuffer[] queue = new ByteBuffer[MAXSIZE];
	private int nQueue = 0;

	public OutputToSocket(Socket socket) throws IOException {
		out = socket.getOutputStream();
	}

	public void add(ByteBuffer buf) {
		queue[nQueue++] = buf;
	}

	public void write() {
		assert nQueue == 1;
		try {
			byte[] buf = new byte[queue[0].remaining()];
			queue[0].get(buf);
			out.write(buf);
		} catch (IOException e) {
			fatal("network write error", e); // TODO
		}
		Arrays.fill(queue, null);
		nQueue = 0;
	}

}
