package suneido.language;

import suneido.SuValue;

abstract public class SuCallable extends SuValue {
	public FunctionSpec[] params;
	public Object[][] constants;
	public Object self = null; // used within call methods, set by object.Eval
}
