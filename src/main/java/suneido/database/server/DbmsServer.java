/* Copyright 2016 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido.database.server;

import static suneido.util.ByteBuffers.stringToBuffer;

import java.io.IOException;
import java.net.InetAddress;
import java.nio.ByteBuffer;
import java.nio.channels.Channel;
import java.nio.channels.SocketChannel;
import java.nio.channels.WritableByteChannel;
import java.util.*;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.function.BiConsumer;

import javax.annotation.concurrent.NotThreadSafe;
import javax.annotation.concurrent.ThreadSafe;

import com.google.common.base.Strings;
import com.google.common.util.concurrent.ThreadFactoryBuilder;

import suneido.Build;
import suneido.SuException;
import suneido.util.Errlog;
import suneido.util.ServerBySelect;
import suneido.util.ServerBySelect.Handler;

/**
 * Server side of the client-server protocol.
 * {@link DbmsClient} is the client side.
 * Based on {@link suneido.util.ServerBySelect}.
 * See {@link Command} for the actual request implementations.
 */
public class DbmsServer {
	public final ServerDataSet serverDataSet = new ServerDataSet();
	private final ServerBySelect server;
	public static final int helloSize = 50; // must match cSuneido

	public DbmsServer(int idleTimeoutMin) {
		server = new ServerBySelect(
				(SocketChannel c) -> { return new DbmsServerHandler(c, serverDataSet); },
				idleTimeoutMin);
	}

	public void open(int port) {
		server.open(port);
	}

	/** does not return */
	public void serve() {
		server.serve();
	}

	/**
	 * There is an instance of Handler for each open connection.
	 * Each handler has ServerData which tracks open transactions, queries, etc.
	 * A handler is constructed when a new connection is accepted.
	 * The request method is called each time the channel becomes readable.
	 * The request method reads the request, executes it, writes the response
	 * and then reregisters the channel with the selector.
	 */
	@NotThreadSafe
	static class DbmsServerHandler implements Handler {
		private final ThreadFactory threadFactory =
				new ThreadFactoryBuilder().setNameFormat("DbmsServer-thread-%d").build();
		private final Executor executor = Executors.newCachedThreadPool(threadFactory);
		private static final ThreadLocal<ByteBuffer> tlbuf =
				ThreadLocal.withInitial(() -> ByteBuffer.allocateDirect(SuChannel.BUFSIZE));
		// avoid calling values every time since it clones
		private static final Command[] commands = Command.values();
		private final ServerDataSet serverDataSet;
		private final ServerData serverData;
		private static class InitOnce {
			static final ByteBuffer hello = hello();
		}

		DbmsServerHandler(Channel channel, ServerDataSet serverDataSet) {
			sendHello(channel);
			this.serverDataSet = serverDataSet;
			serverData = new ServerData(channel);
			if (channel instanceof SocketChannel) {
				InetAddress adr = ((SocketChannel) channel).socket().getInetAddress();
				serverData.setSessionId(adr.getHostAddress());
			}
			serverDataSet.add(serverData);
		}

		private static void sendHello(Channel channel) {
			ByteBuffer hello = InitOnce.hello;
			hello.rewind();
			try {
				((WritableByteChannel) channel).write(hello);
			} catch (IOException e) {
				throw new RuntimeException("error writing hello", e);
			}
		}

		private static ByteBuffer hello() {
			String hello = "Suneido " + Build.desc() + "\r\n";
			assert hello.length() < helloSize;
			return stringToBuffer(Strings.padEnd(hello, helloSize, '\000'));
		}

		@Override
		public void request(Channel channel,
				BiConsumer<Channel, Handler> reregister) {
			executor.execute(() -> handleRequest(channel, reregister));
		}

		void handleRequest(Channel channel,
				BiConsumer<Channel, Handler> reregister) {
			try {
				// create a new SuChannel for each request
				SuChannel io = new SuChannel(channel, tlbuf.get());
				int icmd = io.getByte();
				Command cmd = commands[icmd];
				ServerData.threadLocal.set(serverData);
				try {
					cmd.execute(io);
				} catch (Throwable e) {
					Class<? extends Throwable> c = e.getClass();
					if (c != RuntimeException.class && c != SuException.class)
						Errlog.error("DbmsServerBySelect.run", e);
					io.clear();
					io.put(false).put(e.toString());
				}
				io.write();
				reregister.accept(channel, this);
			} catch (Throwable e) {
				try {
					channel.close();
					close();
				} catch (IOException e1) {
				}
			}
		}

		@Override
		public void close() {
			serverData.end();
			serverDataSet.remove(serverData);
		}

		@Override
		public String toString() {
			return serverData.getSessionId();
		}

	}

	//--------------------------------------------------------------------------

	@ThreadSafe
	static class ServerDataSet {
		private final Set<ServerData> data = new HashSet<>();

		synchronized void add(ServerData serverData) {
			data.add(serverData);
		}

		synchronized void remove(ServerData serverData) {
			data.remove(serverData);
		}

		synchronized List<String> connections() {
			List<String> list = new ArrayList<>();
			for (ServerData sd : data)
				list.add(sd.getSessionId());
			return list;
		}

		synchronized int killConnections(String sessionId) {
			int nkilled = 0;
			Iterator<ServerData> iter = data.iterator();
			while (iter.hasNext()) {
				ServerData serverData = iter.next();
				if (sessionId.equals(serverData.getSessionId())) {
					++nkilled;
					serverData.end();
					try {
						serverData.connection.close();
					} catch (IOException e) {
					}
					iter.remove();
				}
			}
			return nkilled;
		}
	}

	public List<String> connections() {
		return serverDataSet.connections();
	}

	public int killConnections(String sessionId) {
		return serverDataSet.killConnections(sessionId);
	}

}
