package suneido.language;

import suneido.SuValue;

abstract public class SuCallable extends SuValue {
	protected FunctionSpec params;
	protected Object[] constants;
	/** used to do super calls by methods and blocks within methods
	 *  set by {@link SuClass.linkMethods} */
	protected SuClass myClass;

	@Override
	public boolean isCallable() {
		return true;
	}

	public Object superInvoke(Object self, String member, Object... args) {
		return myClass.superInvoke(self, member, args);
	}

}
