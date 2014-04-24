/* Copyright 2010 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido.database.server;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.nio.ByteBuffer;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class TestServer {

	public static void main(String[] args) {
		try {
			run();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private static void run() throws IOException {
		ServerSocketChannel serverChannel = ServerSocketChannel.open();
		ServerSocket serverSocket = serverChannel.socket();
		serverSocket.setReuseAddress(true);
		serverSocket.bind(new InetSocketAddress(3147));
		while (true) {
			SocketChannel channel = serverChannel.accept();
			Runnable handler = new Handler(channel);
			Thread worker = new Thread(handler);
			worker.start();
		}
	}

	private static class Handler implements Runnable {
		private final SocketChannel channel;
		private final Input input;
		private final Output outputQueue;
		private ByteBuffer line;
		private Command cmd;
		private ByteBuffer extra;

		public Handler(SocketChannel channel) {
			this.channel = channel;
			input = new Input(channel);
			outputQueue = new Output(channel);
		}

		@Override
		public void run() {
			while (getRequest())
				executeRequest();
			close();
		}

		private boolean getRequest() {
			line = input.readLine();
			if (line == null)
				return false;
			cmd = getCmd(line);
			line = line.slice();
			int nExtra = cmd.extra(line);
			line.position(0);
			extra = input.readExtra(nExtra);
			return true;
		}

		private static Command getCmd(ByteBuffer buf) {
			try {
				String word = firstWord(buf);
				return Command.valueOf(word.toUpperCase());
			} catch (IllegalArgumentException e) {
				return null;
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

		private void executeRequest() {
			try {
				cmd.execute(line, extra, outputQueue);
			} catch (Throwable e) {
				e.printStackTrace();
			}
			line = null;
			cmd = null;
			extra = null;
			outputQueue.write();
		}

		public void close() {
			try {
				channel.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}

	} // end of Handler

	public static class Input {
		private final SocketChannel channel;
		private final ByteBuffer buf = ByteBuffer.allocate(16 * 1024);
		private int nlPos;

		public Input(SocketChannel channel) {
			this.channel = channel;
		}

		public ByteBuffer readLine() {
			do {
				if (read() == -1)
					return null;
				nlPos = indexOf(buf, (byte) '\n');
			} while (nlPos == -1);
			ByteBuffer line = buf.duplicate();
			line.position(0);
			line.limit(++nlPos);
			return line;
		}

		private int read() {
			try {
				return channel.read(buf);
			} catch (IOException e) {
				return -1;
			}
		}

		private static int indexOf(ByteBuffer buf, byte b) {
			for (int i = 0; i < buf.position(); ++i)
				if (buf.get(i) == b)
					return i;
			return -1;
		}

		public ByteBuffer readExtra(int n) {
			while (buf.position() < nlPos + n)
				if (read() == -1)
					return null;
			buf.flip();
			buf.position(nlPos);
			ByteBuffer result = buf.slice();
			buf.clear();
			return result;
		}

	} // end of Input

	public static class Output {
		private final SocketChannel channel;
		private final List<ByteBuffer> queue = new ArrayList<>();
		private ByteBuffer[] bufs = new ByteBuffer[0];
		private int n;

		public Output(SocketChannel channel) {
			this.channel = channel;
		}

		public void add(ByteBuffer buf) {
			queue.add(buf);
		}

		public void write() {
			bufs = queue.toArray(bufs);
			n = queue.size();
			queue.clear();
			try {
				while (!isEmpty())
					channel.write(bufs, 0, n);
			} catch (IOException e) {
				e.printStackTrace();
			}
			Arrays.fill(bufs, null);
		}

		private boolean isEmpty() {
			for (int i = 0; i < n; ++i)
				if (bufs[i].remaining() > 0)
					return false;
			return true;
		}

	} // end of Output

	static enum Command {

		GET1 {
			private final ByteBuffer response
				= ByteBuffer.wrap(new byte[] { 'E', 'O', 'F', '\r', '\n' });

			@Override
			public int extra(ByteBuffer buf) {
				buf.get();
				buf.get();
				getnum('T', buf);
				return getnum('Q', buf);
			}

			@Override
			public ByteBuffer execute(ByteBuffer line, ByteBuffer extra,
					Output outputQueue) {
				outputQueue.add( response.duplicate());
				return null;
			}
		};

		public int extra(ByteBuffer buf) {
			return 0;
		}

		public ByteBuffer execute(ByteBuffer line, ByteBuffer extra,
				Output outputQueue) {
			return null;
		}

		static int getnum(char type, ByteBuffer buf) {
			int i = buf.position();
			while (i < buf.limit() && Character.isWhitespace(buf.get(i)))
				++i;
			if (i >= buf.limit()
					|| Character.toUpperCase(buf.get(i)) != type
					|| !Character.isDigit(buf.get(i + 1)))
				return -1;
			++i;
			StringBuilder sb = new StringBuilder();
			while (i < buf.limit() && Character.isDigit(buf.get(i)))
				sb.append((char) buf.get(i++));
			int n = Integer.valueOf(sb.toString());
			while (i < buf.limit() && Character.isWhitespace(buf.get(i)))
				++i;
			buf.position(i);
			return n;
		}

	}

}
