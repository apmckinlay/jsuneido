package suneido.database.server;

import static suneido.util.Util.stringToBuffer;

import java.io.IOException;
import java.net.InetAddress;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.*;

import javax.annotation.concurrent.GuardedBy;
import javax.annotation.concurrent.NotThreadSafe;

import suneido.Repl;
import suneido.SuException;
import suneido.database.TheDb;
import suneido.language.Compiler;
import suneido.language.Globals;
import suneido.util.*;
import suneido.util.ServerBySocket.HandlerFactory;

public class DbmsServerBySocket {
	@GuardedBy("serverDataSet")
	static final Set<ServerData> serverDataSet = new HashSet<ServerData>();
	private static InetAddress inetAddress;
	private static final ScheduledExecutorService scheduler
			= Executors.newSingleThreadScheduledExecutor();

	public static void run(int port) {
		TheDb.open();
		Globals.builtin("Print", new Repl.Print());
		Compiler.eval("JInit()");
		ServerBySocket server = new ServerBySocket(new DbmsHandlerFactory());
		inetAddress = server.getInetAddress();
		scheduler.scheduleAtFixedRate(new Runnable() {
				public void run() {
					TheDb.db().limitOutstandingTransactions();
				}
			}, 1, 1, TimeUnit.SECONDS);
		scheduler.scheduleAtFixedRate(new Runnable() {
				public void run() {
					TheDb.db().dest.force();
				}
			}, 1, 1, TimeUnit.MINUTES);
		try {
			server.run(port);
		} catch (IOException e) {
			e.printStackTrace();
		}
		scheduler.shutdown();
	}

	public static InetAddress getInetAddress() {
		return inetAddress;
	}

	private static class DbmsHandlerFactory implements HandlerFactory {
		@Override
		public Runnable newHandler(Socket socket, String address) throws IOException {
			return new Handler(socket, address);
		}
	}

	/**
	 * There is an instance of Handler for each open connection. Once a complete
	 * request has been received, it is executed in a separate thread. Since
	 * requests do not overlap, moreInput will not be called while a request is
	 * executing.
	 */
	@NotThreadSafe
	private static class Handler implements Runnable {
		private static final ByteBuffer hello = stringToBuffer("jSuneido Server\r\n");
		private final Socket socket;
		private final InputBySocket input;
		private final NetworkOutput outputQueue;
		private final ServerData serverData;
		private Command cmd;
		private ByteBuffer line;
		private ByteBuffer extra;

		Handler(Socket socket, String address) throws IOException {
			this.socket = socket;
			input = new InputBySocket(socket);
			outputQueue = new OutputBySocket(socket);
			ServerData.threadLocal.set(new ServerData(outputQueue));
			serverData = new ServerData(outputQueue);
			serverData.setSessionId(address);
			synchronized(serverDataSet) {
				serverDataSet.add(serverData);
			}
		}

		@Override
		public void run() {
			outputQueue.add(hello.duplicate());
			outputQueue.write();
			while (getRequest())
				executeRequest();
			close();
		}

		private boolean getRequest() {
			line = input.readLine();
			if (line == null)
				return false;
//System.out.println("> " + bufferToString(line));
			cmd = getCmd(line);
			line = line.slice();
			int nExtra = cmd.extra(line);
			line.position(0);
			extra = input.readExtra(nExtra);
			return true;
		}

		private void executeRequest() {
			ByteBuffer output = null;
			try {
				output = cmd.execute(line, extra, outputQueue);
			} catch (Throwable e) {
if (!(e instanceof SuException))
e.printStackTrace();
				output = stringToBuffer("ERR " + escape(e.toString()) + "\r\n");
			}
			cmd = null;
			line = null;
			extra = null;
			if (output != null)
				outputQueue.add(output);
			outputQueue.write();
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

		private static String escape(String s) {
			return s.replace("\r", "\\r").replace("\n", "\\n");
		}

		public void close() {
			synchronized(serverDataSet) {
				serverData.end();
				serverDataSet.remove(serverData);
			}
			try {
				socket.close();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}

}
