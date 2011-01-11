package suneido.database.server;

import java.io.*;
import java.net.Socket;
import java.net.UnknownHostException;

public class TestSuneidoClient {
	private static final int NTHREADS = 80;
	private static final long DURATION = 5 * 60 * 1000;
//	private static final int MAX_B = 80000;
	private final static int MAXDATA = 1000;

	public static void main(String... args)
			throws UnknownHostException, IOException {
		String address = args.length == 0 ? "localhost" : args[0];
		for (int i = 0; i < NTHREADS; ++i)
			new Thread(new Run(address)).start();
	}

	@SuppressWarnings("deprecation")
	private static class Run implements Runnable {
//		private final Random rand = new Random();
		private final byte[] buf = new byte[MAXDATA];
		private final String address;

		public Run(String address) {
			this.address = address;
		}

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
			Socket socket = new Socket(address, 3147);
			socket.setSoTimeout(2000);
			DataInputStream inputstream = new DataInputStream(socket.getInputStream());
			DataOutputStream outputstream = new DataOutputStream(socket.getOutputStream());
			inputstream.readLine();
			long t = System.currentTimeMillis();
			int i;
			for (i = 0; ; ++i) {
				if (i % 100 == 0) {
					long elapsed = System.currentTimeMillis() - t;
					if (elapsed > DURATION)
						break;
				}
				int n = 9999999; //rand.nextInt(MAX_B);
				String query = "testConcurrency where b = " + n;
				String request = "GET1 +  T0 Q" + query.length() + "\n";
//System.out.println(">" + request + "\t" + query);
				outputstream.write(request.getBytes());
				outputstream.write(query.getBytes());
				int c = inputstream.read();
				if (c != 'E')
					System.out.println("expected E(OF) but got " + c);
				c = inputstream.read();
				if (c != 'O')
					System.out.println("expected (E)O(F) but got " + c);
				String response = inputstream.readLine().trim();
//System.out.println("<" + response);
				if (!response.equals("F") ) {
					String[] parts = response.split(" ");
					assert parts[0].startsWith("A");
					assert parts[1].startsWith("R");
					assert parts[2].startsWith("(");
					n = Integer.parseInt(parts[1].substring(1));
					inputstream.readFully(buf, 0, n);
				}
			}
System.out.println("done " + i);
		}
	}

}
