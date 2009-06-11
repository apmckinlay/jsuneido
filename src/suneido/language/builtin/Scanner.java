package suneido.language.builtin;

import suneido.language.BuiltinClass;

public class Scanner extends BuiltinClass {

	@Override
	public ScannerInstance newInstance(Object[] args) {
		return new ScannerInstance(args);
	}

}
