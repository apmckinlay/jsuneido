package suneido.database.server;

import java.util.Date;

import javax.annotation.concurrent.GuardedBy;
import javax.annotation.concurrent.ThreadSafe;

@ThreadSafe
public class Timestamp {

	@GuardedBy("this")
	private static Date prev = new Date();

	public synchronized static Date next() {
		Date ts = new Date();
		if (ts.compareTo(prev) <= 0)
			ts = new Date(prev.getTime() + 1);
		prev = ts;
		return ts;
	}

}
