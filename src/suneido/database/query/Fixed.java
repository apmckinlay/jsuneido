package suneido.database.query;

import static suneido.util.Util.displayListToParens;

import java.util.*;

import com.google.common.base.Objects;

public class Fixed {
	public String field;
	public List<Object> values;

	public Fixed(String field, Object value) {
		this.field = field;
		values = Collections.singletonList(value);
	}
	public Fixed(String field, List<Object> values) {
		this.field = field;
		this.values = values;
	}

	@Override
	public String toString() {
		return field + "=" + displayListToParens(values);
	}

	/**
	 * fixed1 has precedence e.g. combine(f=1, f=2) => f=1
	 */
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

	@Override
	public boolean equals(Object other) {
		if (this == other)
			return true;
		if (!(other instanceof Fixed))
			return false;
		Fixed f = (Fixed) other;
		return Objects.equal(field, f.field) && 
				Objects.equal(values, f.values);
	}

	@Override
	public int hashCode() {
		throw new UnsupportedOperationException();
	}
}
