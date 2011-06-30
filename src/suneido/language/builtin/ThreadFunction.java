package suneido.language.builtin;

import suneido.Suneido;
import suneido.language.*;

public class ThreadFunction extends SuFunction {

	public static final FunctionSpec fs = new FunctionSpec("callable");

	@Override
	public Object call(Object... args) {
		args = Args.massage(fs, args);
		Thread thread = new Thread(new Callable(args[0]));
		thread.setDaemon(true); // so it won't stop Suneido exiting
		thread.start();
		return null;
	}

	private static class Callable implements Runnable {
		private final Object callable;

		public Callable(Object callable) {
			this.callable = callable;
		}

		@Override
		public void run() {
			try {
				Ops.call(callable);
			} catch (Throwable e ) {
				Suneido.uncaught("in thread", e);
			}
		}

	}

}
