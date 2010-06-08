package suneido.language.builtin;

import static suneido.util.Util.array;

import java.io.IOException;
import java.net.Socket;

import suneido.SuException;
import suneido.SuValue;
import suneido.language.*;
import suneido.util.ServerBySocket;
import suneido.util.ServerBySocket.HandlerFactory;

public class SocketServer extends SuClass {

	@Override
	public Object invoke(Object self, String method, Object... args) {
		if (method == "CallClass")
			return CallClass(self, args);
		return super.invoke(self, method, args);
	}

	private static final FunctionSpec callFS =
			new FunctionSpec(array("name", "port"), false, false);

	private Object CallClass(Object self, Object[] args) {
		args = Args.massage(callFS, args);
		int port = Ops.toInt(args[1] != Boolean.FALSE ? args[1] : Ops.get(self, "Port"));
		Thread thread = new Thread(new Listener((SuValue) self, port));
		thread.setDaemon(true);
		thread.start();
		return null;
	}

	private static class Listener implements Runnable {
		SuValue serverClass;
		int port;

		Listener(SuValue serverClass, int port) {
			this.serverClass = serverClass;
			this.port = port;
		}

		@Override
		public void run() {
			ServerBySocket server = new ServerBySocket(new ListenerHandlerFactory());
			try {
				server.run(port);
			} catch (IOException e) {
				throw new SuException("SocketServer failed", e);
			}
		}

		private class ListenerHandlerFactory implements HandlerFactory {
			@Override
			public Runnable newHandler(Socket socket, String address) throws IOException {
				return new Instance(serverClass, socket, address);
			}
		}

	}

	private static class Instance extends SuInstance implements Runnable {
		final SocketClient.Instance socket;
		final String address;

		Instance(SuValue serverClass, Socket socket, String address) throws IOException {
			super(serverClass);
			this.socket = new SocketClient.Instance(socket);
			this.address = address;
		}

		@Override
		public void run() {
			try {
				invoke(this, "_init");
				invoke(this, "Run");
			} finally {
				socket.Close();
			}
		}

		@Override
		public Object invoke(Object self, String method, Object... args) {
			if (method == "RemoteUser")
				return RemoteUser(args);
			if (method == "Read" || method == "Readline" ||
					method == "Write" || method == "Writeline")
				return socket.invoke(self, method, args);
			return super.invoke(self, method, args);
		}

		private Object RemoteUser(Object[] args) {
			return address;
		}

	}

}
