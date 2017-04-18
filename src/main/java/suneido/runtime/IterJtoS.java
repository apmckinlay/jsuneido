/* Copyright 2017 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido.runtime;

import java.util.ConcurrentModificationException;
import java.util.Iterator;

import suneido.SuException;
import suneido.SuValue;

/** Adapts a Java iterable to be a Suneido iterator */
public class IterJtoS extends SuValue {
	private final Iterable<Object> c; // needed for Dup
	private final Iterator<Object> iter;

	public IterJtoS(Iterable<Object> c) {
		this.c = c;
		iter = c.iterator();
	}

	@Override
	public String typeName() {
		return "ObjectIter";
	}

	@Override
	public SuValue lookup(String method) {
		return IterateMethods.singleton.lookup(method);
	}

	public static final class IterateMethods extends BuiltinMethods {
		public static final SuValue singleton = new IterateMethods();

		private IterateMethods() {
			super("objectiter", IterateMethods.class, null);
		}

		public static Object Next(Object self) {
			IterJtoS it = (IterJtoS) self;
			try {
				return it.iter.hasNext() ? it.iter.next() : self;
			} catch (ConcurrentModificationException e) {
				throw new SuException("object modified during iteration");
			}
		}

		public static Object Dup(Object self) {
			IterJtoS it = (IterJtoS) self;
			return new IterJtoS(it.c);
		}
	}

}