package suneido;

import java.util.Arrays;

public class CommandLineOptions {
	private static final String DEFAULT_IP = "127.0.0.1";
	private static final int DEFAULT_PORT = 3147;
	private final String[] args;
	private int arg_i = 0;
	public enum Action {
		REPL, SERVER, DUMP, LOAD, CHECK, VERSION, REBUILD
	}
	public Action action;
	public String action_arg = null;
	public int server_port = -1;

	public static CommandLineOptions parse(String[] args) {
		return new CommandLineOptions(args).parse();
	}

	public CommandLineOptions(String[] args) {
		this.args = args;
	}

	public CommandLineOptions parse() {
		for (arg_i = 0; arg_i < args.length; ++arg_i) {
			String arg = args[arg_i];
			if (!arg.startsWith("-"))
				break;
			else if (arg.equals("--")) {
				++arg_i;
				break;
			} else if (arg.equals("-repl"))
				setAction(Action.REPL);
			else if (arg.equals("-server") || arg.equals("-s")) {
				setAction(Action.SERVER);
				optionalStringValue();
			} else if (arg.equals("-port") || arg.equals("-p")) {
				try {
					server_port = Integer.parseInt(args[++arg_i]);
				} catch (NumberFormatException e) {
				}
				if (server_port <= 0 | 65535 < server_port)
					throw new SuException("invalid port: " + args[arg_i - 1]);
			} else if (arg.equals("-dump")) {
				setAction(Action.DUMP);
				optionalStringValue();
			} else if (arg.equals("-load"))
				setAction(Action.LOAD);
			else if (arg.equals("-check"))
				setAction(Action.CHECK);
			else if (arg.equals("-rebuild"))
				setAction(Action.REBUILD);
			else if (arg.equals("-version"))
				setAction(Action.VERSION);
			else
				throw new SuException("unknown command line argument: " + arg);
		}
		defaults();
		validate();
		return this;
	}

	private void validate() {
		if (server_port != -1 && action != Action.SERVER)
			throw new SuException("port should only be specifed with server, not " + action);
	}

	private void defaults() {
		if (action == null)
			action = Action.SERVER;
		if (action == Action.SERVER) {
			if (action_arg == null)
				action_arg = DEFAULT_IP;
			if (server_port == -1)
				server_port = DEFAULT_PORT;
		}
	}

	private void setAction(Action action) {
		if (this.action != null)
			throw new SuException("only one action is allowed, either " + this.action + " or " + action);
		this.action = action;
	}

	private void optionalStringValue() {
		String next = arg_i + 1 < args.length ? args[arg_i + 1] : "--";
		if (! next.startsWith("-") && ! next.equals("--")) {
			action_arg = next;
			++arg_i;
		}
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append(action);
		if (action_arg != null &&
				!(action == Action.SERVER && action_arg == DEFAULT_IP))
			sb.append(" ").append(action_arg);
		if (server_port != -1 && server_port != DEFAULT_PORT)
			sb.append(" port=" + server_port);
		if (arg_i < args.length)
			sb.append(" rest: ")
					.append(Arrays.toString(Arrays.copyOfRange(args, arg_i, args.length)));
		return sb.toString();
	}

	public static void main(String[] args) {
		System.out.println(parse(args));
	}
}
