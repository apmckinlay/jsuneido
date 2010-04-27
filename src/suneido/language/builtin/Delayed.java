package suneido.language.builtin;

import java.util.concurrent.TimeUnit;

import suneido.language.*;
import suneido.util.SocketServer;

public class Delayed extends BuiltinFunction {

	private static final FunctionSpec fs = new FunctionSpec("ms", "function");

	@Override
	public Object call(Object... args) {
		args = Args.massage(fs, args);
		SocketServer.scheduler.schedule(new Run(args[1]),
				Ops.toInt(args[0]), TimeUnit.MILLISECONDS);
		return null;
	}

	private static class Run implements Runnable {
		private final Object fn;
		public Run(Object fn) {
			this.fn = fn;
		}
		public void run() {
			Ops.call(fn);
		}
	}

	// TODO return a Timer object that with a Kill method

}
