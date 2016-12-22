/* Copyright 2010 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido.database.server;

import static suneido.util.ByteBuffers.stringToBuffer;

import java.io.IOException;
import java.net.InetAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;

import javax.annotation.concurrent.NotThreadSafe;
import javax.annotation.concurrent.ThreadSafe;

import com.google.common.util.concurrent.ThreadFactoryBuilder;

import suneido.SuException;
import suneido.Suneido;
import suneido.util.Errlog;
import suneido.util.ServerBySelect;

/**
 * Uses {@link suneido.util.ServerBySelect}
 */
public class DbmsServerBySelect {
	private final ThreadFactory threadFactory =
		new ThreadFactoryBuilder().setNameFormat("DbmsServer-thread-%d").build();
	private final Executor executor = Executors.newCachedThreadPool(threadFactory);
	private static final ByteBuffer hello = stringToBuffer(
			"Suneido Database Server (" + Suneido.cmdlineoptions.impersonate + ")\r\n");
	private static final int BUFSIZE = 16 * 1024;
	private static final ThreadLocal<ByteBuffer> tlbuf = 
			ThreadLocal.withInitial(() -> ByteBuffer.allocate(BUFSIZE));
	private ServerBySelect server;
	public ServerDataSet serverDataSet = new ServerDataSet();
	
	public DbmsServerBySelect(int idleTimeoutMin) {
		server = new ServerBySelect(Handler::new, idleTimeoutMin);
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
	private class Handler implements ServerBySelect.Handler {
		private final ServerData serverData;

		Handler(SocketChannel channel) {
			serverData = new ServerData(channel);
			InetAddress adr = channel.socket().getInetAddress();
			serverData.setSessionId(adr.getHostAddress());
			serverDataSet.add(serverData);
			hello.rewind();
			try {
				channel.write(hello);
			} catch (IOException e) {
			}
		}

		@Override
		public void request(SocketChannel channel) {
			executor.execute(() -> handleRequest(channel));
		}

		private void handleRequest(SocketChannel channel) {
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
				Command cmd = getCmd(word);
				int nExtra = cmd.extra(line);
				
				ByteBuffer extra = nExtra > BUFSIZE ||
						cmd == Command.OUTPUT || cmd == Command.UPDATE
						? newBuf(buf, nExtra)
						: buf;
				while (extra.position() < nExtra)
					channel.read(extra);
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
				server.reregister(channel, this);
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

	private static int indexOf(ByteBuffer buf, int pos, byte b) {
		for (int i = pos; i < buf.position(); ++i)
			if (buf.get(i) == b)
				return i;
		return -1;
	}

	private static String readline(SocketChannel channel, ByteBuffer buf) throws IOException {
		buf.clear();
		int pos = 0;
		while (true) {
			if (buf.remaining() == 0) // line too long
				return null; // ???
			if (-1 == channel.read(buf))
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
		String s = new String(buf.array(), buf.arrayOffset(), len, 
				StandardCharsets.ISO_8859_1);
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

	private static Command getCmd(String word) {
		try {
			if (word.isEmpty())
				return Command.NILCMD;
			return Command.valueOf(word.toUpperCase());
		} catch (IllegalArgumentException e) {
			return Command.BADCMD;
		}
	}

	private static void send(SocketChannel channel, ArrayList<ByteBuffer> writeBufs)
			throws IOException {
		ByteBuffer[] bufs = writeBufs.toArray(new ByteBuffer[0]);
		while (! bufsEmpty(bufs))
			channel.write(bufs);
	}
	private static boolean bufsEmpty(ByteBuffer[] bufs) {
		for (ByteBuffer b : bufs)
			if (b.remaining() > 0)
				return false;
		return true; // everything written
	}

	private static String escape(String s) {
		return s.replace("\r", "\\r").replace("\n", "\\n");
	}
	
	@ThreadSafe
	private static class ServerDataSet {
		private final Set<ServerData> serverDataSet = new HashSet<>();
		
		synchronized void add(ServerData serverData) {
			serverDataSet.add(serverData);
		}
		
		synchronized void remove(ServerData serverData) {
			serverDataSet.remove(serverData);
		}
		
		synchronized List<String> connections() {
			List<String> list = new ArrayList<>();
			for (ServerData sd : serverDataSet)
				list.add(sd.getSessionId());
			return list;
		}

		synchronized int killConnections(String sessionId) {
			int nkilled = 0;
			Iterator<ServerData> iter = serverDataSet.iterator();
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
