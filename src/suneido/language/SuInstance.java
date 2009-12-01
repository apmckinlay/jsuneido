package suneido.language;

import static suneido.language.SuClass.Marker.GETMEM;
import static suneido.language.SuClass.Marker.GETTER;

import java.util.HashMap;
import java.util.Map;

import suneido.*;
import suneido.language.SuClass.Method;
import suneido.language.builtin.ContainerMethods;

public class SuInstance extends SuValue {
	private final SuValue myclass;
	private final Map<String, Object> ivars = new HashMap<String, Object>();

	public SuInstance(SuValue myclass) {
		this.myclass = myclass;
	}

	@Override
	public Object call(Object... args) {
		return myclass.invoke(this, "Call", args);
	}

	@Override
	public Object invoke(Object self, String method, Object... args) {
		// self will normally be the same as "this"
		// except when object.Eval is used
		if (method == "Base")
			return Base(self, args);
		if (method == "Delete")
			return Delete(self, args);
		if (method == "Eval")
			return ContainerMethods.Eval(self, args);
		if (method == "Eval2")
			return ContainerMethods.Eval2(self, args);
		if (method == "Members")
			return Members(self, args);
		if (method == "Member?")
			return MemberQ(self, args);
		return myclass.invoke(self, method, args);
	}

	private Object Base(Object self, Object[] args) {
		Args.massage(FunctionSpec.noParams, args);
		return myclass;
	}

	private static final FunctionSpec keyFS = new FunctionSpec("key");

	private Object Delete(Object self, Object[] args) {
		args = Args.massage(keyFS, args);
		return ivars.remove(args[0]) == null ? false : this;
	}

	private Object MemberQ(Object self, Object[] args) {
		args = Args.massage(keyFS, args);
		String key = Ops.toStr(args[0]);
		if (ivars.containsKey(key))
			return true;
		return myclass.invoke(myclass, "Member?", args);
	}

	private Object Members(Object self, Object[] args) {
		Args.massage(FunctionSpec.noParams, args);
		return new SuContainer(ivars.keySet());
	}

	@Override
	public String toString() {
		if (myclass != null && myclass instanceof SuClass
				&& ((SuClass) myclass).get3("ToString") instanceof Method)
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
		else if (value instanceof Method)
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
		return this.myclass == that.myclass && this.ivars.equals(that.ivars);
	}

	@Override
	public int hashCode() {
		int result = 17;
		result = 31 * result + myclass.hashCode();
		result = 31 * result + ivars.hashCode();
		return result;
	}

	@Override
	public String typeName() {
		return "Object";
	}

}
