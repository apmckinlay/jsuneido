/* Copyright 2009 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido.runtime.builtin;

import static suneido.runtime.Args.Special.NAMED;

import java.io.IOException;
import java.net.Socket;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;

import suneido.SuException;
import suneido.SuValue;
import suneido.TheDbms;
import suneido.runtime.*;
import suneido.util.Errlog;
import suneido.util.ServerBySocket;
import suneido.util.ServerBySocket.HandlerFactory;

import com.google.common.collect.Lists;
import com.google.common.util.concurrent.ThreadFactoryBuilder;

/**
 * Thread per connection socket server.
 * A user defined class derives from SocketServer.
 * The server is started by "calling" the class.
 * A "master" instance is created
 * and any arguments are passed to the New of the class.
 * The master instance is duplicated (shallow copy) for each connection
 * and Run is called.
 * <p>
 * Uses {@link SocketClient} for actual IO
 * <p>
 * WARNING: Since it is thread per connection
 * you should not use shared mutable data structures.<p>
 * Note: the name and exit parameters are not used on jSuneido.
 * @see ServerBySocket
 */
public class SocketServer extends SuClass {
	public static final SocketServer singleton = new SocketServer();

	private SocketServer() {
		super("builtin", "SocketServer", null,
				BuiltinMethods.methods("SocketServer", SocketServer.class));
	}

	@Override
	protected Object newInstance(Object... args) {
		throw new SuException("cannot create instances of SocketServer");
	}

	public static Object CallClass(Object self, Object... args) {
		args = convert(args);
		int port = Ops.toInt(getPort(self, args));
		Thread thread = new Thread(
				new Listener(port, new Instance((SuClass) self, args)));
		thread.setDaemon(true);
		thread.start();
		return null;
	}

	/** @return An args array with name, port, and exit as named args (if present) */
	static Object[] convert(Object[] args) {
		if (args.length == 0)
			return args;
		List<Object> list2 = Lists.newArrayList();
		// need to use ArgsIterator to handle @args
		ArgsIterator iter = new ArgsIterator(args);
		Object x = iter.next();
		if (! (x instanceof Map.Entry)) {
			addNamed(list2, "name", x);
			if (! iter.hasNext())
				return list2.toArray();
			x = iter.next();
			if (! (x instanceof Map.Entry)) {
				addNamed(list2, "port", x);
				if (! iter.hasNext())
					return list2.toArray();
				x = iter.next();
				if (! (x instanceof Map.Entry)) {
					addNamed(list2, "exit", x);
					if (! iter.hasNext())
						return list2.toArray();
					x = iter.next();
				}
			}
		}
		List<Object> list = Lists.newArrayList();
		while (true) {
			if (! (x instanceof Map.Entry))
				list.add(x);
			else {
				@SuppressWarnings("rawtypes")
				Map.Entry e = (Map.Entry) x;
				addNamed(list, e.getKey(), e.getValue());
			}
			if (! iter.hasNext())
				break;
			x = iter.next();
		}
		// put name and port at end, in case of other unnamed
		list.addAll(list2);
		return list.toArray();
	}

	private static void addNamed(List<Object> list, Object name, Object value ) {
		list.add(NAMED);
		list.add(name);
		list.add(value);
	}

	private static Object getPort(Object self, Object[] args) {
		ArgsIterator iter = new ArgsIterator(args);
		while (iter.hasNext()) {
			Object x = iter.next();
			if (x instanceof Map.Entry) {
				@SuppressWarnings("rawtypes")
				Map.Entry e = (Map.Entry) x;
				if (e.getKey().equals("port"))
					return e.getValue();
			}
		}
		return Ops.get(self, "Port");
	}

	private static class Listener implements Runnable {
		private static final ThreadFactory threadFactory =
				new ThreadFactoryBuilder()
					.setDaemon(true)
					.setNameFormat("SocketServer-thread-%d")
					.build();
		private static final ExecutorService executor =
				Executors.newCachedThreadPool(threadFactory);
		final Instance master;
		final int port;

		Listener(int port, Instance master) {
			this.port = port;
			this.master = master;
		}

		@Override
		public void run() {
			ServerBySocket server =
					new ServerBySocket(executor, new ListenerHandlerFactory());
			try {
				server.run(port);
			} catch (IOException e) {
				throw new SuException("SocketServer failed", e);
			}
		}

		private class ListenerHandlerFactory implements HandlerFactory {
			@Override
			public Runnable newHandler(Socket socket) throws IOException {
				return Instance.dup(master, socket);
			}
		}
	}

	// needs to be public for builtin methods to work
	public static class Instance extends SuInstance implements Runnable {
		SocketClient socket;

		Instance(SuClass serverClass, Object[] args) {
			super(serverClass);
			super.lookup("New").eval(this, args);
		}

		Instance(Instance orig, Socket socket) throws IOException {
			super(orig);
			this.socket = new SocketClient(socket);
		}

		static Instance dup(Instance orig, Socket socket) {
			try {
				return new Instance(orig, socket);
			} catch (IOException e) {
				throw new SuException("SocketServer failed", e);
			}
		}

		@Override
		public void run() {
			try {
				super.lookup("Run").eval0(this);
			} catch (Exception e) {
				Errlog.errlog("exception in SocketServer", e);
			} finally {
				socket.close();
				TheDbms.closeIfIdle();
				/*
				 * need to close idle connections because threads may be active
				 * but only rarely use the database connection
				 * so the connection will get timed out by the database server
				 * and then if we try to use it again we'll get an error
				 */
			}
		}

		@Override
		public SuValue lookup(String method) {
			SuValue f = builtins.getMethod(method);
			if (f != null)
				return f;
			f = SocketClient.getMethod(method);
			if (f != null)
				return f;
			return super.lookup(method);
		}

		//
		// BUILT-IN METHODS
		//

		private static final BuiltinMethods builtins = new BuiltinMethods(
				"socketserver", Instance.class);

		public static Object RemoteUser(Object self) {
			Instance instance = (Instance) self;
			return instance.socket.getInetAddress();
		};
	}

}
