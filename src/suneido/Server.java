package suneido;

import java.net.*;
import java.io.*;

public class Server {
	static final int PORT = 3456;
	static final int NTHREADS = 1;
	
	public static void main(String[] args) {
		start();
	}
	
	public static void start() {
		final ServerSocket serverSocket = open();
		for (int i = 1; i < NTHREADS; ++i) {
			Thread thread = new Thread() {
				public void run() {
					dispatch(serverSocket);
				}
			};
		thread.start();
		}
		dispatch(serverSocket);
	}

	private static ServerSocket open() {
		try {
			return new ServerSocket(PORT);
		} catch (IOException e) {
			System.out.println(e.getMessage());
			throw new SuException("can't open port " + PORT);
		}
	}
	
	private static void dispatch(final ServerSocket serverSocket) {
		Socket socket;
		while (true) {
			try {
				socket = serverSocket.accept();
			} catch (IOException e) {
				System.out.println(e.getMessage());
				continue ;
			}
			try {
				InputStream in = socket.getInputStream();
				OutputStream out = socket.getOutputStream();
				out.write("hello world\n".getBytes(), 0, 12);
				byte[] buf = new byte[100];
				int n;
				while (-1 != (n = in.read(buf)))
					out.write(buf, 0, n);
			} catch (IOException e) {
				System.out.println(e.getMessage());
			} finally {
				try {
					socket.close();
				} catch (IOException e) {
					// ignore
				}
			}
		}
	}
}
