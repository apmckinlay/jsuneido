/* Copyright 2009 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido.runtime.builtin;

import static suneido.util.Util.array;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketTimeoutException;

import suneido.SuException;
import suneido.SuValue;
import suneido.runtime.*;
import suneido.util.Util;

/***
 * Also used by {@link SocketServer}
 */
public class SocketClient extends SuValue {
	private static final BuiltinMethods methods = new BuiltinMethods(SocketClient.class);
	private final Socket socket;
	private final InputStream input;
	private final OutputStream output;

	// args must already be massaged
	private SocketClient(Object... args) {
		String address = Ops.toStr(args[0]);
		int port = Ops.toInt(args[1]);
		int timeout = Ops.toInt(args[2]) * 1000;
		int timeoutConnect = Ops.toInt(Ops.mul(args[3], 1000));
		try {
			if (timeoutConnect == 0)
				socket = new Socket(address, port); // default timeout
			else {
				socket = new Socket();
				socket.connect(new InetSocketAddress(address, port), timeoutConnect);
			}
			socket.setSoTimeout(timeout);
			socket.setTcpNoDelay(true); // disable nagle
			input = new BufferedInputStream(socket.getInputStream());
			output = socket.getOutputStream();
		} catch (IOException e) {
			throw new SuException("socket open failed", e);
		}
	}

	SocketClient(Socket socket) throws IOException {
		this.socket = socket;
		socket.setTcpNoDelay(true); // disable nagle
		input = new BufferedInputStream(socket.getInputStream());
		output = socket.getOutputStream();
	}

	@Override
	public SuValue lookup(String method) {
		return methods.lookup(method);
	}

	public static SuValue getMethod(String method) {
		return methods.getMethod(method);
	}

	public static Object Close(Object self) {
		socketClient(self).close();
		return null;
	}

	void close() {
		try {
			socket.close();
		} catch (IOException e) {
			throw new SuException("socket Close failed", e);
		}
	}

	@Params("nbytes = INTMAX")
	public static Object Read(Object self, Object a) {
		int n = Ops.toInt(a);
		if (n == 0)
			return "";
		byte[] data = new byte[n];
		int nr = 0;
		try {
			do {
				int r = socketClient(self).input.read(data, nr, n - nr);
				if (r == -1)
					break;
				nr += r;
			} while (nr < n);
		} catch (SocketTimeoutException e) {
			if (nr == 0)
				throw new SuException("socket Read lost connection or timeout", e);
		} catch (IOException e) {
			if (nr == 0)
				throw new SuException("socket Read failed", e);
		}
		if (nr == 0)
			throw new SuException("socket Read lost connection or timeout");
		return Util.bytesToString(data, nr);
	}

	public static Object Readline(Object self) {
		try {
			return socketClient(self).readLine();
		} catch (IOException e) {
			throw new SuException("socket Readline failed", e);
		}
	}

	// NOTE: line ending should be consistent across file, socket, and runpiped
	private String readLine() throws IOException {
		StringBuilder sb = new StringBuilder();
		while (true) {
			int c = input.read();
			if (c == '\n' || c == -1)
				break ;
			if (sb.length() < Util.MAX_LINE)
				sb.append((char) c);
		}
		return Util.toLine(sb);
	}

	@Params("string")
	public static Object Write(Object self, Object a) {
		String data = Ops.toStr(a);
		try {
			socketClient(self).output.write(Util.stringToBytes(data));
		} catch (IOException e) {
			throw new SuException("socket Write failed", e);
		}
		return null;
	}

	private static final byte[] newline = "\r\n".getBytes();

	@Params("string")
	public static Object Writeline(Object self, Object a) {
		String data = Ops.toStr(a);
		try {
			socketClient(self).output.write(Util.stringToBytes(data));
			socketClient(self).output.write(newline);
		} catch (IOException e) {
			throw new SuException("socket Writeline failed", e);
		}
		return null;
	}

	// need because SocketServer shares these methods
	private static SocketClient socketClient(Object self) {
		return self instanceof SocketServer.Instance
				? ((SocketServer.Instance) self).socket
				: (SocketClient) self;
	}

	@Override
	protected void finalize() throws Throwable {
		close();
	}

	private static final FunctionSpec newFS = new FunctionSpec(array("address",
			"port", "timeout", "timeoutConnect"), 60, 0);

	private static final FunctionSpec callFS = new FunctionSpec(array(
			"address", "port", "timeout", "timeoutConnect", "block"), 60, 0,
			false);

	public static final BuiltinClass clazz = new BuiltinClass("SocketClient",
			newFS) {

		@Override
		public SocketClient newInstance(Object... args) {
			args = Args.massage(newFS, args);
			return new SocketClient(args);
		}

		@Override
		public Object call(Object... args) {
			args = Args.massage(callFS, args);
			SocketClient sc = new SocketClient(args);
			Object block = args[4];
			if (block == Boolean.FALSE)
				return sc;
			try {
				return Ops.call(block, sc);
			} finally {
				sc.close();
			}
		}
	};

	public String getInetAddress() {
		return socket.getInetAddress().getHostAddress();
	}

}
