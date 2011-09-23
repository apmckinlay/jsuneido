package suneido.database.query;

import static suneido.util.Verify.verify;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import suneido.Suneido;

public class Cache {
	List<CacheEntry> entries = new ArrayList<CacheEntry>();

	public void add(List<String> index, Set<String> needs, Set<String> firstneeds,
			boolean is_cursor, double cost) {
		verify(cost >= 0);
		entries.add(new CacheEntry(index, needs, firstneeds, is_cursor, cost));
		if (entries.size() > 20)
			Suneido.errlog("large query cache " + entries.size());
		}

	public double get(List<String> index, Set<String> needs, Set<String> firstneeds,
			boolean is_cursor)
		{
		for (CacheEntry c : entries)
			if (index.equals(c.index) && needs.equals(c.needs) &&
					firstneeds.equals(c.firstneeds) && is_cursor == c.is_cursor)
				return c.cost;
		return -1;
		}

	private static class CacheEntry {
		CacheEntry(List<String> index, Set<String> needs,
				Set<String> firstneeds, boolean is_cursor, double cost) {
			this.index = index;
			this.needs = needs;
			this.firstneeds = firstneeds;
			this.is_cursor = is_cursor;
			this.cost = cost;
		}
		List<String> index;
		Set<String> needs;
		Set<String> firstneeds;
		boolean is_cursor;
		double cost;
	}
}
