package suneido.language;

class Print extends BuiltinFunction {

	@Override
	public Object call(Object... args) {
		for (int i = 0; i < args.length; ++i) {
			if (i > 0)
				System.out.print(' ');
			Object x = args[i];
			if (x == Args.Special.NAMED)
				System.out.print((String) args[++i] + ":");
			else
				System.out.print(Ops.toStr(x));
		}
		System.out.println();
		return null;
	}

}