/**
 *
 */
package suneido.language;

import suneido.SuString;
import suneido.SuValue;

class Print extends SuFunction {
	@Override
	public String toString() {
		return "Print";
	}
	@Override
	public SuValue invoke(String method, SuValue... args) {
		if (method == "call")
			return invoke(args);
		else
			return super.invoke(method, args);
	}
	private SuValue invoke(SuValue... args) {
		for (int i = 0; i < args.length; ++i) {
			if (i > 0)
				System.out.print(' ');
			SuValue x = args[i];
			if (x == NAMED)
				System.out.print(args[++i].string() + ":");
			else if (x instanceof SuString)
				System.out.print(x.string());
			else
				System.out.print(x.toString());
		}
		System.out.println();
		return null;
	}
}