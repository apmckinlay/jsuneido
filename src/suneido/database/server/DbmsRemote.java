package suneido.database.server;

import java.io.*;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.util.*;

import suneido.SuContainer;
import suneido.SuException;
import suneido.database.Mmfile;
import suneido.database.Record;
import suneido.database.query.Query.Dir;
import suneido.language.Ops;
import suneido.language.Pack;
import suneido.util.Tr;

import com.google.common.base.Function;
import com.google.common.base.Splitter;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;

public class DbmsRemote implements Dbms {
	private Socket socket;
	private DataInputStream in;
	private DataOutputStream out;

	public DbmsRemote(String ip, int port) {
		try {
			socket = new Socket(ip, 3147);
			socket.setSoTimeout(5000);
			in = new DataInputStream(socket.getInputStream());
			out = new DataOutputStream(socket.getOutputStream());
		} catch (Exception e) {
			throw new SuException("can't connect to " + ip + ":" + port, e);
		}
		String msg = readLine();
		if (! msg.startsWith("Suneido Database Server"))
			throw new SuException("invalid connect response: " + msg);
System.out.println("got: " + msg);
		write("BINARY\r\n");
		ok();
	}

	public void close() {
		try {
			socket.close();
		} catch (IOException e) {
			throw new SuException("error", e);
		}
	}

	@Override
	public DbmsTran transaction(boolean readwrite) {
		// TODO transaction
		return null;
	}

	@Override
	public void admin(ServerData serverData, String s) {
		// TODO admin

	}

	@Override
	public int request(ServerData serverData, DbmsTran tran, String s) {
		// TODO request
		return 0;
	}

	@Override
	public DbmsQuery cursor(ServerData serverData, String s) {
		// TODO cursor
		return null;
	}

	@Override
	public DbmsQuery query(ServerData serverData, DbmsTran tran, String s) {
		// TODO query
		return null;
	}

	@Override
	public List<LibGet> libget(String name) {
		// TODO libget
		return new ArrayList<LibGet>();
	}

	@Override
	public List<String> libraries() {
		return ImmutableList.copyOf(readList("LIBRARIES"));
	}

	private Iterable<String> readList(String cmd) {
		write(cmd + "\r\n");
		String s = readLine();
		if (! s.startsWith("(") || ! s.endsWith(")"))
			throw new SuException(cmd + " expected (...)\r\ngot: " + s);
		s = s.substring(1, s.length() - 1);
		return Splitter.on(',').omitEmptyStrings().trimResults().split(s);
	}

	@Override
	public List<Integer> tranlist() {
		Iterable<String> iter = readList("TRANLIST");
		return ImmutableList.copyOf(
				Iterables.transform(iter, new Function<String,Integer>() {
					@Override
					public Integer apply(String s) {
						return Integer.parseInt(s);
					}
				}));
	}

	@Override
	public Date timestamp() {
		write("TIMESTAMP\r\n");
		String s = readLine();
		Date date = Ops.stringToDate(s);
		if (date == null)
			throw new SuException("bad timestamp from server: " + s);
		return date;
	}

	@Override
	public void dump(String filename) {
		write("DUMP " + filename + "\r\n");
		ok();
	}

	@Override
	public void copy(String filename) {
		write("COPY " + filename + "\r\n");
		ok();
	}

	@Override
	public Object run(String s) {
		write("RUN " + Tr.tr(s, "\r\n", " ") + "\r\n");
		return readValue();
	}

	@Override
	public long size() {
		write("SIZE\r\n");
		int n = readInt('S');
		return Mmfile.intToOffset(n);
	}

	@Override
	public SuContainer connections() {
		write("CONNECTIONS\r\n");
		return (SuContainer) readValue();
	}

	@Override
	public void erase(DbmsTran tran, long recadr) {
		// TODO erase

	}

	@Override
	public long update(DbmsTran tran, long recadr, Record rec) {
		// TODO update
		return 0;
	}

	@Override
	public HeaderAndRow get(ServerData serverData, Dir dir, String query,
			boolean one, DbmsTran tran) {
		// TODO get
		return null;
	}

	@Override
	public int cursors() {
		write("CURSORS\r\n");
		return readInt('N');
	}

	@Override
	public String sessionid(String s) {
		write("SESSIONID " + s + "\r\n");
		return readLine();
	}

	@Override
	public int finalSize() {
		write("FINAL\r\n");
		return readInt('N');
	}

	@Override
	public void log(String s) {
		write("LOG " + Tr.tr(s, " \r\n", " ").trim() + "\r\n");
		ok();
	}

	@Override
	public int kill(String s) {
		write("KILL " + Tr.tr(s, " \r\n", " ").trim() + "\r\n");
		return readInt('N');
	}

	@SuppressWarnings("deprecation")
	private String readLine() {
		try {
			String s = in.readLine();
			if (s == null)
				throw new SuException("lost connection");
			if (s.startsWith("ERR"))
				throw new SuException(s.substring(4) + " (from server)");
			return s;
		} catch (IOException e) {
			throw new SuException("error", e);
		}
	}

	private void write(String s) {
		try {
			out.writeBytes(s);
		} catch (IOException e) {
			throw new SuException("error", e);
		}
	}

	private void ok() {
		String s = readLine();
		if (! s.equals("OK"))
			throw new SuException("expected 'OK', got: " + s);
	}

	private int readInt(char c) {
		String s = readLine();
		return ck_getnum(c, s);
	}

	private static final int ERR = -1;

	private static int getnum(char type, String s) {
		Scanner scan = new Scanner(s);
		try {
			scan.skip("\\s*" + type);
			return scan.nextInt();
		} catch (NoSuchElementException e) {
			return ERR;
		}
	}

	private static int ck_getnum(char type, String s) {
		int num = getnum(type, s);
		if (num == ERR)
			throw new SuException("expecting: " + type + "#, got: " + s);
		return num;
	}

	private Object readValue() {
		String s = readLine();
		if (s.equals(""))
			return null;
		int n = ck_getnum('P', s);
		byte[] buf = new byte[n];
		try {
			in.read(buf);
		} catch (IOException e) {
			throw new SuException("error", e);
		}
		return Pack.unpack(ByteBuffer.wrap(buf));
	}

}
