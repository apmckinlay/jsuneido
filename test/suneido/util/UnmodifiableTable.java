/* Copyright 2011 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido.util;

import com.google.common.collect.ForwardingTable;

public class UnmodifiableTable<R,C,V> extends ForwardingTable<R,C,V> {
	com.google.common.collect.Table<R,C,V> table;

	public UnmodifiableTable(com.google.common.collect.Table<R,C,V> table) {
		this.table = table;
	}

	@Override
	protected com.google.common.collect.Table<R,C,V> delegate() {
		return table;
	}

	@Override
	public V put(R rowKey, C columnKey, V value) {
		throw new UnsupportedOperationException();
	}

	@Override
	public void clear() {
		throw new UnsupportedOperationException();
	}

	@Override
	public V remove(Object rowKey, Object columnKey) {
		throw new UnsupportedOperationException();
	}

}

