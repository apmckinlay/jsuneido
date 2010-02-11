package suneido;

// TODO disallow multiple actions
// TODO allow dump/load single tables
// TODO -port
public class CommandLineOptions {
	public enum Action {
		REPL, SERVER, DUMP, LOAD, CHECK, VERSION
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
			else if (arg.equals("-check"))
				action = Action.CHECK;
			else if (arg.equals("-version"))
				action = Action.VERSION;
			else
				throw new SuException("unknown command line argument: " + arg);
		}
	}
}
