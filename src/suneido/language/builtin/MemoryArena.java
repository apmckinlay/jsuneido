package suneido.language.builtin;

import suneido.language.SuFunction0;

public class MemoryArena extends SuFunction0 {

	@Override
	public Object call0() {
		return Runtime.getRuntime().totalMemory();
	}

}
