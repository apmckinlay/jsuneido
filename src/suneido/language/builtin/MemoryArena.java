package suneido.language.builtin;

import suneido.language.BuiltinFunction0;

public class MemoryArena extends BuiltinFunction0 {

	@Override
	public Object call0() {
		return (int) Runtime.getRuntime().totalMemory();
	}

}
