package suneido.language.builtin;

import suneido.SuRecord;
import suneido.language.SuFunction1;

public class RecordQ extends SuFunction1 {

	@Override
	public Object call1(Object a) {
		return a instanceof SuRecord;
	}

}
