package suneido.database.server;

import java.util.Date;

import net.jcip.annotations.GuardedBy;
import net.jcip.annotations.ThreadSafe;

@ThreadSafe
public class Timestamp {

	@GuardedBy("this")
	private static Date prev = new Date();

	public static synchronized Date next() {
		Date ts = new Date();
		if (ts.compareTo(prev) <= 0)
			ts = new Date(prev.getTime() + 1);
		prev = ts;
		return ts;
	}

}
