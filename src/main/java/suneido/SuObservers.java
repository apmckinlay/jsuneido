/* Copyright 2012 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido;

import java.util.Iterator;
import java.util.List;
import java.util.Set;

import suneido.language.Args;
import suneido.language.SuBoundMethod;

import com.google.common.base.Objects;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;

/**
 * Used by {@link SuRecordNew} to implements observers.
 * Layered on top of {@link SuRules}
 * which is layered on top of {@link SuContainer}.
 */
public class SuObservers extends SuRules {
	private final List<Object> observers = Lists.newArrayList();
	private final Set<Object> invalidated = Sets.newLinkedHashSet();
	List<ActiveObserver> activeObservers = Lists.newArrayList();

	public SuObservers() {
	}

	public SuObservers(SuObservers r) {
		super(r);
	}

	public void addObserver(Object observer) {
		observers.add(observer);
	}

	public void removeObserver(Object observer) {
		observers.remove(observer);
	}

	@Override
	public void invalidate(Object field) {
		super.invalidate(field);
		callObservers(field);
	}

	// called by SuRules
	@Override
	protected void invalidated(Object field) {
		invalidated.add(field);
	}

	@Override
	public void put(Object field, Object value) {
		super.put(field, value);
		callObservers(field);
	}

	private static class ActiveObserver {
		public Object observer;
		public Object field;

		public ActiveObserver(Object observer, Object member) {
			this.observer = observer;
			this.field = member;
		}

		@Override
		public boolean equals(Object other) {
			if (this == other)
				return true;
			if (! (other instanceof ActiveObserver))
				return false;
			ActiveObserver that = (ActiveObserver) other;
			return Objects.equal(observer, that.observer) &&
					Objects.equal(field, that.field);
		}

		@Override
		public int hashCode() {
			throw new UnsupportedOperationException();
		}
	}

	public void callObservers(Object member) {
		callObservers2(member);
		invalidated.remove(member);
		while (! invalidated.isEmpty()) {
			Iterator<Object> iter = invalidated.iterator();
			Object m = iter.next();
			iter.remove();
			callObservers2(m);
		}
	}

	private void callObservers2(Object member) {
		for (Object observer : observers) {
			ActiveObserver ao = new ActiveObserver(observer, member);
			if (activeObservers.contains(ao))
				continue;
			activeObservers.add(ao);
			try {
				if (observer instanceof SuBoundMethod)
					((SuBoundMethod) observer).call(Args.Special.NAMED, "member",
							member);
				else if (observer instanceof SuValue)
					((SuValue) observer).eval(this, Args.Special.NAMED,
							"member", member);
				else
					throw new SuException("invalid observer");
			} finally {
				activeObservers.remove(ao);
			}
		}
	}

}
