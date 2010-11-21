package suneido.language.builtin;

import java.util.Date;

import suneido.language.BuiltinFunction0;

public class UnixTime extends BuiltinFunction0 {

	@Override
	public Object call0() {
		return new Date().getTime() / 1000;
	}

}
