/* Copyright 2009 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido.language.builtin;

import static suneido.util.Util.array;

import java.io.IOException;
import java.net.Socket;
import java.util.concurrent.*;

import suneido.*;
import suneido.language.*;
import suneido.util.ServerBySocket;
import suneido.util.ServerBySocket.HandlerFactory;

import com.google.common.collect.ImmutableMap;
import com.google.common.util.concurrent.ThreadFactoryBuilder;

public class SocketServer extends SuClass {
	public static final SocketServer singleton = new SocketServer();

	private SocketServer() {
		super("SocketServer", null,
				ImmutableMap.of("CallClass", new CallClass()));
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
		private static final int CORE_THREADS = 0;
		private static final int MAX_THREADS = 8;
		private static final ThreadFactory threadFactory =
				new ThreadFactoryBuilder().setDaemon(true).build();
		private static final ThreadPoolExecutor executor =
				new ThreadPoolExecutor(
					CORE_THREADS, MAX_THREADS,
					1, TimeUnit.MINUTES,
					new SynchronousQueue<Runnable>(),
					threadFactory,
					new ThreadPoolExecutor.CallerRunsPolicy());
		SuClass serverClass;
		int port;

		Listener(SuClass serverClass, int port) {
			this.serverClass = serverClass;
			this.port = port;
		}

		@Override
		public void run() {
			ServerBySocket server =	new ServerBySocket(executor,
					new ListenerHandlerFactory());
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

	static class Instance extends SuInstance implements Runnable {
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
				super.lookup("New").eval0(this);
				super.lookup("Run").eval0(this);
			} catch (SuException e) {
				Suneido.errlog("exception in SocketServer", e);
			} finally {
				socket.close();
			}
		}

		@Override
		public SuValue lookup(String method) {
			if (method == "RemoteUser")
				return RemoteUser;
			SuValue f = SocketClient.getMethod(method);
			if (f != null)
				return f;
			return super.lookup(method);
		}

		private final SuValue RemoteUser = new SuMethod0() {
			@Override
			public Object eval0(Object self) {
				return address;
			}
		};

	}

}
