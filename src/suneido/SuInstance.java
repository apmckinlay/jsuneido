package suneido;

import java.util.HashMap;

import suneido.Symbols.Num;

/**
 * The class for instances of Suneido classes.
 * Contains a HashMap of instance variable (members).
 * @see SuClass
 * @see SuMethod
 * @author Andrew McKinlay
 * <p><small>Copyright 2008 Suneido Software Corp. All rights reserved. Licensed under GPLv2.</small></p>
 */
public class SuInstance extends SuValue {
	/*private*/ SuValue myclass;
	private final HashMap<SuValue,SuValue> m = new HashMap<SuValue, SuValue>();

	SuInstance(SuValue myclass) {
		this.myclass = myclass;
	}

	/**
	 * Delegates to its parent SuClass instance.
	 * Converts CALL to CALL_INSTANCE to differentiate from CALL_CLASS.
	 * Would be simpler to translate CALL to CALL_CLASS instead
	 * but there's no easy place to do this.
	 * So compiler has to compile Call as CALL_INSTANCE
	 * and CallClass as CALL.
	 */
	@Override
	public SuValue invoke(SuValue self, int method, SuValue ... args) {
		if (method == Num.CALL)
			method = Num.CALL_INSTANCE;
		//TODO generic instance methods
		return myclass.invoke(self, method, args);
	}

	@Override
	public String toString() {
		return "a Suneido instance";
	}

	@Override
	public SuValue getdata(SuValue member) {
		return m.get(member);
	}

	@Override
	public void putdata(SuValue member, SuValue value) {
		m.put(member, value);
	}
}
