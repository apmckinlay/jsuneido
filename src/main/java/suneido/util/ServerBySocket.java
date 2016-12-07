/* Copyright 2008 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido.util;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.Executor;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.annotation.concurrent.NotThreadSafe;

/**
 * Socket server framework using plain sockets (not NIO). Uses a supplied
 * HandlerFactory to create a new Runnable handler for each accepted connection.
 */
@NotThreadSafe
public class ServerBySocket {
	private final Executor executor;
	private final HandlerFactory handlerFactory;
	private final AtomicBoolean shouldLog = new AtomicBoolean(true);
	private static final int THROTTLE_SLEEP_MS = 100;

	public ServerBySocket(Executor executor, HandlerFactory handlerFactory) {
		this.executor = executor;
		this.handlerFactory = handlerFactory;
	}

	public void run(int port) throws IOException {
		try (ServerSocket serverSocket = new ServerSocket(port)) {
			while (true) {
				Socket clientSocket = serverSocket.accept();
				// disable Nagle since we don't have gathering write
				clientSocket.setTcpNoDelay(true);
				try {
					Runnable handler = handlerFactory.newHandler(clientSocket);
					executor.execute(handler);
					shouldLog.set(true);
				} catch (RejectedExecutionException e) {
					clientSocket.close();
					if (shouldLog.getAndSet(false))
						Errlog.error("SocketServer too many connections");
					Util.interruptableSleep(THROTTLE_SLEEP_MS);
				}
			}
		}
	}

	public interface HandlerFactory {
		Runnable newHandler(Socket socket) throws IOException;
	}

	// ==========================================================================

	// static class EchoHandlerFactory implements HandlerFactory {
	// @Override
	// public Runnable newHandler(Socket socket) throws IOException {
	// return new EchoHandler(socket);
	// }
	// }
	//
	// static class EchoHandler implements Runnable {
	// private final Socket socket;
	// private final InputStream in;
	// private final OutputStream out;
	// private final byte[] buf = new byte[128];
	//
	// EchoHandler(Socket socket) throws IOException {
	// this.socket = socket;
	// in = socket.getInputStream();
	// out = socket.getOutputStream();
	// }
	//
	// @Override
	// public void run() {
	// try {
	// out.write("EchoServer\r\n".getBytes());
	// int n;
	// while (-1 != (n = in.read(buf))) {
	// out.write(buf, 0, n);
	// }
	// socket.close();
	// } catch (IOException e) {
	// e.printStackTrace();
	// }
	// }
	// }
	//
	// public static void main(String[] args) {
	// ServerBySocket server = new ServerBySocket(new EchoHandlerFactory());
	// try {
	// server.run(1234);
	// } catch (IOException e) {
	// e.printStackTrace();
	// }
	// }

}
