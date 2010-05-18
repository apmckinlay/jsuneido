package suneido.database.server;

import java.io.IOException;
import java.io.InputStream;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.util.Arrays;

import javax.annotation.concurrent.NotThreadSafe;

@NotThreadSafe
public class SocketInput {
	private static final int INITIAL_SIZE = 16 * 1024;
	private static final int MAX_SIZE = 64 * 1024;
	private final Socket socket;
	private final InputStream in;
	private byte[] buf = new byte[INITIAL_SIZE];
	private int len = 0;
	private int nlPos;

	public SocketInput(Socket socket) throws IOException {
		this.socket = socket;
		in = socket.getInputStream();
	}

	public ByteBuffer readLine() {
		int n;
		do {
			if (-1 == (n = read()))
				return null;
			len += n;
			nlPos = indexOf(buf, len, (byte) '\n');
		} while (nlPos == -1);
		return ByteBuffer.wrap(buf, 0, nlPos);
	}

	private int read() {
		if (len >= buf.length)
			buf = Arrays.copyOf(buf, 2 * buf.length);
		try {
			return in.read(buf, len, buf.length - len);
		} catch (IOException e) {
			// we get this if the client aborts the connection
			return -1;
		}
	}

	private static int indexOf(byte[] buf, int len, byte b) {
		for (int i = 0; i < len; ++i)
			if (buf[i] == b)
				return i;
		return -1;
	}

	public ByteBuffer readExtra(int nExtra) {
		int n;
		while (len < nlPos + nExtra) {
			if (-1 == (n = read()))
				return null;
			len += n;
		}
		ByteBuffer result =  ByteBuffer.wrap(buf, nlPos, nExtra);
		len = 0;
		if (buf.length > MAX_SIZE) // don't keep buffer bigger than max
			buf = new byte[MAX_SIZE];
		return result;
	}

}
