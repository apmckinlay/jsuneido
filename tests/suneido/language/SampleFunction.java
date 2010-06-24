package suneido.language;

import java.util.List;

import suneido.SuException;

public class SampleFunction extends SuFunction {

	@Override
	public Object call(Object self, Object... args) {
//		args = Args.massage(FunctionSpec.noParams, args);
//		Object[] constants = null;
		try {
			try {
				System.out.println();
			} catch (Throwable e) {
				return e;
			}
		} catch (IllegalAccessError e) {
		}
		return "y";
	}

	void test(List<Object> list) {
		throw new SuException("block:continue");
	}

}