/* Copyright 2010 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido.util;

import static suneido.util.Util.stringToBuffer;

import java.io.IOException;
import java.net.*;
import java.nio.ByteBuffer;
import java.nio.channels.*;
import java.util.*;
import java.util.concurrent.ConcurrentSkipListSet;

import javax.annotation.concurrent.NotThreadSafe;

import suneido.Suneido;

/**
 * Socket server framework using NIO Selector and non-blocking channels.
 * Uses a supplied HandlerFactory to create a new
 * Handler for each accepted connection. Calls handler.start initially. Then
 * calls handler.moreInput each time more data is read. Input is accumulated
 * until moreInput consumes it, presumably after a complete request has been
 * received. The handler is given an OutputQueue to send its output to. Output
 * is gather written asynchronously. OutputQueue's are synchronized. It is up
 * to handlers to create worker threads if desired.
 */
@NotThreadSafe
public class ServerBySelect {
	private final HandlerFactory handlerFactory;
	private InetAddress inetAddress;
	private Selector selector;
	private static final int INITIAL_BUFSIZE = 16 * 1024;
	private static final int MAX_BUFSIZE = 64 * 1024;
	private final int idleTimeoutMs;
	private static final int ONE_MINUTE_IN_MS = 60 * 1000;
	private static final int SELECT_TIMEOUT_MS = ONE_MINUTE_IN_MS;
	private static final int IDLE_CHECK_INTERVAL_MS = ONE_MINUTE_IN_MS;
	private final Set<SelectionKey> needWrite
			= new ConcurrentSkipListSet<SelectionKey>(new IdentityComparator());
	private long lastIdleCheck = 0;

	public ServerBySelect(HandlerFactory handlerFactory) {
		this(handlerFactory, 0);
	}

	public ServerBySelect(HandlerFactory handlerFactory, int idleTimeoutMin) {
		this.handlerFactory = handlerFactory;
		this.idleTimeoutMs = idleTimeoutMin * ONE_MINUTE_IN_MS;
	}

	public void run(int port) throws IOException {
		ServerSocketChannel serverChannel = ServerSocketChannel.open();
		ServerSocket serverSocket = serverChannel.socket();
		serverSocket.bind(new InetSocketAddress(port));
		inetAddress = serverSocket.getInetAddress();
		selector = Selector.open();
		registerChannel(serverChannel, SelectionKey.OP_ACCEPT);
		while (true) {
			try {
				int nready = selector.select(SELECT_TIMEOUT_MS);
				if (nready > 0)
					handleSelected();
				handleWriters();
				closeIdleConnections();
			} catch (Throwable e) {
				Suneido.errlog("error in server loop", e);
			}
		}
	}

	public InetAddress getInetAddress() {
		return inetAddress;
	}

	private void handleSelected() throws IOException {
		Iterator<SelectionKey> iter = selector.selectedKeys().iterator();
		while (iter.hasNext()) {
			SelectionKey key = iter.next();
			iter.remove();
			if (!key.isValid())
				continue;
			if (key.isAcceptable())
				accept(key);
			else if (key.isReadable())
				read(key);
			else if (key.isWritable())
				write(key);
		}
	}

	private void handleWriters() {
		if (needWrite.isEmpty())
			return;
		Iterator<SelectionKey> iter = needWrite.iterator();
		while (iter.hasNext()) {
			SelectionKey key = iter.next();
			iter.remove();
			if (key.isValid())
				key.interestOps(SelectionKey.OP_WRITE);
		}
	}

	private void needWrite(SelectionKey key) {
		needWrite.add(key);
		selector.wakeup();
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
		Handler handler = handlerFactory.newHandler(new OutputQueue(this, key2),
				adr.getHostAddress());
		key2.attach(new Info(handler));
		handler.start();
	}

	private SelectionKey registerChannel(SelectableChannel channel, int ops)
			throws IOException {
		channel.configureBlocking(false);
		return channel.register(selector, ops);
	}

	private static class Info {
		long idleSince = 0;
		final Handler handler;
		ByteBuffer readBuf = ByteBuffer.allocateDirect(INITIAL_BUFSIZE);
		ByteBuffer[] writeBufs = new ByteBuffer[0]; // set by OutputQueue.write

		Info(Handler handler) {
			this.handler = handler;
		}
	}

	private static void read(SelectionKey key) {
		try {
			read2(key);
		} catch (Exception e) {
			errorClose(key, e);
		}
	}

