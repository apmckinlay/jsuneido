package suneido.language;

class Print extends SuFunction {
	@Override
	public String toString() {
		return "Print";
	}
	@Override
	public Object invoke(String method, Object... args) {
		if (method == "call")
			return invoke(args);
		else
			return super.invoke(method, args);
	}
	private Object invoke(Object... args) {
		for (int i = 0; i < args.length; ++i) {
			if (i > 0)
				System.out.print(' ');
			Object x = args[i];
			if (x == Args.Special.NAMED)
				System.out.print((String) args[++i] + ":");
			else
				System.out.print(Ops.toString(x));
		}
		System.out.println();
		return null;
	}
}