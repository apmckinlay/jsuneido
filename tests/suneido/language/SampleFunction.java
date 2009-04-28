package suneido.language;

import java.util.List;

public class SampleFunction extends SuFunction {

	@Override
	public Object call(Object... args) {

		return null;
	}

	void test(List<Object> list) {
		for (Object x : list)
			for (Object y : list)
				call(x, y);
	}

}