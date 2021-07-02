/* Copyright 2010 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido.runtime.builtin;

import java.util.Arrays;
import java.util.concurrent.atomic.AtomicInteger;

import suneido.SuException;
import suneido.SuObject;
import suneido.Suneido;
import suneido.TheDbms;
import suneido.runtime.*;
import suneido.util.Errlog;
import suneido.util.Util;

public class SuThread extends BuiltinClass {
	public static final SuThread singleton = new SuThread();
	private final AtomicInteger count = new AtomicInteger(0);

	private SuThread() {
		super(SuThread.class);
	}

	@Override
	protected Object newInstance(Object... args) {
		throw new SuException("can't create instances of Thread");
	}

	private static final FunctionSpec callableFS = new FunctionSpec("block");

	@Override
	public Object call(Object... args) {
		args = Args.massage(callableFS, args);
		Thread thread = new Thread(Suneido.threadGroup, new Callable(args[0]));
		thread.setDaemon(true); // so it won't stop Suneido exiting
		thread.setName("Thread-" + count.getAndIncrement());
		thread.start();
		return null;
	}

	private static class Callable implements Runnable {
		private final Object callable;

		public Callable(Object callable) {
			// runs in the parent thread
			this.callable = callable;
		}

		@Override
		public void run() {
			// runs in the child thread
			try {
				// don't want to auth here
				// since that will force dbms connection
				Ops.call(callable);
			} catch (Throwable e ) {
				if (! Suneido.exiting)
					Errlog.error("uncaught in thread:", e);
			} finally {
				TheDbms.close();
			}
		}
	}

	public static Object Count(Object self) {
		return Suneido.threadGroup.activeCount();
	}

	public static Object List(Object self) {
		Thread[] threads = list();
		SuObject list = new SuObject(threads.length);
		for (Thread t : threads)
			list.put(t.getName(), t.getState().toString());
		return list;
	}

	public static Thread[] list() {
		Thread[] threads;
		int n;
		do {
			threads = new Thread[Thread.activeCount() + 4];
			n = Suneido.threadGroup.enumerate(threads);
		} while (n >= threads.length);
		return Arrays.copyOf(threads, n);
	}

	@Params("name=null")
	public static Object Name(Object self, Object arg) {
		return extraName(arg == null ? null : Ops.toStr(arg));
	}

	public static String extraName(String extra) {
		Thread t = Thread.currentThread();
		String name = t.getName();
		if (extra != null) {
			name = Util.beforeFirst(name, " ") + " " + extra;
			t.setName(name);
		}
		return name;
	}

}
