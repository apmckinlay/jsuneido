package suneido.language;

import suneido.SuValue;

abstract public class SuCallable extends SuValue {
	protected FunctionSpec[] params;
	protected Object[][] constants;
}
