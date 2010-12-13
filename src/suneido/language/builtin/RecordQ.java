package suneido.language.builtin;

import suneido.SuRecord;
import suneido.language.BuiltinFunction1;

public class RecordQ extends BuiltinFunction1 {

	@Override
	public Object call1(Object a) {
		return a instanceof SuRecord;
	}

}
