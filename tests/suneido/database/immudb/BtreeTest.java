/* Copyright 2010 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido.database.immudb;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.*;

import java.util.*;

import org.junit.Test;

import com.google.common.collect.Lists;

public class BtreeTest {
	private final Storage stor = new TestStorage(1024, 64);
	private Random rand = new Random(123456);
	private Tran tran = new Tran(stor);
	private Btree btree = new Btree4(tran);
	private List<Record> keys = Lists.newArrayList();
	private int NKEYS = 100;
	private BtreeInfo info;
	private int redirs;

	private static class Btree4 extends Btree {
		@Override public int maxNodeSize() { return 4; }
		public Btree4(Tran tran) {
			super(tran);
		}
		public Btree4(Tran tran, BtreeInfo info) {
			super(tran, info);
		}
	}

	@Test
	public void empty() {
		assertTrue(btree.isEmpty());
		assertThat(btree.get(record("hello", 1234)), is(0));
	}

	@Test
	public void left_edge() {
		add(NKEYS);
		check();
		// remove all the keys from the leftmost leaf node
		Collections.sort(keys);
		for (int i = 0; i < NKEYS/2; ++i)
			assertTrue(btree.remove(keys.get(i)));
		btree.check();

		for (int i = NKEYS/2; i < NKEYS; ++i) {
			Record key = keys.get(i);
			assertThat("key " + key, btree.get(key), is(adr(key)));
		}
		for (int i = 0; i < NKEYS/2; ++i)
			btree.add(keys.get(i));
		check();

		Record min = record("", 0);
		btree.add(min);
		btree.check();
	}

	@Test
	public void add_in_order() {
		rand = new Random(328457);
		keys = randomKeys(rand, NKEYS);
		Collections.sort(keys);
		for (Record key : keys)
			btree.add(key);
		assertThat(btree.treeLevels(), is(3));
		check();
		assertThat(tran.intrefs.size(), is(btree.nnodes));
		assertTrue(tran.redirs().noneAdded());
	}

	@Test
	public void add_in_reverse_order() {
		rand = new Random(95369);
		keys = randomKeys(rand, NKEYS);
		Collections.sort(keys);
		Collections.reverse(keys);
		for (Record key : keys)
			btree.add(key);
		assertThat(btree.treeLevels(), is(5));
		check();
	}

	@Test
	public void remove_in_order() {
		rand = new Random(7645378);
		add(NKEYS);
		Collections.sort(keys);
		removeAndCheck(NKEYS, rand, keys, btree);
	}

	@Test
	public void remove_in_reverse_order() {
		rand = new Random(156756);
		add(NKEYS);
		Collections.sort(keys);
		Collections.reverse(keys);
		removeAndCheck(NKEYS, rand, keys, btree);
	}

	private void removeAndCheck(int NKEYS, Random rand, List<Record> keys,
			Btree btree) {
		for (int i = 0; i < NKEYS / 2; ++i) {
			assertTrue(btree.remove(keys.get(i)));
			keys.remove(i);
		}
		check();
		for (Record key : keys)
			assertTrue(btree.remove(key));
		assertTrue(btree.isEmpty());
	}

	@Test
	public void add_and_remove() {
		rand = new Random(564367);
		NKEYS = 1000;
		add(NKEYS);

		check();

		for (int i = 0; i < NKEYS / 2; ++i)
			assertTrue(btree.remove(keys.get(i)));

		for (int i = 0; i < NKEYS / 2; ++i) {
			Record key = keys.get(i);
			assertThat(btree.get(key), is(0));
			assertFalse(btree.remove(key));
		}
		Record minKey = record("", 0);
		btree.add(minKey);
		Record maxKey = record("zzzzzzz", Integer.MAX_VALUE);
		btree.add(maxKey);
		assertThat("key " + minKey, btree.get(minKey), is(adr(minKey)));
		assertThat("key " + maxKey, btree.get(maxKey), is(adr(maxKey)));
		for (int i = NKEYS / 2; i < NKEYS; ++i) {
			Record key = keys.get(i);
			assertThat("key " + key, btree.get(key), is(adr(key)));
		}
		assertTrue(btree.remove(minKey));
		assertTrue(btree.remove(maxKey));
		for (int i = NKEYS / 2; i < NKEYS; ++i)
			assertTrue(btree.remove(keys.get(i)));
		assertThat("treeLevels", btree.treeLevels(), is(0));
	}

	@Test
	public void update_and_store() {
		rand = new Random(789456);
		store();

		tran = new Tran(stor);
		btree = new Btree4(tran, info);
		addRemoveAndStore(2);
		assertThat("levels", btree.treeLevels(), is(0));

		tran = new Tran(stor);
		btree = new Btree4(tran, info);
		addRemoveAndStore(3);
		assertThat("levels", btree.treeLevels(), is(1));

		tran = new Tran(stor, redirs);
		btree = new Btree4(tran, info);
		addRemoveAndStore(400);
		assertThat("levels", btree.treeLevels(), is(6));

		tran = new Tran(stor, redirs);
		btree = new Btree4(tran, info);
		check();
		checkIterate();

		store();
	}

	private void addRemoveAndStore(int n) {
//System.out.println("--------------- before update");
//btree.print();
		check();
		add(2 * n);
		remove(n);
//System.out.println("--------------- after update");
//btree.print();
		check();
		store();
//System.out.println("--------------- after store");
//btree.print();
	}

	private void add(int n) {
		for (int i = 0; i < n; ++i) {
			Record key = randomKey(rand);
			assertTrue(btree.add(key, true));
			keys.add(key);
		}
	}

	private void remove(int n) {
		for (int i = 0; i < n; ++i) {
			int r = rand.nextInt(keys.size());
			assertTrue(btree.remove(keys.get(r)));
			keys.remove(r);
		}
	}

	private void check() {
		btree.check();
		Collections.shuffle(keys, rand);
		for (Record key : keys)
			assertThat("key " + key, btree.get(key), is(adr(key)));
	}

	private void store() {
		tran.startStore();
		Btree.store(tran);
		info = btree.info();
		redirs = tran.storeRedirs();
		tran = null;
	}

	@Test
	public void iterate_empty() {
		Btree.Iter iter = btree.iterator();
		assertTrue(iter.eof());
		iter.next();
		assertTrue(iter.eof());

		iter = btree.iterator();
		assertTrue(iter.eof());
		iter.prev();
		assertTrue(iter.eof());
	}

	@Test
	public void iterate() {
		rand = new Random(1291681);
		add(NKEYS);
		checkIterate();

		int i = keys.size();
		Btree.Iter iter = btree.iterator();
		for (iter.prev(); ! iter.eof(); iter.prev())
			assertThat(iter.cur(), is(keys.get(--i)));
		assertThat(i, is(0));
		iter.prev();
		assertTrue(iter.eof());
	}

	public Btree.Iter checkIterate() {
		Collections.sort(keys);

		int i = 0;
		Btree.Iter iter = btree.iterator();
		for (iter.next(); ! iter.eof(); iter.next())
			assertThat("i " + i, iter.cur(), is(keys.get(i++)));
		assertThat(i, is(keys.size()));
		return iter;
	}

	@Test
	public void iterate_delete_behind() {
		rand = new Random(546453);
		add(NKEYS);
		Collections.sort(keys);

		int i = 0;
		Btree.Iter iter = btree.iterator();
		for (iter.next(); ! iter.eof(); iter.next()) {
			assertThat(iter.cur(), is(keys.get(i)));
			if (rand.nextInt(5) == 3)
				btree.remove(keys.get(rand.nextInt(i)));
			++i;
		}
		assertThat(i, is(keys.size()));
	}

	@Test
	public void iterate_delete_ahead() {
		rand = new Random(876564);
		add(NKEYS);
		Collections.sort(keys);

		int i = 0;
		Btree.Iter iter = btree.iterator();
		for (iter.next(); ! iter.eof(); iter.next()) {
			assertThat(iter.cur(), is(keys.get(i++)));
			if (i < keys.size() && rand.nextInt(5) == 3) {
				int at = i + rand.nextInt(keys.size() - i);
				btree.remove(keys.get(at));
				keys.remove(at);
			}
		}
		assertThat(i, is(keys.size()));
	}

	@Test
	public void iterate_delete_all() {
		rand = new Random(89876);
		add(NKEYS);
		Collections.sort(keys);

		int i = 0;
		Btree.Iter iter = btree.iterator();
		for (iter.next(); ! iter.eof(); iter.next()) {
			assertThat(iter.cur(), is(keys.get(i)));
			assertTrue(btree.remove(keys.get(i)));
			++i;
		}
		assertThat(i, is(keys.size()));
	}

	@Test
	public void unique() {
		rand = new Random(1291681);
		add(NKEYS);
		for (Record key : keys)
			assertFalse(btree.add(key, true));
	}

	@Test
	public void update() {
		rand = new Random(456978);
		add(NKEYS);
		Collections.shuffle(keys, rand);
		update(200);
		check();
		checkIterate();

		store();
		tran = new Tran(stor);
		btree = new Btree4(tran, info);
		update(200);
		check();
		checkIterate();
	}

	public void update(int n) {
		for (int i = 0; i < n; ++i) {
			int k = rand.nextInt(NKEYS);
			Record oldkey = keys.get(k);
			Record newkey = updateKey(oldkey);
			keys.set(k, newkey);
			btree.update(oldkey, newkey, true);
		}
	}

	private Record updateKey(Record oldkey) {
		if ((rand.nextInt() % 2) == 0)
			return randomKey(rand);
		else {
			// new address must be larger than old one
			return new RecordBuilder()
				.add(oldkey, 0)
				.add(Btree.getAddress(oldkey) + 1)
				.build();
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
		Record rec = record("a data record");
		int intref = tran.refToInt(rec);
		Record key = record("hello", intref);
		btree.add(key);
		tran.startStore();
		DataRecords.store(tran);
		int adr = tran.getAdr(intref);
		assert adr != 0;
		Btree.store(tran);
		info = btree.info();

		tran = new Tran(stor);
		btree = new Btree4(tran, info);
		assertThat(btree.get(record("hello")), is(adr));
	}

	private static int adr(Record key) {
		return Btree.getAddress(key);
	}

	public static List<Record> randomKeys(Random rand, int n) {
		List<Record> keys = new ArrayList<Record>();
		for (int i = 0; i < n; ++i)
			keys.add(randomKey(rand));
		return keys;
	}

	public static Record randomKey(Random rand) {
		int n = 3 + rand.nextInt(5);
		String s = "";
		for (int i = 0; i < n; ++i)
			s += (char) ('a' + rand.nextInt(26));
		final int UPDATE_ALLOWANCE = 10000;
		return record(s, rand.nextInt(Integer.MAX_VALUE - UPDATE_ALLOWANCE));
	}

	static Record record(String s) {
		return new RecordBuilder().add(s).build();
	}

	private static Record record(String s, int n) {
		return new RecordBuilder().add(s).add(n).build();
	}

}
