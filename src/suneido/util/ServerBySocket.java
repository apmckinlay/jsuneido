package suneido.util;

import java.io.*;
import java.net.*;
import java.util.concurrent.*;

import javax.annotation.concurrent.NotThreadSafe;

import com.google.common.util.concurrent.ThreadFactoryBuilder;

/**
 * Socket server framework using plain sockets (not NIO).
 * Uses a supplied HandlerFactory to create a new Runnable handler
 * for each accepted connection.
 */
@NotThreadSafe
public class ServerBySocket {
	private static final int CORE_THREADS = 4;
	private static final int MAX_THREADS = 4;
	private static final int QUEUE_SIZE = 4;
	private static final ThreadFactory threadFactory =
			new ThreadFactoryBuilder().setDaemon(true).build();
	private static final ThreadPoolExecutor executor =
			new ThreadPoolExecutor(CORE_THREADS, MAX_THREADS,
					60, TimeUnit.SECONDS,
					new ArrayBlockingQueue<Runnable>(QUEUE_SIZE),
					threadFactory, new ThreadPoolExecutor.CallerRunsPolicy());
	private final HandlerFactory handlerFactory;
	private InetAddress inetAddress;

	public ServerBySocket(HandlerFactory handlerFactory) {
		this.handlerFactory = handlerFactory;
	}

	public void run(int port) throws IOException {
		ServerSocket serverSocket = new ServerSocket(port);
		serverSocket.setReuseAddress(true);
		inetAddress = serverSocket.getInetAddress();
		while (true) {
			Socket clientSocket = serverSocket.accept();
			// disable Nagle since we don't have gathering write
			clientSocket.setTcpNoDelay(true);
			Runnable handler = handlerFactory.newHandler(clientSocket, inetAddress.toString());
			executor.execute(handler);
		}
	}

	public InetAddress getInetAddress() {
		return inetAddress;
	}

	public static interface HandlerFactory {
		public Runnable newHandler(Socket socket, String address) throws IOException;
	}

	//==========================================================================

	static class EchoHandlerFactory implements HandlerFactory {
		@Override
		public Runnable newHandler(Socket socket, String address) throws IOException {
			return new EchoHandler(socket);
		}
	}

	static class EchoHandler implements Runnable {
		private final Socket socket;
		private final InputStream in;
		private final OutputStream out;
		private final byte[] buf = new byte[128];

		EchoHandler(Socket socket) throws IOException {
			this.socket = socket;
			in = socket.getInputStream();
			out = socket.getOutputStream();
		}

		@Override
		public void run() {
			try {
				out.write("EchoServer\r\n".getBytes());
				int n;
				while (-1 != (n = in.read(buf))) {
					out.write(buf, 0, n);
				}
				socket.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	public static void main(String[] args) {
		ServerBySocket server = new ServerBySocket(new EchoHandlerFactory());
		try {
			server.run(1234);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

}
