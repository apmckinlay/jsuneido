package suneido.language.builtin;

import java.util.Date;

import suneido.language.SuFunction0;

public class UnixTime extends SuFunction0 {

	@Override
	public Object call0() {
		return new Date().getTime() / 1000;
	}

}
