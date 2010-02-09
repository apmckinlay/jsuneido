package suneido;

public class CommandLineOptions {
	public enum Action {
		REPL, DUMP, LOAD, SERVER, VERSION
	}
	public Action action = null;

	public CommandLineOptions(String[] args) {
		for (String arg : args) {
			if (arg.equals("-repl"))
				action = Action.REPL;
			else if (arg.equals("-server"))
				action = Action.SERVER;
			else if (arg.equals("-dump"))
				action = Action.DUMP;
			else if (arg.equals("-load"))
				action = Action.LOAD;
			else if (arg.equals("-version"))
				action = Action.VERSION;
		}
	}
}
