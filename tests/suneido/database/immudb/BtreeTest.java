/* Copyright 2010 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido.database.immudb;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

import java.util.*;

import org.junit.Test;

public class BtreeTest {
	private final Storage stor = new TestStorage();
	private Tran tran = new Tran(stor);
	private int root;
	private int levels;
	private int redirs;

	private static class Btree4 extends Btree {
		@Override public int maxNodeSize() { return 4; }
		public Btree4(Tran tran) {
			super(tran);
		}
		public Btree4(Tran tran, int root, int treeLevels) {
			super(tran, root, treeLevels);
		}
	}

	@Test
	public void empty() {
		Btree btree = new Btree4(tran);
		assertThat(btree.get(record("hello", 1234)), is(0));
	}

	@Test
	public void main() {
		List<Record> keys = new ArrayList<Record>();
		int NKEYS = 100;
		Random rand = new Random(1234);
		for (int i = 0; i < NKEYS; ++i)
			keys.add(randomKey(rand));

		Btree btree = new Btree4(tran);
		for (Record key : keys)
			btree.add(key);

		Collections.shuffle(keys, rand);
		for (Record key : keys)
			assertThat(btree.get(key), is(adr(key)));
	}

	@Test
	public void store() {
		List<Record> keys = new ArrayList<Record>();
		Random rand = new Random(90873);

		Btree btree = new Btree4(tran);
		store(btree);

		tran = new Tran(stor);
		btree = new Btree4(tran, root, levels);
		addAndStore(3, rand, keys, btree);
		assertThat("levels", btree.treeLevels(), is(0));

		tran = new Tran(stor);
		btree = new Btree4(tran, root, levels);
		addAndStore(3, rand, keys, btree);
		assertThat("levels", btree.treeLevels(), is(1));

		tran = new Tran(stor, new Redirects(DbHashTree.from(stor, redirs)));
		btree = new Btree4(tran, root, levels);
		addAndStore(120, rand, keys, btree);
		assertThat("levels", btree.treeLevels(), is(4));

		tran = new Tran(stor, new Redirects(DbHashTree.from(stor, redirs)));
		btree = new Btree4(tran, root, levels);
		check(keys, rand, btree);
	}

	private void check(List<Record> keys, Random rand, Btree btree) {
		Collections.shuffle(keys, rand);
		for (Record key : keys)
			assertThat("key " + key, btree.get(key), is(adr(key)));
	}

	private void store(Btree btree) {
		tran.startStore();
		btree.store();
		root = btree.root();
		levels = btree.treeLevels();
		redirs = tran.storeRedirs();
		tran = null;
	}

	private void addAndStore(int n, Random rand, List<Record> keys, Btree btree) {
//System.out.println("--------------- before add");
//btree.print();
		check(keys, rand, btree);
		add(n, rand, keys, btree);
//System.out.println("--------------- after add");
//btree.print();
		check(keys, rand, btree);
		store(btree);
//System.out.println("--------------- after store");
//btree.print();
	}

	private void add(int n, Random rand, List<Record> keys, Btree btree) {
		for (int i = 0; i < n; ++i) {
			Record key = randomKey(rand);
			btree.add(key);
			keys.add(key);
		}
	}

	@Test
	public void intref_adr_should_be_greater_than_db_offset() {
		Record intref = record("hello", 123 | IntRefs.MASK);
		Record offset = record("hello", 567);
		assert intref.compareTo(offset) > 0;
		assert offset.compareTo(intref) < 0;
	}

	@Test
	public void translate_data_refs() {
		Btree btree = new Btree4(tran);
		Record rec = record("a data record");
		int intref = tran.refToInt(rec);
		Record key = record("hello", intref);
		btree.add(key);
		tran.startStore();
		DataRecords.store(tran);
		int adr = tran.getAdr(intref);
		assert adr != 0;
		btree.store();
		int root = btree.root();
		int levels = btree.treeLevels();

		tran = new Tran(stor);
		btree = new Btree4(tran, root, levels);
		assertThat(btree.get(record("hello")), is(adr));
	}

	private int adr(Record key) {
		return Btree.getAddress(key);
	}

	public Record randomKey(Random rand) {
		int n = 1 + rand.nextInt(5);
		String s = "";
		for (int i = 0; i < n; ++i)
			s += (char) ('a' + rand.nextInt(26));
		return record(s, rand.nextInt(Integer.MAX_VALUE));
	}

	private Record record(String s) {
		return new MemRecord().add(s);
	}

	private Record record(String s, int n) {
		return new MemRecord().add(s).add(n);
	}

}
