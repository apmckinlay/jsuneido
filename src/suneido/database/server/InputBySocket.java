package suneido.database.server;

import java.io.IOException;
import java.io.InputStream;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.util.Arrays;

import javax.annotation.concurrent.NotThreadSafe;

@NotThreadSafe
public class InputBySocket {
	private static final int INITIAL_SIZE = 16 * 1024;
	private static final int MAX_SIZE = 64 * 1024;
	private final InputStream in;
	private byte[] data = new byte[INITIAL_SIZE];
	private int len = 0;
	private int nlPos;

	public InputBySocket(Socket socket) throws IOException {
		in = socket.getInputStream();
	}

	public ByteBuffer readLine() {
		int n;
		do {
			if (-1 == (n = read()))
				return null;
			len += n;
			nlPos = indexOf(data, len, (byte) '\n');
		} while (nlPos == -1);
		return ByteBuffer.wrap(data, 0, ++nlPos);
	}

	private int read() {
		if (len >= data.length)
			data = Arrays.copyOf(data, 2 * data.length);
		try {
			return in.read(data, len, data.length - len);
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
		ByteBuffer result =  ByteBuffer.wrap(data, nlPos, nExtra).slice();
		len = 0;
		if (data.length > MAX_SIZE) // don't keep buffer bigger than max
			data = new byte[MAX_SIZE];
		return result;
	}

}
