package suneido.util;

import java.io.IOException;
import java.io.OutputStream;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

import javax.annotation.concurrent.NotThreadSafe;

@NotThreadSafe
public class OutputBySocket implements NetworkOutput {
	private final OutputStream out;
	private final List<ByteBuffer> queue = new ArrayList<>();
	private byte[] data = new byte[8];

	public OutputBySocket(Socket socket) throws IOException {
		out = socket.getOutputStream();
	}

	public void add(ByteBuffer buf) {
		queue.add(buf);
	}

	public void write() {
//System.out.println("< " + bufferToString(queue.get(0)));
		for (ByteBuffer buf : queue)
			write(buf);
		queue.clear();
	}

	private void write(ByteBuffer buf) {
		int n = buf.remaining();
		if (n > data.length)
			data = new byte[n];
		buf.get(data, 0, n);
		try {
			out.write(data, 0, n);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	@Override
	public void close() {
		try {
			out.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

}
