/* Copyright 2008 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido.database.query;

import static suneido.util.Verify.verify;
import gnu.trove.map.TObjectDoubleMap;
import gnu.trove.map.hash.TObjectDoubleHashMap;

import java.util.List;
import java.util.Set;

import com.google.common.base.Objects;

public class Cache {
	TObjectDoubleMap<CacheKey> entries = 
		new TObjectDoubleHashMap<>(5, .5f, -1.0);

	public void add(List<String> index, Set<String> needs, Set<String> firstneeds,
			boolean is_cursor, double cost) {
		verify(cost >= 0);
		CacheKey key = new CacheKey(index, needs, firstneeds, is_cursor);
		entries.put(key, cost);
	}

	public double get(List<String> index, Set<String> needs, Set<String> firstneeds,
			boolean is_cursor) {
		CacheKey key = new CacheKey(index, needs, firstneeds, is_cursor);
		return entries.get(key);
	}
	
	private static class CacheKey {
		final List<String> index;
		final Set<String> needs;
		final Set<String> firstneeds;
		final boolean is_cursor;
		
		CacheKey(List<String> index, Set<String> needs,	Set<String> firstneeds, 
				boolean is_cursor) {
			this.index = index;
			this.needs = needs;
			this.firstneeds = firstneeds;
			this.is_cursor = is_cursor;
		}
		
		@Override
		public int hashCode() {
			return Objects.hashCode(index, needs, firstneeds, is_cursor);
		}
		
		@Override
		public boolean equals(Object other) {
			if (this == other)
				return true;
			if (! (other instanceof CacheKey))
				return false;
			CacheKey that = (CacheKey) other;
			return index.equals(that.index) && needs.equals(that.needs) &&
					firstneeds.equals(that.firstneeds) && is_cursor == that.is_cursor;
		}
		
	}
}
