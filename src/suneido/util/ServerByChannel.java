package suneido.util;

import static suneido.util.Util.stringToBuffer;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.nio.ByteBuffer;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;

import javax.annotation.concurrent.NotThreadSafe;

/**
 * Socket server framework using blocking NIO channels (but not Selector).
 * Uses a supplied HandlerFactory to create a new Runnable handler
 * for each accepted connection.
 * Creates a Thread per connection.
 *
 * @author Andrew McKinlay
 */
@NotThreadSafe
public class ServerByChannel {
	private final HandlerFactory handlerFactory;
	private InetAddress inetAddress;

	public ServerByChannel(HandlerFactory handlerFactory) {
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

	public interface HandlerFactory {
		Runnable newHandler(SocketChannel channel, String address);
	}

	//==========================================================================

	public static void main(String[] args) {
		ServerByChannel server = new ServerByChannel(new EchoHandlerFactory());
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
