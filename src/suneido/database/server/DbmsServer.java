package suneido.database.server;

import static suneido.database.Database.theDB;
import static suneido.util.Util.stringToBuffer;

import java.io.IOException;
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
import suneido.util.SocketServer;
import suneido.util.SocketServer.OutputQueue;

public class DbmsServer {
	private static final Executor executor = Executors.newCachedThreadPool();
	@GuardedBy("serverDataSet")
	static final Set<ServerData> serverDataSet = new HashSet<ServerData>();

	public static void run(int port) {
		Database.open_theDB();
		Globals.builtin("Print", new Repl.Print());
		Compiler.eval("JInit()");
		SocketServer server = new SocketServer(new HandlerFactory());
		SocketServer.scheduler.scheduleAtFixedRate(new Runnable() {
				public void run() {
					theDB.limitOutstandingTransactions();
				}
			}, 1, 1, TimeUnit.SECONDS);
		SocketServer.scheduler.scheduleAtFixedRate(new Runnable() {
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

	private static class HandlerFactory implements SocketServer.HandlerFactory {
		@Override
		public SocketServer.Handler newHandler(OutputQueue outputQueue,
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
	private static class Handler implements SocketServer.Handler, Runnable {
		private final OutputQueue outputQueue;
		private static final ByteBuffer hello = stringToBuffer("jSuneido Server\r\n");
		private int linelen = 0;
		private Command cmd = null;
		private int cmdlen = 0;
		private int nExtra = -1;
		private final ServerData serverData;
		private ByteBuffer line;
		private ByteBuffer extra;

		Handler(OutputQueue outputQueue, String address) {
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
		public void moreInput(ByteBuffer buf) {
			// first state = waiting for newline
			if (linelen == 0) {
				ByteBuffer line = getLine(buf);
				if (line == null)
					return;
				linelen = line.remaining();
//System.out.print(">" + bufferToString(line));
				cmd = getCmd(line);
				cmdlen = line.position();
				nExtra = cmd.extra(line);
			}
			// next state = waiting for extra data (if any)
			if (nExtra != -1 && buf.remaining() >= linelen + nExtra) {
				assert buf.position() == 0;

				buf.position(cmdlen);
				buf.limit(linelen);
				line = buf.slice();

				buf.position(linelen);
				buf.limit(linelen + nExtra);
				extra = buf.slice();

				buf.position(buf.limit()); // consume all input
				// since synchronous, it's safe to discard extra input
				linelen = 0;
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
			for (int i = 0; i < buf.remaining(); ++i)
				if (buf.get(i) == (byte) '\n')
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
		public void run() {
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

		private String escape(String s) {
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
