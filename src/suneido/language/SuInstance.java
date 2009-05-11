package suneido.language;

import static suneido.language.SuClass.Marker.*;

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
		if (method == "Base")
			return myclass;
		return myclass.invoke(self, method, args);
	}

	@Override
	public String toString() {
		if (myclass != null && myclass instanceof SuClass
				&& ((SuClass) myclass).get3("ToString") == METHOD)
			return Ops.toStr(invoke(this, "ToString"));
		else
			return myclass + "()";
	}

	@Override
	public Object get(Object member) {
		Object value = ivars.get(member);
		if (value != null)
			return value;
		value = ((SuClass) myclass).get2(member);
		if (value == GETTER)
			value = invoke(this, "Get_", member);
		else if (value == GETMEM)
			value = invoke(this, ("Get_" + (String) member).intern());
		else if (value == METHOD)
			value = new SuMethod(this, (String) member);
		if (value == null)
			throw new SuException("member not found: " + member);
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
