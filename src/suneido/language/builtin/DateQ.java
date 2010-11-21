package suneido.language.builtin;

import java.util.Date;

import suneido.language.BuiltinFunction1;

public class DateQ extends BuiltinFunction1 {

	@Override
	public Object call1(Object a) {
		return a instanceof Date;
	}

}
