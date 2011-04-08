package suneido.util;

import java.io.*;
import java.net.*;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

import javax.annotation.concurrent.NotThreadSafe;

/**
 * Socket server framework using plain sockets (not NIO).
 * Uses a supplied HandlerFactory to create a new Runnable handler
 * for each accepted connection.
 * Uses bounded queue and CallerRunsPolicy to throttle requests.
 */
@NotThreadSafe
public class ServerBySocket {
	private final Executor executor;
	private final HandlerFactory handlerFactory;
	private InetAddress inetAddress;

	public ServerBySocket(HandlerFactory handlerFactory) {
		this(Executors.newCachedThreadPool(), handlerFactory);
	}

	public ServerBySocket(Executor executor, HandlerFactory handlerFactory) {
		this.executor = executor;
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
