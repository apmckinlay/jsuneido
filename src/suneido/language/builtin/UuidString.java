package suneido.language.builtin;

import java.util.UUID;

import suneido.language.BuiltinFunction0;

public class UuidString extends BuiltinFunction0 {

	@Override
	public Object call0() {
		return UUID.randomUUID().toString();
	}

}