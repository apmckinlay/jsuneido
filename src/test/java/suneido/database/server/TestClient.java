/* Copyright 2010 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido.database.server;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.net.UnknownHostException;

public class TestClient {
	private static final int NTHREADS = 40;
	private static final long DURATION = 10 * 60 * 1000; // 10 minutes

	public static void main(String... args)
			throws UnknownHostException, IOException {
		String address = args.length == 0 ? "localhost" : args[0];
		for (int i = 0; i < NTHREADS; ++i)
			new Thread(new Run(address)).start();
	}

	private static class Run implements Runnable {
		private final String address;

		public Run(String address) {
			this.address = address;
		}

		@Override
		public void run() {
			try {
				run2();
			} catch (UnknownHostException e) {
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		private void run2() throws UnknownHostException, IOException {
			try (Socket socket = new Socket(address, 3147)) {
				socket.setSoTimeout(5000);
				DataInputStream inputstream = new DataInputStream(socket.getInputStream());
				DataOutputStream outputstream = new DataOutputStream(socket.getOutputStream());
				long t = System.currentTimeMillis();
				for (int i = 0; ; ++i) {
					if (i % 100 == 0) {
						long elapsed = System.currentTimeMillis() - t;
						if (elapsed > DURATION) {
							System.out.println("done " + i);
							break;
						}
					}
					String query = "testConcurrency where b = 9999999";
					String request = "GET1 +  T0 Q" + query.length() + "\n";
					outputstream.write(request.getBytes());
					outputstream.write(query.getBytes());
					expect(inputstream, 'E', "E");
					expect(inputstream, 'O', "O");
					expect(inputstream, 'F', "F");
					expect(inputstream, '\r', "\\r");
					expect(inputstream, '\n', "\\n");
				}
			}
		}

		private static void expect(DataInputStream inputstream,
				char expected, String expected2) throws IOException {
			int c = inputstream.read();
			if (c != expected)
				System.out.println("expected " + expected2 + " but got " + c
						+ (' ' <= c && c <= '~' ? " '" + (char) c + "'" : ""));
		}
	}

}
