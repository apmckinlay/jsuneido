package suneido.database.server;

import static suneido.util.Util.stringToBuffer;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

import javax.annotation.concurrent.NotThreadSafe;

import suneido.database.*;
import suneido.language.Compiler;
import suneido.util.SocketServer;
import suneido.util.SocketServer.OutputQueue;

public class DbmsServer {
	private static final int PORT = 3147;
	private static final Executor executor = Executors.newCachedThreadPool();

	private static class HandlerFactory implements SocketServer.HandlerFactory {
		@Override
		public SocketServer.Handler newHandler(OutputQueue outputQueue) {
			return new Handler(outputQueue);
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
		private final ServerData serverData = new ServerData();
		private ByteBuffer line;
		private ByteBuffer extra;

		Handler(OutputQueue outputQueue) {
			this.outputQueue = outputQueue;
		}

		@Override
		public void start() {
			hello.rewind();
			outputQueue.add(hello);
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
				assert buf.limit() == linelen + nExtra;

				buf.position(cmdlen);
				buf.limit(linelen);
				line = buf.slice();

				buf.position(linelen);
				buf.limit(linelen + nExtra);
				extra = buf.slice();

				buf.position(linelen + nExtra); // consume input
				assert buf.remaining() == 0;
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
			return line.slice();
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
				if (output != null) // TODO handle OK etc. better
					output = output.duplicate();
			} catch (Throwable e) {
//e.printStackTrace();
//System.out.println("ERR " + err);
//System.out.println("FROM " + Util.bufferToString(line).trim());
//System.out.println("EXTRA " + Util.bufferToString(extra));
				output = stringToBuffer("ERR " + e.toString() + "\r\n");
			}
			if (output != null) {
				output.rewind();
				outputQueue.add(output);
			}
		}
	}


	public static void main(String[] args) {
		Mmfile mmf = new Mmfile("suneido.db", Mode.OPEN);
		Database.theDB = new Database(mmf, Mode.OPEN);
		Compiler.eval("JInit()");
		Compiler.eval("Use('Accountinglib')");
		SocketServer server = new SocketServer(new HandlerFactory());
		try {
			server.run(PORT);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}
