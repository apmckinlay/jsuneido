package suneido;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.util.Collections;
import java.util.List;

import suneido.database.server.DbmsServer;

import com.sun.net.httpserver.*;

public class HttpServerMonitor {

	public static void run(int port) {
		HttpServer server;
		try {
			server = HttpServer.create(new InetSocketAddress(port),	0);
		} catch (IOException e) {
			e.printStackTrace();
			return;
		}
		server.createContext("/", new MyHandler());
		server.setExecutor(null); // null creates a default executor
		server.start();
	}

	private static class MyHandler implements HttpHandler {
		public void handle(HttpExchange t) throws IOException {
			List<String> conns = DbmsServer.connections();
			Collections.sort(conns);
			StringBuilder sb = new StringBuilder();
		       	sb.append("<html>\r\n"
		       			+ "<head>\r\n"
		       			+ "<title>Suneido Server Monitor</title>\r\n"
		       			+ "<meta http-equiv=\"refresh\" content=\"15\" />\r\n"
		       			+ "</head>\r\n"
		       			+ "<body>\r\n"
		       			+ "<h1>Suneido Server Monitor</h1>\r\n"
		       			+ "<p>Built: ")
	       		.append(WhenBuilt.when())
	       		.append("</p>\r\n"
	       			+ "<p>Heap Size: ")
	   			.append(mb(Runtime.getRuntime().totalMemory()))
	       		.append("mb</p>\r\n"
	       			+ "<p>Transactions: ")
	   			.append(TheDbms.dbms().tranlist().size())
	       		.append("</p>\r\n"
	       			+ "<p>Cursors: ")
	   			.append(TheDbms.dbms().cursors())
	       		.append("</p>\r\n"
	       			+ "<p>Database Size: ")
	 	  		.append(mb(TheDbms.dbms().size()))
	       		.append("mb</p>\r\n"
	       			+ "<p>Connections: (")
		   		.append(conns.size())
	       		.append(") ");
       		String sep = "";
       		for (String s : conns) {
       			sb.append(sep).append(s);
       			sep = " + ";
       		}
	       	sb.append("</p>\r\n"
	       			+ "</body>\r\n"
	       			+ "</html>\r\n");
			String response = sb.toString();
			t.sendResponseHeaders(200, response.length());
			OutputStream os = t.getResponseBody();
			os.write(response.getBytes());
			os.close();
		}

		private static long mb(long n) {
			return ((n + 512 * 1024) / (1024 * 1024));
		}
	}

}
