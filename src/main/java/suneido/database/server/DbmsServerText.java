/* Copyright 2010 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido.database.server;

import static suneido.util.ByteBuffers.bufferToString;
import static suneido.util.ByteBuffers.bufsEmpty;
import static suneido.util.ByteBuffers.indexOf;
import static suneido.util.ByteBuffers.stringToBuffer;

import java.io.IOException;
import java.net.InetAddress;
import java.nio.ByteBuffer;
import java.nio.channels.*;
import java.util.*;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.function.BiConsumer;

import javax.annotation.concurrent.NotThreadSafe;
import javax.annotation.concurrent.ThreadSafe;

import com.google.common.util.concurrent.ThreadFactoryBuilder;

import suneido.SuException;
import suneido.Suneido;
import suneido.util.Errlog;
import suneido.util.ServerBySelect;
import suneido.util.ServerBySelect.Handler;

/**
 * Server side of the *text* client-server protocol.
 * {@link DbmsClientText} is the client side.
 * Based on {@link suneido.util.ServerBySelect}
 * See {@link CommandText} for the actual request implementations.
 */
public class DbmsServerText {
	public final ServerDataSet serverDataSet = new ServerDataSet();
	private final ServerBySelect server;

	public DbmsServerText(int idleTimeoutMin) {
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
		private static final ByteBuffer hello = stringToBuffer(
				"Suneido Database Server (" + Suneido.cmdlineoptions.impersonate + ")\r\n");
		private static final int BUFSIZE = 16 * 1024;
		private static final ThreadLocal<ByteBuffer> tlbuf =
				ThreadLocal.withInitial(() -> ByteBuffer.allocate(BUFSIZE));
		private final ServerDataSet serverDataSet;
		private final ServerData serverData;

		DbmsServerHandler(Channel channel, ServerDataSet serverDataSet) {
			this.serverDataSet = serverDataSet;
			serverData = new ServerData(channel);
			if (channel instanceof SocketChannel) {
				InetAddress adr = ((SocketChannel) channel).socket().getInetAddress();
				serverData.setSessionId(adr.getHostAddress());
			}
			serverDataSet.add(serverData);
			hello.rewind();
			try {
				((WritableByteChannel) channel).write(hello);
			} catch (IOException e) {
				// ignore
			}
		}

		@Override
		public void request(Channel channel,
				BiConsumer<Channel, Handler> reregister) {
			executor.execute(() -> handleRequest(channel, reregister));
		}

		private void handleRequest(Channel channel,
				BiConsumer<Channel, Handler> reregister) {
			try {
				ByteBuffer buf = tlbuf.get();
				String line = readline(channel, buf);
				if (line == null) { // disconnected
					channel.close();
					close();
					return;
				}
				String word = firstWord(line);
				line = line.substring(word.length()).trim();
				CommandText cmd = getCmd(word);
				int nExtra = cmd.extra(line);

				ByteBuffer extra = nExtra > BUFSIZE ||
						cmd == CommandText.OUTPUT || cmd == CommandText.UPDATE
						? newBuf(buf, nExtra)
						: buf;
				while (extra.position() < nExtra)
					((ReadableByteChannel) channel).read(extra);
				extra.flip();

				ByteBuffer output = null;
				ArrayList<ByteBuffer> writeBufs = new ArrayList<>();
				try {
					ServerData.threadLocal.set(serverData);
					output = cmd.execute(line, extra, writeBufs::add);
				} catch (Throwable e) {
					Class<? extends Throwable> c = e.getClass();
					if (c != RuntimeException.class && c != SuException.class)
						Errlog.error("DbmsServerBySelect.run", e);
					output = stringToBuffer("ERR " + escape(e.toString()) + "\r\n");
				}
				if (output != null)
					writeBufs.add(output);
				send(channel, writeBufs);
				reregister.accept(channel, this);
			} catch (IOException e) {
				try {
					channel.close();
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

	private static ByteBuffer newBuf(ByteBuffer buf, int nExtra) {
		ByteBuffer dst = ByteBuffer.allocateDirect(nExtra);
		buf.flip();
		dst.put(buf);
		return dst;
	}

	private static String readline(Channel channel, ByteBuffer buf) throws IOException {
		buf.clear();
		int pos = 0;
		while (true) {
			if (buf.remaining() == 0) // line too long
				return null; // ???
			if (-1 == ((ReadableByteChannel) channel).read(buf))
				return null; // disconnected
			int i = indexOf(buf, pos, (byte) '\n');
			if (i != -1) {
				pos = i;
				break;
			}
			pos = buf.position();
		}
		int len = pos;
		if (buf.get(pos - 1) == '\r')
			--len;
		String s = bufferToString(buf, 0, len);
		// if we read more than just line, leave it at start of buf
		buf.limit(buf.position());
		buf.position(pos + 1);
		buf.compact();
		return s;
	}

	private static String firstWord(String line) {
		int i = line.indexOf(' ');
		return i == -1 ? line : line.substring(0, i);
	}

	private static CommandText getCmd(String word) {
		try {
			if (word.isEmpty())
				return CommandText.NILCMD;
			return CommandText.valueOf(word.toUpperCase());
		} catch (IllegalArgumentException e) {
			return CommandText.BADCMD;
		}
	}

	private static void send(Channel channel, ArrayList<ByteBuffer> writeBufs)
			throws IOException {
		ByteBuffer[] bufs = writeBufs.toArray(new ByteBuffer[0]);
		while (! bufsEmpty(bufs))
			((GatheringByteChannel) channel).write(bufs);
	}

	private static String escape(String s) {
		return s.replace("\r", "\\r").replace("\n", "\\n");
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
