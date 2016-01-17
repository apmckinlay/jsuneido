/* Copyright 2010 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido.runtime.builtin;

import suneido.TheDbms;
import suneido.runtime.Ops;
import suneido.runtime.Params;
import suneido.util.Errlog;

public class SuThread {

	@Params("callable")
	public static Object ThreadFunction(Object fn) {
		Thread thread = new Thread(new Callable(fn));
		thread.setDaemon(true); // so it won't stop Suneido exiting
		thread.start();
		return null;
	}

	private static class Callable implements Runnable {
		private final Object callable;
		private final byte[] token;

		public Callable(Object callable) {
			// runs in the parent thread
			this.callable = callable;
			// NOTE: getting token here will keep parent connection alive
			token = TheDbms.dbms().token();
			// token may be empty if parent not authorized
		}

		@Override
		public void run() {
			// runs in the child thread
			try {
				// don't want to auth here since that will force dbms connection
				TheDbms.setAuthToken(token);
				Ops.call(callable);
			} catch (Throwable e ) {
				Errlog.error("Thread uncaught", e);
			}
		}

	}

}
