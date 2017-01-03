/* Copyright 2010 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido.util;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.nio.channels.*;
import java.util.Iterator;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.function.BiConsumer;
import java.util.function.Function;

import javax.annotation.concurrent.NotThreadSafe;

/**
 * Socket server framework using NIO Selector for accept and readability.
 * Uses a supplied function to create a new Handler for each accepted connection.
 * Constructs handler initially while channel is still in blocking mode.
 * It is up to handlers to create worker threads.
 * Does NOT doing any reading or writing.
 * Assumes request-response model.
 * Waits till channel is readable and then passes channel to handler.request
 * with the channel in blocking mode and unregistered from selector.
 * When the handler finishes reading the request and writing the response
 * it should reregister.
 * It is up to the handler to detect closed connections and close the channel.
 * Closes idle connections.
 */
@NotThreadSafe
public class ServerBySelect {
	private final Function<SocketChannel, Handler> handlerFactory;
	private Selector selector;
	private final int idleTimeoutMs;
	private static final int ONE_MINUTE_IN_MS = 60 * 1000;
	private static final int SELECT_TIMEOUT_MS = ONE_MINUTE_IN_MS;
	private static final int IDLE_CHECK_INTERVAL_MS = ONE_MINUTE_IN_MS;
	private long lastIdleCheck = System.currentTimeMillis();
	private final ConcurrentLinkedQueue<ChannelHandler> reregister
			= new ConcurrentLinkedQueue<>();

	public ServerBySelect(Function<SocketChannel, Handler> handlerFactory) {
		this(handlerFactory, 0);
	}

	public ServerBySelect(Function<SocketChannel, Handler> handlerFactory,
			int idleTimeoutMin) {
		this.handlerFactory = handlerFactory;
		this.idleTimeoutMs = idleTimeoutMin * ONE_MINUTE_IN_MS;
	}

	public void open(int port) {
		try {
			ServerSocketChannel serverChannel = ServerSocketChannel.open();
			ServerSocket serverSocket = serverChannel.socket();
			//serverSocket.setReuseAddress(true);
			serverSocket.bind(new InetSocketAddress(port));
			selector = Selector.open();
			registerChannel(serverChannel, SelectionKey.OP_ACCEPT);
		} catch (IOException e) {
			throw new RuntimeException("IOException in ServerBySelect.open", e);
		}
	}

	/** does not return */
	public void serve() {
		int errorCount = 0;
		while (true) {
			try {
				int nready = selector.select(SELECT_TIMEOUT_MS);
				// must be before handleSelected
				// so we don't try to reregister a key we just cancel'ed
				handleReregister();
				if (nready > 0)
					handleSelected();
				closeIdleConnections();
				errorCount = 0; // reset after success
			} catch (Throwable e) {
				Errlog.error("error in server loop", e);
				if (++errorCount > 100)
					Errlog.fatal("ServerBySelect too many consecutive errors");
			}
		}
	}

	private void handleReregister() throws IOException {
		ChannelHandler ch;
		while (null != (ch = reregister.poll())) {
			SelectionKey key = registerChannel(ch.channel, SelectionKey.OP_READ);
			key.attach(new Info(ch.handler));
		}
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
				readable(key);
		}
	}

	private void accept(SelectionKey key)
			throws IOException {
		ServerSocketChannel server = (ServerSocketChannel) key.channel();
		SocketChannel channel = server.accept();
		if (channel == null)
			return;
		Handler handler = handlerFactory.apply(channel);
		SelectionKey key2 = registerChannel(channel, SelectionKey.OP_READ);
		key2.attach(new Info(handler));
	}

	private SelectionKey registerChannel(SelectableChannel channel, int op)
			throws IOException {
		channel.configureBlocking(false);
		return channel.register(selector, op);
	}

	private static class Info {
		volatile long idleSince = 0;
		final Handler handler;

		Info(Handler handler) {
			this.handler = handler;
		}
	}

	private void readable(SelectionKey key) throws IOException {
		SocketChannel channel = (SocketChannel) key.channel();
		Info info = (Info) key.attachment();
		info.idleSince = 0;
		key.cancel(); // stop monitoring while handler running
		channel.configureBlocking(true);
		info.handler.request(channel, this::reregister);
	}

	/** called by handlers when they finish handling a request (thread safe) */
	private void reregister(Channel channel, Handler handler) {
		// use a queue to avoid messing with the selector from multiple threads
		reregister.add(new ChannelHandler((SocketChannel) channel, handler));
		selector.wakeup();
	}
	private static class ChannelHandler {
		final SocketChannel channel;
		final Handler handler;
		public ChannelHandler(SocketChannel channel, Handler handler) {
			this.channel = channel;
			this.handler = handler;
		}
	}

	private void closeIdleConnections() throws IOException {
		if (idleTimeoutMs == 0)
			return;
		long t = System.currentTimeMillis();
		long d = t - lastIdleCheck;
		if (d < IDLE_CHECK_INTERVAL_MS)
			return;
		if (d > 4 * IDLE_CHECK_INTERVAL_MS)
			Errlog.info("closeIdleConnections has not run for " + d + " ms, " +
					"idleTimeoutMs is " + idleTimeoutMs);
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

	// =========================================================================

	/**
	 * Constructed for each new connection that is accepted,
	 * with the channel in blocking mode.
	 */
	public interface Handler {
		/**
		 * Called when the channel is readable (but no data has been read)
		 * with the channel in blocking mode.
		 * Any lengthy processing should be done in a separate thread.
		 * Should close the channel when connection is broken.
		 */
		void request(Channel channel, BiConsumer<Channel, Handler> reregister);

		/** used when closing idle connections */
		void close();
	}

	// demo ====================================================================

//	public static void main(String[] args) {
//		ServerBySelect server =
//				new ServerBySelect(EchoHandler::new, 1); // 1 min timeout
//		server.open(1234);
//		server.serve();
//	}
//
//	static class EchoHandler implements Handler {
//		private static final Executor executor =
//				Executors.newCachedThreadPool();
//		private static ByteBuffer hello =
//				ByteBuffers.stringToBuffer("EchoServer\r\n");
//		ByteBuffer buf = ByteBuffer.allocate(1000);
//		private static Random rand = new Random();
//
//		EchoHandler(Channel channel) {
//			hello.rewind();
//			try {
//				((WritableByteChannel) channel).write(hello);
//			} catch (IOException e) {
//				e.printStackTrace();
//			}
//		}
//
//		@Override
//		public void request(Channel channel,
//				BiConsumer<Channel, Handler> reregister) {
//			switch (rand.nextInt(3)) {
//			case 0: // single threaded
//				handleRequest(channel, reregister);
//				break;
//			case 1: // thread per request
//				new Thread(() -> handleRequest(channel, reregister)).start();
//				break;
//			case 2: // thread pool
//				executor.execute(() -> handleRequest(channel, reregister));
//				break;
//			}
//		}
//
//		private void handleRequest(Channel channel,
//				BiConsumer<Channel, Handler> reregister) {
//			try {
//				buf.clear();
//				if (-1 != ((ReadableByteChannel) channel).read(buf)) {
//					buf.flip();
//					((WritableByteChannel) channel).write(buf);
//					reregister.accept(channel, this);
//				} else
//					channel.close();
//			} catch (IOException e) {
//				e.printStackTrace();
//				try {
//					channel.close();
//				} catch (IOException ec) {
//					e.printStackTrace();
//				}
//			}
//		}
//
//		@Override
//		public void close() {
//			System.out.println("closing (idle)");
//		}
//	}

}
