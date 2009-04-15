package suneido.language;

class Print extends SuFunction {
	@Override
	public String toString() {
		return "Print";
	}
	@Override
	public Object call(Object... args) {
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