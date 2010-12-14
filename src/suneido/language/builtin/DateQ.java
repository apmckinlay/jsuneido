package suneido.language.builtin;

import java.util.Date;

import suneido.language.SuFunction1;

public class DateQ extends SuFunction1 {

	@Override
	public Object call1(Object a) {
		return a instanceof Date;
	}

}
