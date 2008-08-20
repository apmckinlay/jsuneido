package suneido.database.server;

import static suneido.database.Mode.CREATE;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

import org.ronsoft.nioserver.BufferFactory;
import org.ronsoft.nioserver.ChannelFacade;
import org.ronsoft.nioserver.InputHandler;
import org.ronsoft.nioserver.InputQueue;
import org.ronsoft.nioserver.impl.DumbBufferFactory;
import org.ronsoft.nioserver.impl.GenericInputHandlerFactory;
import org.ronsoft.nioserver.impl.NioDispatcher;
import org.ronsoft.nioserver.impl.StandardAcceptor;

import suneido.database.Database;
import suneido.database.DestMem;

/**
 * Using org.ronsoft.nioserver -
 * see <a href="http://javanio.info/filearea/nioserver">
 *		How to Build a Scalable Multiplexed Server with NIO Mark II</a>
 * @author Andrew McKinlay
 * <p><small>Copyright 2008 Suneido Software Corp. All rights reserved. Licensed under GPLv2.</small></p>
 */
public class Server {
	public static void start(int port) throws IOException {
		Executor executor = Executors.newCachedThreadPool();
		BufferFactory bufFactory = new DumbBufferFactory (1024);
		NioDispatcher dispatcher = new NioDispatcher (executor, bufFactory);
		StandardAcceptor acceptor = new StandardAcceptor (port, dispatcher,
				new GenericInputHandlerFactory(Handler.class));

		dispatcher.start();
		acceptor.newThread();
	}

	/*
	 * If I understand correctly, a given handler instance will only be running
	 * in single thread at any one time, since there is a handler instance for
	 * each channel (connection). So it can use instance variables and not
	 * synchronize them.
	 */
	public static class Handler implements InputHandler {
		ByteBuffer line = null;
		Command cmd = null;
		ByteBuffer extra = null;
		int nExtra = -1;
		ServerData serverData = new ServerData();

		public ByteBuffer nextMessage(ChannelFacade channelFacade) {
			InputQueue inputQueue = channelFacade.inputQueue();
			if (line == null) { // starting state = waiting for newline
				int nlPos = inputQueue.indexOf((byte) '\n');
				if (nlPos == -1)
					return null;
				line = inputQueue.dequeueBytes(nlPos + 1);
				cmd = getCmd(line);
				nExtra = cmd.extra(line);
			}
			// next state = waiting for extra data (if any)
			if (nExtra != -1 && inputQueue.available() >= nExtra) {
				extra = inputQueue.dequeueBytes(nExtra);
				return line;
			}
			return null;
		}
		private static Command getCmd(ByteBuffer buf) {
			try {
				return Command.valueOf(firstWord(buf).toUpperCase());
			} catch (IllegalArgumentException e) {
				return Command.BADCMD;
			}
		}
		private static String firstWord(ByteBuffer buf) {
			String s = "";
			buf.position(0);
			while (buf.remaining() > 0) {
				char c = (char) buf.get();
				if (c == ' ' || c == '\r' || c == '\n')
					break ;
				s += c;
			}
			return s;
		}
		public void handleInput(ByteBuffer message, ChannelFacade channelFacade) {
			ByteBuffer output;
			try {
				output = cmd.execute(line, extra, 
						channelFacade.outputQueue(), serverData);
			} catch (Throwable e) {
				e.printStackTrace();
				output = ByteBuffer.wrap(
						("ERR " + e.toString() + "\r\n").getBytes());
			}
			if (output != null) {
				output.position(0);
				channelFacade.outputQueue().enqueue(output);
			}
			line = extra = null;
			cmd = null;
			nExtra = -1;
		}

		private static final ByteBuffer hello =
			ByteBuffer.wrap("jSuneido Server\r\n".getBytes());
		public void started(ChannelFacade channelFacade) {
			hello.position(0);
			channelFacade.outputQueue().enqueue(hello);
		}

		public void starting(ChannelFacade channelFacade) {
			// not needed
		}

		public void stopped(ChannelFacade channelFacade) {
			// not needed
		}

		public void stopping(ChannelFacade channelFacade) {
			// not needed
		}
	}

	public static void main(String[] args) throws IOException {
		Database.theDB = new Database(new DestMem(), CREATE);
		start(3456);
	}
}
