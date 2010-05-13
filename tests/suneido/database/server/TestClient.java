package suneido.database.server;

import java.io.*;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.Random;

import com.google.common.base.Strings;

public class TestClient {
	private static final int NTHREADS = 20;
	private static final long DURATION = 60 * 1000;
	private static final int MAXREQUEST = 1001;
	private final static int MAXDATA = 10001;

	public static void main(String... args)
			throws UnknownHostException, IOException {
		String address = args.length == 0 ? "localhost" : args[0];
		for (int i = 0; i < NTHREADS; ++i)
			new Thread(new Run(address)).start();
	}

	@SuppressWarnings("deprecation")
	private static class Run implements Runnable {
		private final Random rand = new Random();
		private final String requestData = Strings.repeat(" ", MAXREQUEST);
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
			Socket socket = new Socket(address, 1234);
			socket.setSoTimeout(2000);
			DataInputStream inputstream = new DataInputStream(socket.getInputStream());
			DataOutputStream outputstream = new DataOutputStream(socket.getOutputStream());
			long t = System.currentTimeMillis();
			for (int i = 0; ; ++i) {
				long elapsed = System.currentTimeMillis() - t;
				if (elapsed > DURATION)
					break;
				int n = rand.nextInt(MAXREQUEST);
				String request = n + "\n" + requestData.substring(0, n);
//System.out.println(">" + n);
				outputstream.write(request.getBytes());
				byte[] buf = new byte[MAXDATA];
				String response = inputstream.readLine();
				n = Integer.parseInt(response);
//System.out.println(response + " (" + (n + response.length() + 1) + ")");
				inputstream.readFully(buf, 0, n);
			}
System.out.println("done");
		}
	}

}
