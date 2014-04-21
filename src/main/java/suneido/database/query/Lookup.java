/* Copyright 2010 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido.database.query;

import java.util.HashMap;
import java.util.Map;

import suneido.intfc.database.Record;

class Lookup {
	Map<Record,Object[]> map = new HashMap<>();

	void put(Record key, Object[] data) {
		map.put(key, data);
	}

	Object[] get(Record key) {
		return map.get(key);
	}
}