package suneido.database.query;

import static suneido.Suneido.verify;

import java.util.ArrayList;
import java.util.List;

public class QueryCache {
	List<CacheEntry> entries = new ArrayList<CacheEntry>();

	public void add(List<String> index, List<String> needs, List<String> firstneeds, double cost) {
		verify(cost >= 0);
		entries.add(new CacheEntry(index, needs, firstneeds, cost));
		}

	public double get(List<String> index, List<String> needs, List<String> firstneeds)
		{
		for (CacheEntry c : entries)
			if (c.index == index && c.needs == needs && c.firstneeds == firstneeds)
				return c.cost;
		return -1;
		}

	private static class CacheEntry {
		CacheEntry(List<String> index, List<String> needs,
				List<String> firstneeds, double cost) {
			this.index = index;
			this.needs = needs;
			this.firstneeds = firstneeds;
			this.cost = cost;
		}
		List<String> index;
		List<String> needs;
		List<String> firstneeds;
		double cost;
	}
}
