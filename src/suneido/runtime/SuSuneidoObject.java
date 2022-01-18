package suneido.runtime;

import suneido.SuValue;
import suneido.SuObject;
import suneido.runtime.builtin.SuThread;
import suneido.runtime.builtin.ObjectMethods;

public class SuSuneidoObject extends SuValue {
	private final SuObject ob = new SuObject();


	@Override
	public String typeName() {
		return "Object";
	}

	@Override
	public Object get(Object member) {
		SuObject sub = SuThread.subSuneido.get();
		if (sub != null) {
			return sub.get(member);
		} else {
			return ob.get(member);
		}
	}

	@Override
	public void put(Object member, Object value) {
		SuObject sub = SuThread.subSuneido.get();
		if (sub != null) {
			sub.put(member, value);
		} else {
			ob.put(member, value);
		}
	}

	@Override
	public SuObject toObject() {
		SuObject sub = SuThread.subSuneido.get();
		return sub != null ? sub : ob;
	}

	@Override
	public SuValue lookup(String method) {
		return ObjectMethods.lookup(method);
	}

	@Override
	public String toString() {
		SuObject sub = SuThread.subSuneido.get();
		if (sub != null) {
			return sub.toString();
		} else {
			return ob.toString();
		}
	}
}
