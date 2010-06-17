package suneido.database.query;

import static suneido.SuException.verify;

import java.util.*;

public class Cache {
	List<CacheEntry> entries = new ArrayList<CacheEntry>();

	public void add(List<String> index, Set<String> needs, Set<String> firstneeds, double cost) {
		verify(cost >= 0);
		entries.add(new CacheEntry(index, needs, firstneeds, cost));
		}

	public double get(List<String> index, Set<String> needs, Set<String> firstneeds)
		{
		for (CacheEntry c : entries)
			if (index.equals(c.index) && needs.equals(c.needs) && firstneeds.equals(c.firstneeds))
				return c.cost;
		return -1;
		}

	private static class CacheEntry {
		CacheEntry(List<String> index, Set<String> needs,
				Set<String> firstneeds, double cost) {
			this.index = index;
			this.needs = needs;
			this.firstneeds = firstneeds;
			this.cost = cost;
		}
		List<String> index;
		Set<String> needs;
		Set<String> firstneeds;
		double cost;
	}
}
