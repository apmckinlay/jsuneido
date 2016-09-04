/* Copyright 2009 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido.runtime.builtin;

import static suneido.runtime.Args.Special.NAMED;

import java.io.IOException;
import java.net.Socket;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.atomic.AtomicInteger;

import com.google.common.collect.Lists;
import com.google.common.util.concurrent.ThreadFactoryBuilder;

import suneido.SuException;
import suneido.SuValue;
import suneido.Suneido;
import suneido.TheDbms;
import suneido.runtime.*;
import suneido.util.Errlog;
import suneido.util.ServerBySocket;
import suneido.util.Util;

/**
 * Thread per connection socket server. A user defined class derives from
 * SocketServer. The server is started by "calling" the class. A "master"
 * instance is created and any arguments are passed to the New of the class. The
 * master instance is duplicated (shallow copy) for each connection and Run is
 * called.
 * <p>
 * Uses {@link SocketClient} for actual IO
 * <p>
 * WARNING: Since it is thread per connection you should not use shared mutable
 * data structures.
 * <p>
 * Note: the name and exit parameters are not used on jSuneido.
 * <p>
 * Listener is run in a new thread.
 * Connections use a thread pool shared with all listeners.
 *
 * @see ServerBySocket
 */
public class SocketServer extends SuClass {
	public static final SocketServer singleton = new SocketServer();
	private static final AtomicInteger count = new AtomicInteger(0);
	private static final int MAXTHREADS = 200;

	private SocketServer() {
		super("builtin", "SocketServer", null,
				BuiltinMethods.methods("SocketServer", SocketServer.class));
	}

	@Override
	protected Object newInstance(Object... args) {
		throw new SuException("cannot create instances of SocketServer");
	}

	private static class Info {
		String name;
		int port;
	}

	public static Object CallClass(Object self, Object... args) {
		Info info = new Info();
		SuClass c = (SuClass) self;
		if (info.name == null)
			info.name = Ops.toStr(c.get1(c, "Name"));
		if (info.port == 0)
			info.port = Ops.toInt(c.get1(c, "Port"));
		args = convert(args, info);
		Thread thread = new Thread(Suneido.threadGroup,
				new Listener(info, new Master((SuClass) self, args)));
		thread.setDaemon(true);
		thread.setName("SocketServer-" + count.getAndIncrement());
		thread.start();
		return null;
	}

	/** @return An args array with name, port, and exit as named args (if present) */
	static Object[] convert(Object[] args, Info info) {
		if (args.length == 0)
			return args;
		List<Object> list2 = Lists.newArrayList();
		// need to use ArgsIterator to handle @args
		ArgsIterator iter = new ArgsIterator(args);
		Object x = iter.next();
		if (! (x instanceof Map.Entry)) {
			info.name = Ops.toStr(x);
			addNamed(list2, "name", x);
			if (! iter.hasNext())
				return list2.toArray();
			x = iter.next();
			if (! (x instanceof Map.Entry)) {
				info.port = Ops.toInt(x);
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
				Object key = e.getKey();
				Object val = e.getValue();
				addNamed(list, key, val);
				if (key.equals("name"))
					info.name = Ops.toStr(val);
				else if (key.equals("port"))
					info.port = Ops.toInt(val);
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

	private static class Listener implements Runnable {
		private static final ThreadPoolExecutor executor = executor();
		private final AtomicInteger nconn = new AtomicInteger(0);
		private final Master master;
		private final Info info;

		Listener(Info info, Master master) {
			this.info = info;
			this.master = master;
		}

		@Override
		public void run() {
			SuThread.extraName(info.name);
			ServerBySocket server = new ServerBySocket(executor,
					socket -> master.dup(socket, nconn.getAndIncrement()));
			try {
				server.run(info.port);
			} catch (IOException e) {
				throw new SuException("SocketServer failed", e);
			}
		}
	}
	
	private static ThreadPoolExecutor executor() {
		ThreadFactory threadFactory =
				new ThreadFactoryBuilder()
					.setThreadFactory(r -> new Thread(Suneido.threadGroup, r))
					.setDaemon(true)
					.build();
		ThreadPoolExecutor executor =
				(ThreadPoolExecutor) Executors.newCachedThreadPool(threadFactory);
		executor.setMaximumPoolSize(MAXTHREADS);
		executor.setRejectedExecutionHandler(new ThreadPoolExecutor.AbortPolicy());
		return executor;
	}

	public static class Master extends SuInstance {
		Master(SuClass serverClass, Object[] args) {
			super(serverClass);
			super.lookup("New").eval(this, args);
		}
		Instance dup(Socket socket, int nconn) {
			try {
				return new Instance(this, socket, nconn);
			} catch (IOException e) {
				throw new SuException("SocketServer failed", e);
			}
		}
	}

	// needs to be public for builtin methods to work
	public static class Instance extends SuInstance implements Runnable {
		final SocketClient socket;
		final int nconn;
		final String name;
		final String extra;

		Instance(Master master, Socket socket, int nconn) throws IOException {
			super(master);
			this.socket = new SocketClient(socket);
			this.nconn = nconn;
			String s = Thread.currentThread().getName();
			this.name = Util.beforeFirst(s, " ");
			this.extra = Util.afterFirst(s, " ");
		}

		@Override
		public void run() {
			try {
				Thread.currentThread().setName(
						name + "-connection-" + nconn + " " + extra);
				super.lookup("Run").eval0(this);
			} catch (Exception e) {
				Errlog.error("SocketServer", e);
			} finally {
				Thread.currentThread().setName("SocketServer-thread-pool");
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
