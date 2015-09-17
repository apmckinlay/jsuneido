/* Copyright 2010 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido.database.server;

import static suneido.util.ByteBuffers.stringToBuffer;

import java.io.IOException;
import java.net.InetAddress;
import java.nio.ByteBuffer;
import java.util.*;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;

import javax.annotation.concurrent.GuardedBy;
import javax.annotation.concurrent.NotThreadSafe;

import com.google.common.util.concurrent.ThreadFactoryBuilder;

import suneido.SuException;
import suneido.Suneido;
import suneido.util.Errlog;
import suneido.util.NetworkOutput;
import suneido.util.ServerBySelect;

/**
 * Uses {@link suneido.util.ServerBySelect}
 */
public class DbmsServerBySelect {
	private static final ThreadFactory threadFactory =
		new ThreadFactoryBuilder()
			.setNameFormat("DbmsServer-thread-%d")
			.build();
	private static final Executor executor =
		Executors.newCachedThreadPool(threadFactory);
	@GuardedBy("serverDataSet")
	static final Set<ServerData> serverDataSet = new HashSet<>();
	private static InetAddress inetAddress;

	public static void run(int port, int idleTimeoutMin) {
		ServerBySelect server = new ServerBySelect(new HandlerFactory(), idleTimeoutMin);
		inetAddress = server.getInetAddress();
		try {
			server.run(port);
		} catch (IOException e) {
			Errlog.error("IOException in ServerBySelect.run", e);
		}
	}

	public static InetAddress getInetAddress() {
		return inetAddress;
	}

	private static class HandlerFactory implements ServerBySelect.HandlerFactory {
		@Override
		public ServerBySelect.Handler newHandler(NetworkOutput outputQueue,
				String address) {
			return new Handler(outputQueue, address);
		}
	}

	/**
	 * There is an instance of Handler for each open connection. Once a complete
	 * request has been received, it is executed in a separate thread. Since
	 * requests do not overlap, moreInput will not be called while a request is
	 * executing.
	 */
	@NotThreadSafe
	private static class Handler implements ServerBySelect.Handler, Runnable {
		private static final ByteBuffer hello = stringToBuffer(
				"Suneido Database Server (" + Suneido.cmdlineoptions.impersonate + ")\r\n");
		private final NetworkOutput outputQueue;
		private final ServerData serverData;
		private volatile int linelen = -1;
		private volatile Command cmd = null;
		private volatile int nExtra = -1;
		private volatile ByteBuffer line;
		private volatile ByteBuffer extra;

		Handler(NetworkOutput outputQueue, String address) {
			this.outputQueue = outputQueue;
			serverData = new ServerData(outputQueue);
			serverData.setSessionId(address);
			synchronized(serverDataSet) {
				serverDataSet.add(serverData);
			}
		}

		@Override
		public void start() {
			outputQueue.add(hello.duplicate());
			outputQueue.write();
		}

		@Override
		public synchronized void moreInput(ByteBuffer buf) {
			// first state = waiting for newline
			if (linelen == -1) {
				line = getLine(buf);
				if (line == null)
					return;
//System.out.print("> " + ByteBuffers.bufferToString(line));
				linelen = line.remaining();
				cmd = getCmd(line);
				line = line.slice();
				nExtra = cmd.extra(line);
				line.position(0);
			}
			// next state = waiting for extra data (if any)
			if (nExtra != -1 && buf.remaining() >= linelen + nExtra) {
				assert buf.position() == 0;

				buf.position(linelen);
				buf.limit(linelen + nExtra);
				extra = buf.slice();

				buf.position(buf.limit()); // consume all input
				// since synchronous, it's safe to discard extra input
				linelen = -1;
				nExtra = -1;
				executor.execute(this);
			}
		}

		private static ByteBuffer getLine(ByteBuffer buf) {
			int nlPos = indexOf(buf, (byte) '\n');
			if (nlPos == -1)
				return null;
			ByteBuffer line = buf.duplicate();
			line.position(0);
			line.limit(nlPos + 1);
			return line;
		}
		private static int indexOf(ByteBuffer buf, byte b) {
			// NOTE: use buf.remaining() since buf is flipped
			for (int i = 0; i < buf.remaining(); ++i)
				if (buf.get(i) == b)
					return i;
			return -1;
		}

		private static Command getCmd(ByteBuffer buf) {
			try {
				String word = firstWord(buf);
				if (word.isEmpty())
					return Command.NILCMD;
				return Command.valueOf(word.toUpperCase());
			} catch (IllegalArgumentException e) {
				return Command.BADCMD;
			}
		}
		private static String firstWord(ByteBuffer buf) {
			StringBuilder sb = new StringBuilder();
			buf.position(0);
			while (buf.remaining() > 0) {
				char c = (char) buf.get();
				if (c == ' ' || c == '\r' || c == '\n')
					break ;
				sb.append(c);
			}
			return sb.toString();
		}

		@Override
		public synchronized void run() {
			ByteBuffer output = null;
			try {
				ServerData.threadLocal.set(serverData);
				output = cmd.execute(line, extra, outputQueue);
			} catch (Throwable e) {
				Class<? extends Throwable> c = e.getClass();
				if (c != RuntimeException.class && c != SuException.class)
					Errlog.error("DbmsServerBySelect.run", e);
				output = stringToBuffer("ERR " + escape(e.toString()) + "\r\n");
			}
			line = null;
			extra = null;
			if (output != null)
				outputQueue.add(output);
			outputQueue.write();
		}

		private static String escape(String s) {
			return s.replace("\r", "\\r").replace("\n", "\\n");
		}

		@Override
		public void close() {
			synchronized(serverDataSet) {
				serverData.end();
				serverDataSet.remove(serverData);
			}
		}

		@Override
		public String toString() {
			return serverData.getSessionId();
		}
	}

	public static List<String> connections() {
		List<String> list = new ArrayList<>();
		synchronized(serverDataSet) {
			for (ServerData sd : serverDataSet)
				list.add(sd.getSessionId());
		}
		return list;
	}

	static int kill_connections(String sessionId) {
		int nkilled = 0;
		synchronized(DbmsServer.serverDataSet) {
			Iterator<ServerData> iter = DbmsServer.serverDataSet.iterator();
			while (iter.hasNext()) {
				ServerData serverData = iter.next();
				if (sessionId.equals(serverData.getSessionId())) {
					++nkilled;
					serverData.end();
					serverData.outputQueue.close();
					iter.remove();
				}
			}
		}
		return nkilled;
	}

}
