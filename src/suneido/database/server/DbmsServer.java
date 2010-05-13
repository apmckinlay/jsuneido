package suneido.database.server;

import static suneido.database.Database.theDB;
import static suneido.util.Util.stringToBuffer;

import java.io.IOException;
import java.net.InetAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.*;

import javax.annotation.concurrent.GuardedBy;
import javax.annotation.concurrent.NotThreadSafe;

import suneido.Repl;
import suneido.SuException;
import suneido.language.Globals;
import suneido.util.SocketOutput;
import suneido.util.SocketServer;
import suneido.util.SocketServer.HandlerFactory;

public class DbmsServer {
	@GuardedBy("serverDataSet")
	static final Set<ServerData> serverDataSet = new HashSet<ServerData>();
	private static InetAddress inetAddress;
	public static final ScheduledExecutorService scheduler
			= Executors.newSingleThreadScheduledExecutor();


	public static void run(int port) {
//		Database.open_theDB();
		Globals.builtin("Print", new Repl.Print());
//		Compiler.eval("JInit()");
		SocketServer server = new SocketServer(new DbmsHandlerFactory());
		inetAddress = server.getInetAddress();
		scheduler.scheduleAtFixedRate(new Runnable() {
				public void run() {
					theDB.limitOutstandingTransactions();
				}
			}, 1, 1, TimeUnit.SECONDS);
		scheduler.scheduleAtFixedRate(new Runnable() {
				public void run() {
					theDB.dest.force();
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
		public Runnable newHandler(SocketChannel channel, String address) {
			return new Handler(channel, address);
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
		private final SocketChannel channel;
		private final SocketInput input;
		private final SocketOutput outputQueue;
//		private final ServerData serverData;
		private Command cmd;
		private ByteBuffer line;
		private ByteBuffer extra;

		Handler(SocketChannel channel, String address) {
			this.channel = channel;
			input = new SocketInput(channel);
			outputQueue = new SocketOutput(channel);
//			ServerData.threadLocal.set(new ServerData(outputQueue));
//			serverData = new ServerData(outputQueue);
//			serverData.setSessionId(address);
//			synchronized(serverDataSet) {
//				serverDataSet.add(serverData);
			}

		@Override
		public void run() {
			outputQueue.write(hello.duplicate());
			while (getRequest())
				executeRequest();
			try {
				channel.close();
			} catch (IOException e) {
				e.printStackTrace(); // TODO
			}
		}

		private boolean getRequest() {
			ByteBuffer line = input.readLine();
			if (line == null)
				return false;
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

//		public void close() {
//			synchronized(serverDataSet) {
//				serverData.end();
//				serverDataSet.remove(serverData);
//			}
//		}
	}

}
