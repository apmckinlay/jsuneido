package suneido.database.server;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;

public class TestClient {
	private static final int NTHREADS = 20;
	private static final long DURATION = 5 * 60 * 1000;

	public static void main(String... args) {
		String address = args.length == 0 ? "localhost" : args[0];
		for (int i = 0; i < NTHREADS; ++i)
			new Thread(new Client(address)).start();
	}

	private static class Client implements Runnable {
		private final String address;
		private final ByteBuffer request1 = ByteBuffer.allocate(16);
		private final ByteBuffer request2 = ByteBuffer.allocate(32);
		private final ByteBuffer buf = ByteBuffer.allocate(5);

		public Client(String address) {
			this.address = address;
			byte j = 0;
			for (int i = 0; i < request1.capacity(); ++i)
				request1.put(i, j++);
			for (int i = 0; i < request2.capacity(); ++i)
				request2.put(i, j++);
		}

		public void run() {
			try {
				run2();
			} catch (UnknownHostException e) {
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}

		private void run2() throws UnknownHostException, IOException {
			SocketChannel channel = SocketChannel.open(
					new InetSocketAddress(address, 8080));
			channel.socket().setSoTimeout(2000);
			long t = System.currentTimeMillis();
			while (true) {
				long elapsed = System.currentTimeMillis() - t;
				if (elapsed > DURATION)
					break;
				if (request1.capacity() != channel.write(request1.duplicate()))
					throw new IOException("incomplete write");
				if (request2.capacity() != channel.write(request2.duplicate()))
					throw new IOException("incomplete write");
				buf.clear();
				do {
					if (channel.read(buf) == -1)
						return;
				} while (buf.position() < buf.capacity());
				for (int i = 0; i < buf.capacity(); ++i)
					if (buf.get(i) != i)
						System.out.println("incorrect response byte "
								+ i + " = " + buf.get(i));
			}
		}
	}

}
