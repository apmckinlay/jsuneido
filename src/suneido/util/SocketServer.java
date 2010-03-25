package suneido.util;

import static suneido.util.Util.stringToBuffer;

import java.io.IOException;
import java.net.*;
import java.nio.ByteBuffer;
import java.nio.channels.*;
import java.util.*;
import java.util.concurrent.*;

import javax.annotation.concurrent.Immutable;
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
	private Selector selector;
	private static final int INITIAL_BUFSIZE = 4096;
	private static final int IDLE_TIMEOUT = 4 * 60 * 60 * 1000; // 4 hours
	public static final ScheduledExecutorService scheduler
			= Executors.newSingleThreadScheduledExecutor();

	public SocketServer(HandlerFactory handlerFactory) {
		this.handlerFactory = handlerFactory;
		scheduler.scheduleAtFixedRate(new CloseIdleConnections(),
				1, 1, TimeUnit.MINUTES);
	}

	public void run(int port) throws IOException {
		ServerSocketChannel serverChannel = ServerSocketChannel.open();
		ServerSocket serverSocket = serverChannel.socket();
		serverSocket.bind(new InetSocketAddress(port));
		selector = Selector.open();
		registerChannel(serverChannel, SelectionKey.OP_ACCEPT);
		while (true) {
			int nready = selector.select();
			if (nready > 0)
				handleSelected();
		}
	}

	private void handleSelected() throws IOException {
		Iterator<SelectionKey> iter = selector.selectedKeys().iterator();
		while (iter.hasNext()) {
			SelectionKey key = iter.next();
			if (key.isAcceptable())
				accept(key);
			if (key.isReadable())
				read(key);
			if (key.isValid() && key.isWritable())
				write(key);
			iter.remove();
		}
	}

	private void accept(SelectionKey key)
			throws IOException {
		ServerSocketChannel server = (ServerSocketChannel) key.channel();
		SocketChannel channel = server.accept();
		if (channel == null)
			return;
		SelectionKey key2 = registerChannel(channel, SelectionKey.OP_READ);
		if (key2 == null)
			return;
		InetAddress adr = channel.socket().getInetAddress();
		Handler handler = handlerFactory.newHandler(new OutputQueue(key2),
				adr.getHostAddress());
		key2.attach(new Info(handler));
		handler.start();
	}

	private SelectionKey registerChannel(SelectableChannel channel, int ops)
			throws IOException {
		channel.configureBlocking(false);
		return channel.register(selector, ops);
	}

	private static void read(SelectionKey key)
			throws IOException {
		SocketChannel channel = (SocketChannel) key.channel();
		Info info = (Info) key.attachment();
		ByteBuffer buf = info.readBuf;
		int n;
		do {
			if (buf.remaining() == 0) {
				ByteBuffer oldbuf = buf;
				buf = ByteBuffer.allocate(2 * oldbuf.capacity());
				oldbuf.flip();
				buf.put(oldbuf);
				info.readBuf = buf;
			}
			try {
				n = channel.read(buf);
			} catch (IOException e) {
				// we get this if the client aborts the connection
				n = -1;
			}
		} while (n > 0);
		buf.flip();
		info.handler.moreInput(buf);
		if (buf.remaining() > 0) {
			assert buf.position() == 0;
			buf.compact();
		} else {
			// input has been consumed
			assert buf.remaining() == 0;
			if (buf.capacity() == INITIAL_BUFSIZE)
				buf.clear();
			else // don't hold onto big buffers
				info.readBuf = ByteBuffer.allocate(INITIAL_BUFSIZE);
		}
		if (n < 0) {
			info.handler.close();
			channel.close();
		}
	}

	private static void write(SelectionKey key) throws IOException {
		if (write2(key))
			key.interestOps(SelectionKey.OP_READ); // turn off write interest
	}

	private static boolean write2(SelectionKey key) throws IOException {
		SocketChannel channel = (SocketChannel) key.channel();
		Info info = (Info) key.attachment();
		channel.write(info.writeBufs);
		return bufsEmpty(info.writeBufs);
	}

	private static boolean bufsEmpty(ByteBuffer[] bufs) {
		for (ByteBuffer b : bufs)
			if (b.remaining() > 0)
				return false;
		return true; // everything written
	}

	private static class Info {
		long lastActivity;
		final Handler handler;
		ByteBuffer readBuf = ByteBuffer.allocate(INITIAL_BUFSIZE);
		ByteBuffer[] writeBufs = new ByteBuffer[0]; // set by OutputQueue.write
		Info(Handler handler) {
			this.handler = handler;
			lastActivity = System.currentTimeMillis();
		}
	}

	@Immutable
	public static class OutputQueue {
		private static final ByteBuffer[] ByteBufferArray = new ByteBuffer[0];
		private final SelectionKey key;
		private final List<ByteBuffer> writeQueue = new ArrayList<ByteBuffer>();

		public OutputQueue(SelectionKey key) { // public for tests
			this.key = key;
		}
		public void add(ByteBuffer output) {
			writeQueue.add(output);
		}
		public void write() {
			Info info = (Info) key.attachment();
			info.lastActivity = System.currentTimeMillis();
			info.writeBufs = writeQueue.toArray(ByteBufferArray);
			writeQueue.clear();
			try {
				if (SocketServer.write2(key))
					return;
			} catch (IOException e) {
				e.printStackTrace();
			}
			key.interestOps(SelectionKey.OP_READ | SelectionKey.OP_WRITE);
			key.selector().wakeup();
		}
		public void closeChannel() {
			try {
				key.channel().close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	private class CloseIdleConnections implements Runnable {
		public void run() {
			long t = System.currentTimeMillis();
			for (SelectionKey key : selector.keys()) {
				Info info = (Info) key.attachment();
				if (info != null && t - info.lastActivity > IDLE_TIMEOUT) {
					info.handler.close();
					try {
						key.channel().close();
					} catch (IOException e) {
						e.printStackTrace();
					}
				}
			}
		}
	}

	public static interface HandlerFactory {
		public Handler newHandler(OutputQueue outputQueue, String address);
	}

	public static interface Handler {
		public void start();
		public void moreInput(ByteBuffer buf);
		public void close();
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
	static class EchoHandler implements Handler {
		private static ByteBuffer hello = stringToBuffer("EchoServer\r\n");
		OutputQueue outputQueue;

		EchoHandler(OutputQueue outputQueue) {
			this.outputQueue = outputQueue;
		}

		@Override
		public void start() {
			hello.rewind();
			outputQueue.add(hello);
		}

		@Override
		public void moreInput(ByteBuffer buf) {
			ByteBuffer output = ByteBuffer.allocate(buf.remaining());
			output.put(buf).rewind();
			outputQueue.add(output);
		}

		@Override
		public void close() {
		}
	}
	static class EchoHandlerFactory implements HandlerFactory {
		@Override
		public Handler newHandler(OutputQueue outputQueue, String address) {
			return new EchoHandler(outputQueue);
		}
	}

}
