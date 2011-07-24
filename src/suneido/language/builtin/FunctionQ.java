package suneido.language.builtin;

import suneido.language.*;

public class FunctionQ extends SuFunction1 {

	@Override
	public Object call1(Object a) {
		return a instanceof SuFunction ||
				a instanceof SuBoundMethod ||
				a instanceof SuBlock;
	}

}
