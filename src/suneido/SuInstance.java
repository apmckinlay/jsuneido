package suneido;

import java.util.HashMap;
import static suneido.Symbols.*;

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

	public SuValue getdata(SuValue member) {
		return m.get(member);
	}
	
	public void putdata(SuValue member, SuValue value) {
		m.put(member, value);
	}
}
