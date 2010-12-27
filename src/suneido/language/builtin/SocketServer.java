/* Copyright 2009 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido.language.builtin;

import static suneido.util.Util.array;

import java.io.IOException;
import java.net.Socket;

import suneido.SuException;
import suneido.SuValue;
import suneido.language.*;
import suneido.util.ServerBySocket;
import suneido.util.ServerBySocket.HandlerFactory;

import com.google.common.collect.ImmutableMap;

public class SocketServer extends SuClass {
	public static final SocketServer singleton = new SocketServer();

	private SocketServer() {
		super("SocketServer", null,
				ImmutableMap.of("CallClass", new CallClass()));
	}

	@Override
	protected void linkMethods() {
	}

	@Override
	protected Object newInstance(Object... args) {
		throw new SuException("cannot create instances of SocketServer");
	}

	public static class CallClass extends SuMethod2 {
		{ params = new FunctionSpec(array("name", "port"), false, false); }
		@Override
		public Object eval2(Object self, Object a, Object b) {
			int port = Ops.toInt(b != Boolean.FALSE ? b : Ops.get(self, "Port"));
			Thread thread = new Thread(new Listener((SuClass) self, port));
			thread.setDaemon(true);
			thread.start();
			return null;
		}
	}

	private static class Listener implements Runnable {
		SuClass serverClass;
		int port;

		Listener(SuClass serverClass, int port) {
			this.serverClass = serverClass;
			this.port = port;
		}

		@Override
		public void run() {
			ServerBySocket server =	new ServerBySocket(new ListenerHandlerFactory());
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
		final SocketClient socket;
		final String address;

		Instance(SuClass serverClass, Socket socket, String address) throws IOException {
			super(serverClass);
			this.socket = new SocketClient(socket);
			this.address = address;
		}

		@Override
		public void run() {
			try {
				lookup("New").eval0(this);
				lookup("Run").eval0(this);
			} finally {
				socket.close();
			}
		}

		@Override
		public SuValue lookup(String method) {
			if (method == "RemoteUser")
				return RemoteUser;
			return SocketClient.clazz.lookup(method);
		}

		private final SuValue RemoteUser = new SuMethod0() {
			@Override
			public Object eval0(Object self) {
				return address;
			}
		};

	}

}
