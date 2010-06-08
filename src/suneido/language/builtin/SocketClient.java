package suneido.language.builtin;

import static suneido.util.Util.array;

import java.io.*;
import java.net.Socket;
import java.net.SocketTimeoutException;

import suneido.SuException;
import suneido.SuValue;
import suneido.language.*;

public class SocketClient extends BuiltinClass {

	@Override
	public Instance newInstance(Object[] args) {
		return new Instance(args);
	}

	private static final FunctionSpec callFS =
		new FunctionSpec(array("address", "port", "timeout", "block"),
				60, false);

	@Override
	public Object call(Object... args) {
		Instance sc = newInstance(args);
		args = Args.massage(callFS, args);
		Object block = args[3];
		if (block == Boolean.FALSE)
			return sc;
		try {
			return Ops.call(block, sc);
		} finally {
			sc.Close();
		}
	}

	static class Instance extends SuValue {
		private final Socket socket;
		private final DataInputStream input;
		private final DataOutputStream output;

		private static final FunctionSpec newFS =
			new FunctionSpec(array("address", "port", "timeout"), 60);

		public Instance(Object[] args) {
			args = Args.massage(newFS, args);
			String address = Ops.toStr(args[0]);
			int port = Ops.toInt(args[1]);
			int timeout = Ops.toInt(args[2]);
			try {
				socket = new Socket(address, port);
				socket.setSoTimeout(timeout * 1000);
				socket.setTcpNoDelay(true); // disable nagle
				input = new DataInputStream(socket.getInputStream());
				output = new DataOutputStream(socket.getOutputStream());
			} catch (IOException e) {
				throw new SuException("SocketClient open failed", e);
			}
		}

		public Instance(Socket socket) throws IOException {
			this.socket = socket;
			socket.setTcpNoDelay(true); // disable nagle
			input = new DataInputStream(socket.getInputStream());
			output = new DataOutputStream(socket.getOutputStream());
		}

		@Override
		public Object invoke(Object self, String method, Object... args) {
			if (method == "Close")
				return Close(args);
			if (method == "Read")
				return Read(args);
			if (method == "Readline")
				return Readline(args);
			if (method == "Write")
				return Write(args);
			if (method == "Writeline")
				return Writeline(args);
			throw SuException.methodNotFound("socket", method);
		}

		Object Close(Object... args) {
			try {
				socket.close();
			} catch (IOException e) {
				throw new SuException("socketClient.Close failed", e);
			}
			return null;
		}

		private static final FunctionSpec readFS = new FunctionSpec("size");

		private Object Read(Object[] args) {
			args = Args.massage(readFS, args);
			int n = Ops.toInt(args[0]);
			if (n == 0)
				return "";
			byte[] data = new byte[n];
			int nr = 0;
			try {
				do {
					int r = input.read(data);
					if (r == -1)
						break;
					nr += r;
				} while (nr < n);
			} catch (SocketTimeoutException e) {
				// handled below
			} catch (IOException e) {
				throw new SuException("socketClient.Read failed", e);
			}
			if (nr == 0)
				throw new SuException("socket client: lost connection or timeout");
			return new String(data, 0, nr);
		}

		@SuppressWarnings("deprecation")
		private Object Readline(Object[] args) {
			Args.massage(FunctionSpec.noParams, args);
			try {
				String line = input.readLine();
				if (line == null)
					throw new SuException("socket client: lost connection or timeout");
				return line;
			} catch (IOException e) {
				throw new SuException("socketClient.Readline failed", e);
			}
		}

		private Object Write(Object[] args) {
			args = Args.massage(FunctionSpec.string, args);
			String data = Ops.toStr(args[0]);
			try {
				output.write(data.getBytes());
			} catch (IOException e) {
				throw new SuException("socketClient.Write failed", e);
			}
			return null;
		}

		private static final byte[] newline = "\r\n".getBytes();

		private Object Writeline(Object[] args) {
			Write(args);
			try {
				output.write(newline);
			} catch (IOException e) {
				throw new SuException("socketClient.Writeline failed", e);
			}
			return null;
		}
	}

}
