package suneido.language;

import java.util.List;

public class SampleFunction extends SuFunction {
	private static final Object c;

	static {
		List<Object> constants = ClassGen.shareConstants.get();
		c = constants.get(0);
	}

	@Override
	public Object call(Object... args) {
		return c;
	}

}