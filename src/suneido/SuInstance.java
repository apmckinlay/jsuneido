package suneido;

import java.util.HashMap;

public class SuInstance extends SuValue {
	private SuClass parent;
	private HashMap<SuValue,SuValue> m;
	
	SuInstance(SuClass parent) {
		this.parent = parent;
	}
	
	@Override
	public SuValue invoke(SuValue self, int method, SuValue ... args) {
		if (method == Symbols.CALLi)
			method = Symbols.CALL_INSTANCE;
		return parent.invoke(self, method, args);
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
