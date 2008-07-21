package suneido.database.query;

import static suneido.Util.listToParens;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import suneido.SuValue;

public class Fixed {
	public String field;
	public List<SuValue> values;

	public Fixed(String field, SuValue value) {
		this.field = field;
		values = Collections.singletonList(value);
	}

	@Override
	public String toString() {
		return "Fixed " + field + ", " + listToParens(values);
	}

	public static List<Fixed> combine(List<Fixed> fixed1, List<Fixed> fixed2) {
		if (fixed1.isEmpty())
			return fixed2;
		if (fixed2.isEmpty())
			return fixed1;
		List<Fixed> result = new ArrayList<Fixed>(fixed1);
		for (Fixed f2 : fixed2)
			if (!hasField(fixed1, f2.field))
				result.add(f2);
		return result;
	}

	private static boolean hasField(List<Fixed> fixed, String field) {
		for (Fixed f : fixed)
			if (field.equals(f.field))
				return true;
		return false;
	}
}
