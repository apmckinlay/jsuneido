package suneido.language;

import java.util.List;

import suneido.SuException;

public class SampleFunction extends SuFunction {

	@Override
	public Object call(Object... args) {

		return null;
	}

	void test(List<Object> list) {
		throw new SuException("block:continue");
	}

}