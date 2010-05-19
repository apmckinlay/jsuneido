package suneido.database.server;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.nio.ByteBuffer;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.*;

public class TestServer {

	public static void main(String[] args) {
		try {
			run();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private static void run() throws IOException {
		ServerSocketChannel serverChannel = ServerSocketChannel.open();
		ServerSocket serverSocket = serverChannel.socket();
		serverSocket.setReuseAddress(true);
		serverSocket.bind(new InetSocketAddress(3147));
		while (true) {
			SocketChannel channel = serverChannel.accept();
			Runnable handler = new Handler(channel);
			Thread worker = new Thread(handler);
			worker.start();
		}
	}

	private static class Handler implements Runnable {
		private static final ByteBuffer hello = ByteBuffer.wrap(new byte[]
				{ 'j', 'S', 'u', 'n', 'e', 'i', 'd', 'o', ' ',
				'S', 'e', 'r', 'v', 'e', 'r', '\r', '\n' });
		private final SocketChannel channel;
		private final Input input;
		private final Output outputQueue;
		private ByteBuffer line;
		private ByteBuffer extra;
		private final ByteBuffer response
				= ByteBuffer.wrap(new byte[] { 'E', 'O', 'F', '\r', '\n' });

		public Handler(SocketChannel channel) {
			this.channel = channel;
			input = new Input(channel);
			outputQueue = new Output(channel);
		}

		@Override
		public void run() {
			outputQueue.write(hello.duplicate());
			while (getRequest())
				executeRequest();
			close();
		}

		private boolean getRequest() {
			line = input.readLine();
			if (line == null)
				return false;
			int nExtra = 33; // normally determined by line
			line.position(0);
			extra = input.readExtra(nExtra);
			return true;
		}

		private void executeRequest() {
			try {
				outputQueue.add(response.duplicate());
			} catch (Throwable e) {
				e.printStackTrace();
			}
			line = null;
			extra = null;
			outputQueue.write();
		}

		public void close() {
			try {
				channel.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}

	} // end of Handler

	public static class Input {
		private static final int INITIAL_SIZE = 16 * 1024;
		private static final int MAX_SIZE = 64 * 1024;
		private final SocketChannel channel;
		private ByteBuffer buf = ByteBuffer.allocate(INITIAL_SIZE);
		private int nlPos;

		public Input(SocketChannel channel) {
			this.channel = channel;
		}

		public ByteBuffer readLine() {
			do {
				if (read() == -1)
					return null;
				nlPos = indexOf(buf, (byte) '\n');
			} while (nlPos == -1);
			ByteBuffer line = buf.duplicate();
			line.position(0);
			line.limit(++nlPos);
			return line;
		}

		private int read() {
			if (buf.remaining() == 0) {
				ByteBuffer oldbuf = buf;
				buf = ByteBuffer.allocate(2 * oldbuf.capacity());
				oldbuf.flip();
				buf.put(oldbuf);
			}
			try {
				return channel.read(buf);
			} catch (IOException e) {
				// we get this if the client aborts the connection
				return -1;
			}
		}

		private static int indexOf(ByteBuffer buf, byte b) {
			// NOTE: use buf.position because buf not flipped
			for (int i = 0; i < buf.position(); ++i)
				if (buf.get(i) == b)
					return i;
			return -1;
		}

		public ByteBuffer readExtra(int n) {
			while (buf.position() < nlPos + n)
				if (read() == -1)
					return null;
			buf.flip();
			buf.position(nlPos);
			ByteBuffer result = buf.slice();
			if (buf.capacity() <= MAX_SIZE)
				buf.clear();
			else // don't keep buffer bigger than max
				buf = ByteBuffer.allocateDirect(MAX_SIZE);
			return result;
		}

	} // end of Input

	public static class Output {
		private final SocketChannel channel;
		private final List<ByteBuffer> queue = new ArrayList<ByteBuffer>();
		private ByteBuffer[] bufs = new ByteBuffer[0];
		private int n;

		public Output(SocketChannel channel) {
			this.channel = channel;
		}

		public void add(ByteBuffer buf) {
			queue.add(buf);
		}

		public void write() {
			bufs = queue.toArray(bufs);
			n = queue.size();
			queue.clear();
			try {
				while (!isEmpty())
					channel.write(bufs, 0, n);
			} catch (IOException e) {
				e.printStackTrace();
			}
			Arrays.fill(bufs, null);
		}

		private boolean isEmpty() {
			for (int i = 0; i < n; ++i)
				if (bufs[i].remaining() > 0)
					return false;
			return true;
		}

		public void write(ByteBuffer buf) {
			try {
				while (buf.remaining() > 0)
					channel.write(buf);
			} catch (IOException e) {
				e.printStackTrace();
			}
		}

	} // end of Output

}
