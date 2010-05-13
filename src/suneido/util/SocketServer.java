package suneido.util;

import static suneido.util.Util.stringToBuffer;

import java.io.IOException;
import java.net.*;
import java.nio.ByteBuffer;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;

import javax.annotation.concurrent.NotThreadSafe;

/**
 * Socket server framework. Uses a supplied HandlerFactory to create a new
 * Handler for each accepted connection. Calls handler.start initially. Then
 * calls handler.moreInput each time more data is read. Input is accumulated
 * until moreInput consumes it, presumably after a complete request has been
 * received. The handler is given an OutputQueue to send its output to. Output
 * is gather written asynchronously. OutputQueue's are synchronized. It is up
 * to handlers to create worker threads if desired.
 *
 * Based loosely on examples in Java NIO by Ron Hitchens
 *
 * @author Andrew McKinlay
 */
@NotThreadSafe
public class SocketServer {
	private final HandlerFactory handlerFactory;
	private InetAddress inetAddress;

	public SocketServer(HandlerFactory handlerFactory) {
		this.handlerFactory = handlerFactory;
	}

	public void run(int port) throws IOException {
		ServerSocketChannel serverChannel = ServerSocketChannel.open();
		ServerSocket serverSocket = serverChannel.socket();
		serverSocket.setReuseAddress(true);
		serverSocket.bind(new InetSocketAddress(port));
		inetAddress = serverSocket.getInetAddress();
		while (true) {
			SocketChannel channel = serverChannel.accept();
			Runnable handler = handlerFactory.newHandler(channel, inetAddress.toString());
			Thread worker = new Thread(handler);
			worker.start();
		}
	}

	public InetAddress getInetAddress() {
		return inetAddress;
	}

//	private class CloseIdleConnections implements Runnable {
//		public void run() {
//			long t = System.currentTimeMillis();
//			for (SelectionKey key : selector.keys()) {
//				Info info = (Info) key.attachment();
//				if (info == null)
//					continue;
//				if (info.idleSince == 0)
//					info.idleSince = t;
//				else if (t - info.idleSince > IDLE_TIMEOUT) {
//					info.handler.close();
//					try {
//						key.channel().close();
//						selector.wakeup();
//					} catch (IOException e) {
//						e.printStackTrace();
//					}
//				}
//			}
//		}
//	}

	public static interface HandlerFactory {
		public Runnable newHandler(SocketChannel channel, String address);
	}

	//==========================================================================

	public static void main(String[] args) {
		SocketServer server = new SocketServer(new EchoHandlerFactory());
		try {
			server.run(1234);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	static class EchoHandler implements Runnable {
		private static ByteBuffer hello = stringToBuffer("EchoServer\r\n");
		private final SocketChannel channel;
		private final ByteBuffer buf = ByteBuffer.allocate(128);

		EchoHandler(SocketChannel channel) {
			this.channel = channel;
		}

		@Override
		public void run() {
			try {
				channel.write(hello.duplicate());
				while (-1 != channel.read(buf)) {
					buf.flip();
					channel.write(buf);
					buf.clear();
				}
				channel.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}
	static class EchoHandlerFactory implements HandlerFactory {
		@Override
		public Runnable newHandler(SocketChannel channel, String address) {
			return new EchoHandler(channel);
		}
	}

}
