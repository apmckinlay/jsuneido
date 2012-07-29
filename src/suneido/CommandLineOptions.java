package suneido;

public class CommandLineOptions {
	private static final int DEFAULT_PORT = 3147;
	private final String[] args;
	private int arg_i = 0;
	public enum Action
		{ REPL, SERVER, DUMP, LOAD, CHECK, VERSION, REBUILD, COMPACT, TEST, HELP,
		ERROR, TESTCLIENT, TESTSERVER, CLIENT,
		LOAD2, COMPACT2, REBUILD2 }
	public Action action;
	public String actionArg = null;
	public int serverPort = -1;
	public String remainder = "";
	public String impersonate = WhenBuilt.when();
	private static final int DEFAULT_TIMEOUT = 4 * 60; // 4 hours
	public int timeoutMin = DEFAULT_TIMEOUT;
	public boolean snapshotIsolation = false;
	public int max_update_tran_sec = 10;

	public static CommandLineOptions parse(String... args) {
		return new CommandLineOptions(args).parse();
	}

	public CommandLineOptions(String[] args) {
		this.args = args;
	}

	private CommandLineOptions parse() {
		for (arg_i = 0; arg_i < args.length; ++arg_i) {
			String arg = args[arg_i];
			if (!arg.startsWith("-"))
				break;
			else if (arg.equals("--")) {
				++arg_i;
				break;
			} else if (arg.equals("-repl"))
				setAction(Action.REPL);
			else if (arg.equals("-server") || arg.equals("-s"))
				setAction(Action.SERVER);
			else if (arg.equals("-client") || arg.equals("-c")) {
				setAction(Action.CLIENT);
				if (arg_i + 1 < args.length && ! args[arg_i + 1].startsWith("-"))
					actionArg = args[++arg_i];
				else
					actionArg = "127.0.0.1";
			} else if (arg.equals("-port") || arg.equals("-p")) {
				serverPort = getIntArg();
				if (serverPort <= 0 | 65535 < serverPort) {
					setAction(Action.ERROR);
					actionArg = "invalid port: " + args[arg_i - 1];
				}
			} else if (arg.equals("-dump") || arg.equals("-d"))
				setActionWithArg(Action.DUMP);
			else if (arg.equals("-load") || arg.equals("-l"))
				setActionWithArg(Action.LOAD);
			else if (arg.startsWith("-load:")) {
				setActionWithArg(Action.LOAD2);
				actionArg = arg.substring(6);
			} else if (arg.startsWith("-check")) {
				setAction(Action.CHECK);
				if (arg.startsWith("-check:"))
					actionArg = arg.substring(7);
			} else if (arg.equals("-rebuild"))
				setAction(Action.REBUILD);
			else if (arg.startsWith("-rebuild:")) {
				setAction(Action.REBUILD2);
				actionArg = arg.substring(9);
			} else if (arg.equals("-compact"))
				setAction(Action.COMPACT);
			else if (arg.startsWith("-compact:")) {
				setAction(Action.COMPACT2);
				actionArg = arg.substring(9);
			} else if (arg.equals("-tests") || arg.equals("-t"))
				setAction(Action.TEST);
			else if (arg.equals("-version") || arg.equals("-v"))
				setAction(Action.VERSION);
			else if (arg.equals("-help") || arg.equals("-h") || arg.equals("-?"))
				setAction(Action.HELP);
			else if (arg.equals("-testclient") || arg.equals("-tc"))
				setActionWithArg(Action.TESTCLIENT);
			else if (arg.equals("-testserver") || arg.equals("-ts"))
				setAction(Action.TESTSERVER);
			else if (arg.equals("-impersonate") || arg.equals("-i")) {
				impersonate = getArg();
				if (impersonate == null)
					error("impersonate requires value");
			} else if (arg.equals("-timeout") || arg.equals("-to"))
				timeoutMin = getIntArg();
			else if (arg.equals("-si"))
				snapshotIsolation = true;
			else if (arg.equals("-ut"))
				max_update_tran_sec = getIntArg();
			else
				error("unknown option: " + arg);
			if (action == Action.ERROR)
				return this;
		}
		defaults();
		validate();
		remainder();
		return this;
	}

	private int getIntArg() {
		try {
			String arg = getArg();
			if (arg == null)
				return -1;
			return Integer.parseInt(arg);
		} catch (NumberFormatException e) {
			return -1;
		}
	}

	private void setActionWithArg(Action action) {
		setAction(action);
		optionalStringValue();
	}

	private void setAction(Action action) {
		if (this.action == null)
			this.action = action;
		else {
			error("only one action is allowed, cannot have both " + this.action + " and " + action);
		}
	}

	private void error(String err) {
		action = Action.ERROR;
		actionArg = err;
	}

	private void optionalStringValue() {
		String arg = getArg();
		if (arg != null)
			actionArg = arg;
	}

	private String getArg() {
		String next = arg_i + 1 < args.length ? args[arg_i + 1] : "--";
		if (! next.startsWith("-") && ! next.equals("--")) {
			++arg_i;
			return next;
		} else
			return null;
	}

	private void defaults() {
		if (action == null)
			action = Action.REPL;
		if (serverPort == -1 &&
				(action == Action.SERVER || action == Action.CLIENT))
			serverPort = DEFAULT_PORT;
	}

	private void validate() {
		if (serverPort != -1 && action != Action.SERVER && action != Action.CLIENT)
			error("port should only be specifed with -server or -client, not " + action);
	}

	private void remainder() {
		if (arg_i >= args.length)
			return;
		StringBuilder sb = new StringBuilder();
		for (; arg_i < args.length; ++arg_i)
			sb.append(" ").append(args[arg_i]);
		remainder = sb.toString().trim();
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append(action);
		if (actionArg != null)
			sb.append(" ").append(actionArg);
		if (action != Action.ERROR) {
			if (serverPort != -1 && serverPort != DEFAULT_PORT)
				sb.append(" port=" + serverPort);
			if (remainder != "")
				sb.append(" rest: ").append(remainder);
		}
		if (impersonate != null && ! impersonate.equals(WhenBuilt.when()))
			sb.append(" impersonate='").append(impersonate).append("'");
		if (timeoutMin != DEFAULT_TIMEOUT)
			sb.append(" timeout=" + timeoutMin);
		return sb.toString();
	}

	public static void main(String[] args) {
		System.out.println(parse(args));
	}
}
