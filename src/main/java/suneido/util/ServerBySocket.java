/* Copyright 2008 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido.util;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Date;
import java.util.Queue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.TimeUnit;

import suneido.util.NotThreadSafe;

import com.google.common.base.Joiner;
import com.google.common.collect.EvictingQueue;

/**
 * Socket server framework using plain sockets (not NIO). Uses a supplied
 * HandlerFactory to create a new Runnable handler for each accepted connection.
 */
@NotThreadSafe
public class ServerBySocket {
	private final ExecutorService executor;
	private final HandlerFactory handlerFactory;
	private final Queue<LogEntry> log = EvictingQueue.create(10);

	public ServerBySocket(ExecutorService executor, HandlerFactory handlerFactory) {
		this.executor = executor;
		this.handlerFactory = handlerFactory;
	}

	public void run(int port) throws IOException {
		try (ServerSocket serverSocket = new ServerSocket(port)) {
			while (true) {
				Socket clientSocket = serverSocket.accept();
				log.add(new LogEntry(clientSocket));
				// disable Nagle since we don't have gathering write
				clientSocket.setTcpNoDelay(true);
				try {
					Runnable handler = handlerFactory.newHandler(clientSocket);
					executor.execute(handler);
				} catch (RejectedExecutionException e) {
					clientSocket.close();
					break;
				}
			}
		}
		Errlog.error("SocketServer too many connections, stopping\r\n" +
				"\tlast 10 connections:\r\n\t" +
				Joiner.on("\r\n\t").join(log));
		executor.shutdownNow(); // NOTE: may not clean up all threads or sockets
		try {
			boolean result = executor.awaitTermination(10, TimeUnit.SECONDS);
			Errlog.info("SocketServer awaitTermination(10 sec) returned " + result);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}

	public interface HandlerFactory {
		Runnable newHandler(Socket socket) throws IOException;
	}

	private static class LogEntry {
		final Date timestamp = new Date();
		final Socket clientSocket;

		LogEntry(Socket clientSocket) {
			this.clientSocket = clientSocket;
		}

		@Override
		public String toString() {
			return timestamp + " - " + clientSocket.getInetAddress().getHostName();
		}
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
