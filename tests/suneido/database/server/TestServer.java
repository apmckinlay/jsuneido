package suneido.database.server;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.nio.ByteBuffer;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;

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
		serverSocket.bind(new InetSocketAddress(8080));
		while (true) {
			SocketChannel channel = serverChannel.accept();
			Runnable handler = new Handler(channel);
			Thread worker = new Thread(handler);
			worker.start();
		}
	}

	private static class Handler implements Runnable {
		private final SocketChannel channel;
		private final ByteBuffer buf = ByteBuffer.allocate(48);
		private final ByteBuffer response = ByteBuffer.allocate(5);
		private ByteBuffer request1;

		public Handler(SocketChannel channel) {
			this.channel = channel;
			for (int i = 0; i < response.capacity(); ++i)
				response.put(i, (byte) i);
		}

		@Override
		public void run() {
			try {
				while (-1 != channel.read(buf)) {
					if (null == (request1 = getFirstPart(buf)))
						continue;
					if (buf.position() < buf.capacity())
						continue;
					for (int i = 0; i < buf.capacity(); ++i)
						if (buf.get(i) != i)
							System.out.println("incorrect response byte "
									+ i + " = " + buf.get(i));
					if (response.capacity() != channel.write(response.duplicate()))
						throw new IOException("incomplete write");
					buf.clear();
				}
				channel.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}

		private static ByteBuffer getFirstPart(ByteBuffer buf) {
			int i = indexOf(buf, (byte) 16);
			if (i == -1)
				return null;
			ByteBuffer tmp = buf.duplicate();
			tmp.position(0);
			tmp.limit(i);
			return tmp;
		}

		private static int indexOf(ByteBuffer buf, byte b) {
			for (int i = 0; i < buf.position(); ++i)
				if (buf.get(i) == b)
					return i;
			return -1;
		}

	}
}
