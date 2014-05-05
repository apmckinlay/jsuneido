/* Copyright 2012 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido.util;

import java.util.concurrent.TimeUnit;

import suneido.util.Regex.Pattern;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;

public class RegexCache {
	private static LoadingCache<String,Pattern> cache =
			CacheBuilder.newBuilder()
				.maximumSize(100)
				.expireAfterAccess(2, TimeUnit.SECONDS)
				.build(new CacheLoader<String,Pattern>() {
					@Override
					public Pattern load(String rx) {
						return Regex.compile(rx);
					}});

	public static Pattern getPattern(String rx) {
		return cache.getUnchecked(rx);
	}

}
