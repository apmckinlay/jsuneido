package suneido.database.server;

import static suneido.database.Database.theDB;
import static suneido.util.Util.stringToBuffer;

import java.io.IOException;
import java.net.InetAddress;
import java.nio.ByteBuffer;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.*;

import javax.annotation.concurrent.GuardedBy;
import javax.annotation.concurrent.NotThreadSafe;

import suneido.Repl;
import suneido.SuException;
import suneido.database.Database;
import suneido.language.Compiler;
import suneido.language.Globals;
import suneido.util.NetworkOutput;
import suneido.util.ServerBySelect;

public class DbmsServerBySelect {
	private static final Executor executor = Executors.newCachedThreadPool();
	@GuardedBy("serverDataSet")
	static final Set<ServerData> serverDataSet = new HashSet<ServerData>();
	private static InetAddress inetAddress;

	public static void run(int port) {
		Database.open_theDB();
		Globals.builtin("Print", new Repl.Print());
		Compiler.eval("JInit()");
		ServerBySelect server = new ServerBySelect(new HandlerFactory());
		inetAddress = server.getInetAddress();
		ServerBySelect.scheduler.scheduleAtFixedRate(new Runnable() {
				public void run() {
					theDB.limitOutstandingTransactions();
				}
			}, 1, 1, TimeUnit.SECONDS);
		ServerBySelect.scheduler.scheduleAtFixedRate(new Runnable() {
				public void run() {
					theDB.dest.force();
				}
			}, 1, 1, TimeUnit.MINUTES);
		try {
			server.run(port);
		} catch (IOException e) {
			e.printStackTrace();
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
		private static final ByteBuffer hello = stringToBuffer("jSuneido Server\r\n");
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
//System.out.print("> " + bufferToString(line));
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
if (!(e instanceof SuException))
e.printStackTrace();
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
	}

	public static void main(String[] args) {
		Compiler.eval("Use('Accountinglib')");
		Compiler.eval("Use('etalib')");
		Compiler.eval("Use('ticketlib')");
		Compiler.eval("Use('joblib')");
		Compiler.eval("Use('prlib')");
		Compiler.eval("Use('prcadlib')");
		Compiler.eval("Use('etaprlib')");
		Compiler.eval("Use('invenlib')");
		Compiler.eval("Use('wolib')");
		Compiler.eval("Use('polib')");
		Compiler.eval("Use('configlib')");
		Compiler.eval("Use('demobookoptions')");
		run(3147);
	}
}
