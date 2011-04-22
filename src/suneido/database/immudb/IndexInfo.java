/* Copyright 2011 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido.database.immudb;

public class IndexInfo extends BtreeInfo {
	public static final int NFIELDS = 4;
	public final String columns;

	public IndexInfo(String columns, BtreeInfo info) {
		super(info.root, info.treeLevels, info.nnodes);
		this.columns = columns;
	}

	public IndexInfo(Record rec, int i) {
		super(rec.getInt(i + 1), rec.getInt(i + 2), rec.getInt(i + 3));
		this.columns = rec.getString(i);
	}

	public void addToRecord(RecordBuilder rb) {
		rb.add(columns).add(root).add(nnodes).add(treeLevels);
	}

	public static void addToRecord(RecordBuilder rb, String columns, BtreeInfo info) {
		rb.add(columns).add(info.root).add(info.treeLevels).add(info.nnodes);
	}

}
