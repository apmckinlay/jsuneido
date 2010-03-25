package suneido.util;

import static suneido.util.Util.stringToBuffer;

import java.io.IOException;
import java.net.*;
import java.nio.ByteBuffer;
import java.nio.channels.*;
import java.util.*;

import javax.annotation.concurrent.Immutable;
import javax.annotation.concurrent.NotThreadSafe;

import suneido.SuException;

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
	private static final int SELECT_TIMEOUT = 1000; // in ms, so = 1 sec
	private static final long CHECK_INTERVAL = 60 * 1000; // 1 min
	private static final int IDLE_TIMEOUT = 4 * 60 * 60 * 1000; // 4 hours
	private static long lastCheck = 0;

	public SocketServer(HandlerFactory handlerFactory) {
		this.handlerFactory = handlerFactory;
	}

	public void run(int port) throws IOException {
		ServerSocketChannel serverChannel = ServerSocketChannel.open();
		ServerSocket serverSocket = serverChannel.socket();
		serverSocket.bind(new InetSocketAddress(port));
		selector = Selector.open();
		registerChannel(serverChannel, SelectionKey.OP_ACCEPT);
		while (true) {
			int nready = selector.select(SELECT_TIMEOUT);
			if (nready > 0)
				handleSelected();
			else
				tick();
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
		info.lastActivity = System.currentTimeMillis();
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
		/** does NOT call handler.close */
		public void close() {
			try {
				key.channel().close();
			} catch (IOException e) {
				throw new SuException("error closing connection", e);
			}
		}
	}

	private void tick() throws IOException {
		handlerFactory.fastTick();
		long t = System.currentTimeMillis();
		if (t - lastCheck > CHECK_INTERVAL) {
			lastCheck = t;
			closeIdleConnections();
			handlerFactory.slowTick();
		}
	}

	private void closeIdleConnections() throws IOException {
		for (SelectionKey key : selector.keys()) {
			Info info = (Info) key.attachment();
			if (info != null && lastCheck - info.lastActivity > IDLE_TIMEOUT)
				key.channel().close();
		}
	}

	public static abstract class HandlerFactory {
		public abstract Handler newHandler(OutputQueue outputQueue, String address);
		public void fastTick() {
		}
		public void slowTick() {
		}
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
	static class EchoHandlerFactory extends HandlerFactory {
		@Override
		public Handler newHandler(OutputQueue outputQueue, String address) {
			return new EchoHandler(outputQueue);
		}
	}

}
