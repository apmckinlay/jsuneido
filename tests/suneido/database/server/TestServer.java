package suneido.database.server;

import static suneido.util.Util.stringToBuffer;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

import suneido.util.SocketServer;
import suneido.util.SocketServer.OutputQueue;

public class TestServer {
	private static final Executor executor = Executors.newCachedThreadPool();

	private static class HandlerFactory implements SocketServer.HandlerFactory {
		@Override
		public SocketServer.Handler newHandler(OutputQueue outputQueue,
				String address) {
			return new Handler(outputQueue, address);
		}
	}

	private static class Handler implements SocketServer.Handler, Runnable {
		private static final ByteBuffer hello = stringToBuffer("jSuneido Server\r\n");
		private final OutputQueue outputQueue;
//		private final Random rand = new Random();
//		private final static int MAXDATA = 10001;
//		private final ByteBuffer data = ByteBuffer.allocate(MAXDATA);
		private int required = -1;
public long t;

		public Handler(OutputQueue outputQueue, String address) {
			this.outputQueue = outputQueue;
		}

		@Override
		public void start() {
			outputQueue.add(hello.duplicate());
			outputQueue.write();
		}

		@Override
		public void moreInput(ByteBuffer buf) {
			if (required == -1) {
				if (buf.remaining() < 15)
					return;
				ByteBuffer line = getLine(buf);
				if (line == null)
					return;
//System.out.println("line " + bufferToString(line));
				required = line.remaining() + 1 + 33;
			}
			if (buf.remaining() >= required) {
//System.out.println("got required");
				required = -1;
				buf.position(buf.limit()); // consume all input
				executor.execute(this);
			}
		}
		private static ByteBuffer getLine(ByteBuffer buf) {
			int nlPos = indexOf(buf, (byte) '\n');
			if (nlPos == -1)
				return null;
			ByteBuffer line = buf.duplicate();
			line.position(0);
			line.limit(nlPos);
			return line;
		}
		private static int indexOf(ByteBuffer buf, byte b) {
			for (int i = 0; i < buf.remaining(); ++i)
				if (buf.get(i) == (byte) '\n')
					return i;
			return -1;
		}

		@Override
		public void run() {
//			int n = rand.nextInt(MAXDATA);
//			outputQueue.add(stringToBuffer(n + "\n"));
//			ByteBuffer buf = this.data.duplicate();
//			buf.position(0);
//			buf.limit(n);
//			outputQueue.add(buf);
			outputQueue.add(Command.eof());
			outputQueue.write();
//System.out.println("\t\t\t\tend of run");
		}

		@Override
		public void close() {
		}

	}

	public static void main(String... args) {
		SocketServer server = new SocketServer(new HandlerFactory());
		try {
			server.run(3147);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

}
