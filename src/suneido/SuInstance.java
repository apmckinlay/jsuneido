package suneido;

import java.util.HashMap;

/**
 * The class for instances of Suneido classes.
 * @author Andrew McKinlay
 */
public class SuInstance extends SuValue {
	/*private*/ SuValue myclass;
	private HashMap<SuValue,SuValue> m;
	
	SuInstance(SuValue myclass) {
		this.myclass = myclass;
	}
	
	/**
	 * Delegates to its parent SuClass instance.
	 */
	@Override
	public SuValue invoke(SuValue self, int method, SuValue ... args) {
		if (method == Symbols.CALLi)
			method = Symbols.CALL_INSTANCE;
		//TODO generic instance methods
		return myclass.invoke(self, method, args);
	}

	@Override
	public String toString() {
		return "a Suneido instance";
	}

	public SuValue getdata(SuValue member) {
		return m.get(member);
	}
	
	public void putdata(SuValue member, SuValue value) {
		m.put(member, value);
	}
}
