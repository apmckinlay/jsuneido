package suneido.util;

import java.util.Comparator;

public class IdentityComparator implements Comparator<Object> {

	@Override
	public int compare(Object o1, Object o2) {
		return System.identityHashCode(o1) - System.identityHashCode(o2);
	}

}
