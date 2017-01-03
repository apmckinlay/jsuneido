/* Copyright 2010 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import com.google.common.base.Joiner;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

import suneido.runtime.builtin.SuThread;
import suneido.util.Errlog;

public class HttpServerMonitor {
	private final static int STARTING = 0;
	private final static int RUNNING = 1;
	private final static int CORRUPT = 2;
	private final static int CHECKING = 3;
	private final static int REBUILDING = 4;
	private static AtomicInteger mode = new AtomicInteger(STARTING);

	public static void run(int port) {
		HttpServer server;
		try {
			server = HttpServer.create(new InetSocketAddress(port),	0);
		} catch (IOException e) {
			Errlog.error("HttpServerMonitor.run", e);
			return;
		}
		server.createContext("/", new MyHandler());
		server.setExecutor(null); // null creates a default executor
		server.start();
	}

	public static void starting() {
		mode.set(STARTING);
	}

	public static void running() {
		mode.set(RUNNING);
	}

	public static void checking() {
		mode.set(CHECKING);
	}

	public static void rebuilding() {
		mode.set(REBUILDING);
	}

	public static void corrupt() {
		mode.set(CORRUPT);
	}

	private static class MyHandler implements HttpHandler {
		@Override
		public void handle(HttpExchange t) throws IOException {
			String response =
					"<html>\r\n"
					+ "<head>\r\n"
					+ "<title>Suneido Server Monitor</title>\r\n"
					+ "<meta http-equiv=\"refresh\" content=\"5\" />\r\n"
					+ "</head>\r\n"
					+ "<body>\r\n"
					+ "<h1>Suneido Server Monitor</h1>\r\n"
					+ getBody()
					+ "</body>\r\n"
					+ "</html>\r\n";
			t.sendResponseHeaders(200, response.length());
			try (OutputStream os = t.getResponseBody()) {
				os.write(response.getBytes());
			}
		}

		// NOTE: cSuneido checks for the exact check/rebuild strings
		// if you changes these then cSuneido must be changed to match
		private static String getBody() {
			switch (HttpServerMonitor.mode.get()) {
			case CHECKING:
				return "<h2 style=\"color: red;\">Checking database ...<h2>\r\n";
			case REBUILDING:
				return "<h2 style=\"color: red;\">Rebuilding database ...</h2>\r\n";
			case RUNNING:
			case CORRUPT:
			default:
				return status();
			}
		}

		private static String status() {
			StringBuilder sb = new StringBuilder();
			if (mode.get() == STARTING)
				sb.append("<h2 style=\"color: blue;\">STARTING</h2>\r\n");
			else if (mode.get() == CORRUPT)
				sb.append("<h2 style=\"color: red;\">DATABASE DAMAGE DETECTED - "
						+ "OPERATING IN READ-ONLY MODE</h2>\r\n");
			sb.append("<p>Built: ")
					.append(WhenBuilt.when())
					.append("</p>\r\n");
			sb.append("<p>Heap Size: ")
					.append(mb(Runtime.getRuntime().totalMemory()))
					.append("mb</p>\r\n");
			sb.append("<p>Transactions: ")
					.append(TheDbms.dbms().transactions().size())
					.append("</p>\r\n");
			sb.append("<p>Cursors: ")
					.append(TheDbms.dbms().cursors())
					.append("</p>\r\n");
			sb.append("<p>Database Size: ")
					.append(mb(TheDbms.dbms().size()))
					.append("mb</p>\r\n");

			List<String> conns = Suneido.server.connections();
			sb.append("<p>Connections: (").append(conns.size()).append(") ");
			Collections.sort(conns);
			Joiner.on(", ").appendTo(sb, conns);
			sb.append("</p>\r\n");

			sb.append("<p>Threads: (")
					.append(Suneido.threadGroup.activeCount())
					.append(") ")
					.append(Arrays.asList(SuThread.list()).stream()
						.map(Thread::getName)
						.filter(s -> ! s.contains("-connection-") &&
								! s.contains("-thread-pool"))
						.sorted()
						.collect(Collectors.joining(", ")))
					.append("</p>\r\n");

			return sb.toString();
		}

		private static long mb(long n) {
			return ((n + 512 * 1024) / (1024 * 1024));
		}
	}

}
