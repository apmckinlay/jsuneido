package suneido;

public class CommandLineOptions {
	private static final int DEFAULT_PORT = 3147;
	private final String[] args;
	private int arg_i = 0;
	public enum Action {
		REPL, SERVER, DUMP, LOAD, CHECK, VERSION, REBUILD, COMPACT, TEST, HELP,
		ERROR
	}
	public Action action;
	public String action_arg = null;
	public int server_port = -1;
	public String remainder = "";

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
			else if (arg.equals("-port") || arg.equals("-p")) {
				try {
					server_port = Integer.parseInt(args[++arg_i]);
				} catch (NumberFormatException e) {
				}
				if (server_port <= 0 | 65535 < server_port) {
					setAction(Action.ERROR);
					action_arg = "invalid port: " + args[arg_i - 1];
				}
			} else if (arg.equals("-dump") || arg.equals("-d"))
				setActionWithArg(Action.DUMP);
			else if (arg.equals("-load") || arg.equals("-l"))
				setActionWithArg(Action.LOAD);
			else if (arg.equals("-check"))
				setAction(Action.CHECK);
			else if (arg.equals("-rebuild") )
				setAction(Action.REBUILD);
			else if (arg.equals("-compact"))
				setAction(Action.COMPACT);
			else if (arg.equals("-tests") || arg.equals("-t"))
				setAction(Action.TEST);
			else if (arg.equals("-version") | arg.equals("-v"))
				setAction(Action.VERSION);
			else if (arg.equals("-help") || arg.equals("-h") || arg.equals("-?"))
				setAction(Action.HELP);
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
		action_arg = err;
	}

	private void optionalStringValue() {
		String next = arg_i + 1 < args.length ? args[arg_i + 1] : "--";
		if (! next.startsWith("-") && ! next.equals("--")) {
			action_arg = next;
			++arg_i;
		}
	}

	private void defaults() {
		if (action == null)
			action = Action.SERVER;
		if (action == Action.SERVER && server_port == -1)
			server_port = DEFAULT_PORT;
	}

	private void validate() {
		if (server_port != -1 && action != Action.SERVER)
			error("port should only be specifed with server, not " + action);
	}

	private void remainder() {
		if (arg_i >= args.length)
			return;
		StringBuilder sb = new StringBuilder();
		for (; arg_i < args.length; ++arg_i) {
			sb.append(" ").append(args[arg_i]);
		}
		remainder = sb.substring(1);
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append(action);
		if (action_arg != null)
			sb.append(" ").append(action_arg);
		if (action != Action.ERROR) {
			if (server_port != -1 && server_port != DEFAULT_PORT)
				sb.append(" port=" + server_port);
			if (remainder != "")
				sb.append(" rest: ").append(remainder);
		}
		return sb.toString();
	}

	public static void main(String[] args) {
		System.out.println(parse(args));
	}
}
