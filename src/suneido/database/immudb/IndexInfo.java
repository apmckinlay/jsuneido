/* Copyright 2011 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido.database.immudb;

import com.google.common.base.Objects;

public class IndexInfo extends BtreeInfo {
	public static final int NFIELDS = 4;
	public final String columns; // e.g. "0,1"

	public IndexInfo(String columns, BtreeInfo info) {
		super(info.root, info.treeLevels, info.nnodes);
		this.columns = columns;
	}

	public IndexInfo(String columns, int root, int treeLevels, int nnodes) {
		super(root, treeLevels, nnodes);
		this.columns = columns;
	}

	public IndexInfo(Record rec, int i) {
		super(rec.getInt(i + 1), rec.getInt(i + 2), rec.getInt(i + 3));
		this.columns = rec.getString(i);
	}

	public void addToRecord(RecordBuilder rb) {
		rb.add(columns).add(root).add(treeLevels).add(nnodes);
	}

	public static void addToRecord(RecordBuilder rb, String columns, BtreeInfo info) {
		rb.add(columns).add(info.root).add(info.treeLevels).add(info.nnodes);
	}

	@Override
	public String toString() {
		return Objects.toStringHelper(this)
			.add("columns", columns)
			.add("root", root)
			.add("treeLevels", treeLevels)
			.add("nnodes", nnodes)
			.toString();
	}

	public void check() {
		assert ! IntRefs.isIntRef(root);
	}

}
