package suneido.language.builtin;

import java.util.UUID;

import suneido.language.SuFunction0;

public class UuidString extends SuFunction0 {

	@Override
	public Object call0() {
		return UUID.randomUUID().toString();
	}

}