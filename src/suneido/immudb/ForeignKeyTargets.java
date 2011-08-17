/* Copyright 2011 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido.immudb;

import java.util.Map;
import java.util.Set;

import javax.annotation.concurrent.Immutable;

import suneido.util.PersistentMap;

import com.google.common.base.Predicates;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterables;
import com.google.common.collect.Maps;

@Immutable
class ForeignKeyTargets {
	private final PersistentMap<ForeignKey, ImmutableSet<ForeignKeyTarget>> targets;

	private ForeignKeyTargets(
			PersistentMap<ForeignKey, ImmutableSet<ForeignKeyTarget>> targets) {
		this.targets = targets;
	}

	static ForeignKeyTargets empty() {
		PersistentMap<ForeignKey, ImmutableSet<ForeignKeyTarget>> targets =
				PersistentMap.empty();
		return new ForeignKeyTargets(targets);
	}

	ForeignKeyTargets with(ForeignKey source, ForeignKeyTarget target) {
		ImmutableSet.Builder<ForeignKeyTarget> fks = ImmutableSet.builder();
		Set<ForeignKeyTarget> cur = targets.get(source);
		if (cur != null)
			fks.addAll(cur);
		fks.add(target);
		return new ForeignKeyTargets(targets.with(source, fks.build()));
	}

	ForeignKeyTargets without(ForeignKey source, ForeignKeyTarget target) {
		Set<ForeignKeyTarget> cur = targets.get(source);
		ImmutableSet<ForeignKeyTarget> wo = ImmutableSet.copyOf(
				Iterables.filter(cur, Predicates.not(Predicates.equalTo(target))));
		return new ForeignKeyTargets(targets.with(source, wo));
	}

	Set<ForeignKeyTarget> get(String tablename, String columns) {
		return targets.get(new ForeignKey(tablename, columns));
	}

	static suneido.immudb.ForeignKeyTargets.Builder builder() {
		return new Builder();
	}

	static class Builder {
		private final Map<ForeignKey, ImmutableSet.Builder<ForeignKeyTarget>> targets =
				Maps.newHashMap();

		void add(ForeignKey source, ForeignKeyTarget target) {
			assert source != null;
			assert target != null;
			ImmutableSet.Builder<ForeignKeyTarget> fkeys = targets.get(source);
			if (fkeys == null)
				fkeys = ImmutableSet.builder();
			fkeys.add(target);
			targets.put(source, fkeys);
		}

		ForeignKeyTargets build() {
			PersistentMap.Builder <ForeignKey, ImmutableSet<ForeignKeyTarget>> b =
					PersistentMap.builder();
			for (Map.Entry<ForeignKey, ImmutableSet.Builder<ForeignKeyTarget>> e : targets.entrySet())
				b.put(e.getKey(), e.getValue().build());
			return new ForeignKeyTargets(b.build());
		}

	}

}