	private static void read2(SelectionKey key)	throws IOException {
		SocketChannel channel = (SocketChannel) key.channel();
		Info info = (Info) key.attachment();
		ByteBuffer buf = info.readBuf;
		int n;
		do {
			if (buf.remaining() == 0) {
				ByteBuffer oldbuf = buf;
				buf = ByteBuffer.allocateDirect(2 * oldbuf.capacity());
				oldbuf.flip();
				buf.put(oldbuf);
				info.readBuf = buf;
			}
			n = channel.read(buf);
		} while (n > 0);
		if (n < 0) {
			close(key);
			return;
		}
		buf.flip();
		info.handler.moreInput(buf);
		if (buf.remaining() > 0) {
			assert buf.position() == 0;
			buf.compact();
		} else {
			// input has been consumed
			if (buf.capacity() <= MAX_BUFSIZE)
				buf.clear();
			else // don't keep buffer bigger than max
				info.readBuf = ByteBuffer.allocateDirect(MAX_BUFSIZE);
		}
	}

	private static void write(SelectionKey key) throws IOException {
		if (write2(key))
			if (key.isValid())
				key.interestOps(SelectionKey.OP_READ); // turn off write interest
	}

	// called by selector
	private static boolean write2(SelectionKey key) throws IOException {
		return channelWrite(key);
	}

	// called by worker
	private static boolean write3(SelectionKey key) throws IOException {
		// this synchronized should not be necessary
		// but it seems to prevent problems with garbage being sent sometimes
		synchronized(ServerBySocket.class) {
			return channelWrite(key);
		}
	}

	private static boolean channelWrite(SelectionKey key) {
		SocketChannel channel = (SocketChannel) key.channel();
		Info info = (Info) key.attachment();
		try {
			channel.write(info.writeBufs);
			return bufsEmpty(info.writeBufs);
		} catch (IOException e) {
			errorClose(key, e);
			return true;
		}
	}

	private static void errorClose(SelectionKey key, Exception e) {
		Info info = (Info) key.attachment();
		Suneido.errlog(info.handler + " write failed so closing", e);
		close(key);
	}

	private static void close(SelectionKey key) {
		Info info = (Info) key.attachment();
		info.handler.close();
		key.cancel();
		try {
			key.channel().close();
		} catch (IOException e) {
			// ignore error during close
		}
	}

	private static boolean bufsEmpty(ByteBuffer[] bufs) {
		for (ByteBuffer b : bufs)
			if (b.remaining() > 0)
				return false;
		return true; // everything written
	}

	public static class OutputQueue implements NetworkOutput {
		private static final ByteBuffer[] ByteBufferArray = new ByteBuffer[0];
		private final ServerBySelect selectServer;
		private final SelectionKey key;
		private final List<ByteBuffer> writeQueue = new ArrayList<ByteBuffer>();

		public OutputQueue(ServerBySelect selectServer, SelectionKey key) { // public for tests
			this.selectServer = selectServer;
			this.key = key;
		}
		public void add(ByteBuffer output) {
			writeQueue.add(output);
		}
		public void write() {
			Info info = (Info) key.attachment();
			info.idleSince = 0;
			info.writeBufs = writeQueue.toArray(ByteBufferArray);
			writeQueue.clear();
			try {
				if (ServerBySelect.write3(key))
					return;
			} catch (IOException e) {
				e.printStackTrace();
			}
			selectServer.needWrite(key);
		}
		public void close() {
			key.cancel();
			try {
				key.channel().close();
			} catch (IOException e) {
				// ignore errors during close
			}
		}
	}

	private void closeIdleConnections() throws IOException {
		if (idleTimeoutMs == 0)
			return;
		long t = System.currentTimeMillis();
		if (t - lastIdleCheck < IDLE_CHECK_INTERVAL_MS)
			return;
		lastIdleCheck = t;
		for (SelectionKey key : selector.keys()) {
			Info info = (Info) key.attachment();
			if (info == null)
				continue;
			if (info.idleSince == 0)
				info.idleSince = t;
			else if (t - info.idleSince > idleTimeoutMs) {
				Print.timestamped("closing idle connection " + info.handler);
				info.handler.close();
				key.channel().close();
			}
		}
	}

	public static interface HandlerFactory {
		public Handler newHandler(NetworkOutput outputQueue, String address);
	}

	public static interface Handler {
		public void start();
		public void moreInput(ByteBuffer buf);
		public void close();
	}

	//==========================================================================

	public static void main(String[] args) {
		ServerBySelect server = new ServerBySelect(new EchoHandlerFactory());
		try {
			server.run(1234);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	static class EchoHandler implements Handler {
		private static ByteBuffer hello = stringToBuffer("EchoServer\r\n");
		NetworkOutput outputQueue;

		EchoHandler(NetworkOutput outputQueue) {
			this.outputQueue = outputQueue;
		}

		@Override
		public void start() {
			hello.rewind();
			outputQueue.add(hello);
			outputQueue.write();
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
		public Handler newHandler(NetworkOutput outputQueue, String address) {
			return new EchoHandler(outputQueue);
		}
	}

}
