package suneido.language;

import java.util.HashMap;
import java.util.Map;

import suneido.SuException;
import suneido.SuValue;

public class SuInstance extends SuValue {
	private final SuValue myclass;
	private final Map<String, Object> ivars = new HashMap<String, Object>();

	public SuInstance(SuValue myclass) {
		this.myclass = myclass;
	}

	@Override
	public Object call(Object... args) {
		return myclass.invoke(this, "Call", args);
		// MAYBE myclass.Call()
	}

	@Override
	public Object invoke(Object self, String method, Object... args) {
		return myclass.invoke(self, method, args);
	}

	@Override
	public String toString() {
		// TODO handle user defined toString
		return myclass + "()";
	}

	@Override
	public Object get(Object member) {
		if (!(member instanceof String))
			throw new SuException("non-string member name: "
					+ Ops.typeName(member));
		Object value = ivars.get(member);
		if (value == null) {
			value = myclass.get(member);
			if (value == null)
				throw new SuException("uninitialized member " + member);
		}
		return value;
	}

	@Override
	public void put(Object member, Object value) {
		if (!(member instanceof String))
			throw new SuException("non-string member name: "
					+ Ops.typeName(member));
		ivars.put((String) member, value);
	}

	@Override
	public boolean equals(Object other) {
		if (other == this)
			return true;
		if (!(other instanceof SuInstance))
			return false;
		SuInstance that = (SuInstance) other;
		return this.myclass == that.myclass && this.ivars == that.ivars;
	}

	@Override
	public int hashCode() {
		int result = 17;
		result = 31 * result + myclass.hashCode();
		result = 31 * result + ivars.hashCode();
		return result;
	}

}
