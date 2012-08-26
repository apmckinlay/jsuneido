/* Copyright 2012 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido.immudb;

import static org.hamcrest.Matchers.closeTo;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

import org.junit.Test;

import suneido.immudb.TranIndex.Update;

import com.google.common.collect.Lists;

public class BtreeTest {
	private final Storage stor = new MemStorage(1024, 64);
	private Random rand = new Random(123456);
	private final Tran tran = new Tran(stor, null);
	private Btree btree = new Btree4(tran);
	private List<BtreeKey> keys = Lists.newArrayList();
	private int NKEYS = 100;

	private static class Btree4 extends Btree {
		@Override public int splitSize() { return 4; }
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
		assertThat(btree.get(key("hello", 123).key), is(0));
	}

	@Test
	public void add1() {
		btree.add(key("hello", 123));
		assertThat(btree.get(key("hello", 123).key), is(123));
	}

	@Test
	public void first_leaf_split_end() {
		add("a", 1);
		add("b", 2);
		add("c", 3);
		add("d", 4);
		add("e", 5);
		btree.check();
	}

	@Test
	public void first_leaf_split_left() {
		add("a", 1);
		add("c", 3);
		add("d", 4);
		add("e", 5);
		add("b", 2);
		btree.check();
	}

	@Test
	public void first_leaf_split_right() {
		add("a", 1);
		add("b", 2);
		add("c", 3);
		add("e", 5);
		add("d", 4);
		btree.check();
	}

	@Test
	public void first_tree_insert() {
		add("a", 1);
		add("b", 2);
		add("c", 3);
		add("d", 4);
		add("a1", 5); // first split, new root
		add("a2", 5);
		add("a3", 5); // second split, insert into root
		btree.check();
	}

	@Test
	public void first_tree_insert_end() {
		add("a", 1);
		add("b", 2);
		add("c", 3);
		add("d", 4);
		add("e", 5);
		add("f", 5);
		add("g", 5);
		add("h", 5);
		add("i", 5);
		btree.check();
	}

	private void add(Object... values) {
		btree.add(new BtreeKey(rec(values)));
		btree.freeze();
		btree.check();
	}

	@Test
	public void first_tree_split() {
		btree.add(key("a"));
		btree.add(key("b"));
		btree.add(key("c"));
		btree.add(key("d"));
		btree.add(key("e"));
		btree.add(key("f"));
		btree.add(key("g"));
		btree.add(key("h"));
		btree.add(key("i"));
		btree.add(key("j"));
		btree.add(key("k"));
		btree.add(key("l"));
		btree.add(key("m"));
		btree.add(key("n"));
		btree.add(key("o"));
		btree.add(key("p"));
		btree.add(key("q"));
		btree.check();
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
			BtreeKey key = keys.get(i);
			assertThat("key " + key, btree.get(key.key), is(key.adr()));
		}
		for (int i = 0; i < NKEYS/2; ++i)
			btree.add(keys.get(i));
		check();

		btree.add(key("", 0));
		btree.check();
	}

	@Test
	public void add_in_order() {
		rand = new Random(328457);
		keys = randomKeys(rand, NKEYS);
		Collections.sort(keys);
		for (BtreeKey key : keys)
			btree.add(key);
		assertThat(btree.treeLevels(), is(3));
		check();
	}

	@Test
	public void add_in_reverse_order() {
		rand = new Random(95367);
		keys = randomKeys(rand, NKEYS);
		Collections.sort(keys);
		Collections.reverse(keys);
		for (BtreeKey key : keys)
			btree.add(key);
		assertThat(btree.treeLevels(), is(5));
		check();
	}

	@Test
	public void add_remove_one() {
		btree.add(key("foo"));
		assertFalse(btree.remove(key("bar")));
		assertTrue(btree.remove(key("foo")));
		assertTrue(btree.isEmpty());
	}

	@Test
	public void add_remove_treeLevel() {
		add(15);
		assertFalse(btree.remove(key("dkfjsdkfjds")));
		for (BtreeKey key : keys) {
			assertTrue(btree.remove(key));
		}
		assertTrue(btree.isEmpty());
		for (BtreeKey key : keys)
			assertFalse(btree.remove(key));
		for (BtreeKey key : keys)
			assertThat(btree.get(key.key), is(0));
		assertTrue(btree.isEmpty());
	}

	@Test
	public void remove_in_order() {
		rand = new Random(7645378);
		add(NKEYS);
		Collections.sort(keys);
		removeAndCheck();
	}

	@Test
	public void remove_in_reverse_order() {
		rand = new Random(156756);
		add(NKEYS);
		Collections.sort(keys);
		Collections.reverse(keys);
		removeAndCheck();
	}

	private void removeAndCheck() {
		for (int i = NKEYS - 1; i > NKEYS / 2; --i) {
			assertTrue(btree.remove(keys.get(i)));
			keys.remove(i);
		}
		check();
		for (BtreeKey key : keys)
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
			BtreeKey key = keys.get(i);
			assertThat(btree.get(key.key), is(0));
			assertFalse(btree.remove(key));
		}
		BtreeKey minKey = key("", 0);
		btree.add(minKey);
		BtreeKey maxKey = key("zzzzzzz", Integer.MAX_VALUE);
		btree.add(maxKey);
		assertThat("key " + minKey, btree.get(minKey.key), is(minKey.adr()));
		assertThat("key " + maxKey, btree.get(maxKey.key), is(maxKey.adr()));
		for (int i = NKEYS / 2; i < NKEYS; ++i) {
			BtreeKey key = keys.get(i);
			assertThat("key " + key, btree.get(key.key), is(key.adr()));
		}
		assertTrue(btree.remove(minKey));
		assertTrue(btree.remove(maxKey));
		for (int i = NKEYS / 2; i < NKEYS; ++i)
			assertTrue(btree.remove(keys.get(i)));
		assertTrue(btree.isEmpty());
	}

	private void add(int n) {
		for (int i = 0; i < n; ++i) {
			BtreeKey key = randomKey(rand);
			btree.add(key);
			keys.add(key);
			if (i % 3 == 0)
				btree.freeze();
		}
	}

	private void check() {
		btree.check();
		Collections.shuffle(keys, rand);
		for (BtreeKey key : keys)
			assertThat("key " + key, btree.get(key), is(key.adr()));
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
			assertThat("i " + i, iter.curKey(), is(keys.get(--i).key));
		assertThat(i, is(0));
		iter.prev();
		assertTrue(iter.eof());
	}

	@Test
	public void iters_stick_at_eof() {
		add(3);
		Btree.Iter iter = btree.iterator();
		for (iter.next(); ! iter.eof(); iter.next())
			;
		assertTrue(iter.eof());
		iter.next();
		assertTrue(iter.eof());
		iter.prev();
		assertTrue(iter.eof());
		Btree.Iter iter2 = btree.iterator(iter);
		assertTrue(iter2.eof());
		iter.next();
		assertTrue(iter2.eof());
	}

	public Btree.Iter checkIterate() {
		Collections.sort(keys);

		int i = 0;
		Btree.Iter iter = btree.iterator();
		for (iter.next(); ! iter.eof(); iter.next())
			assertThat("i " + i, iter.curKey(), is(keys.get(i++).key));
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
			assertThat("i " + i, iter.curKey(), is(keys.get(i).key));
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
			assertThat(iter.curKey(), is(keys.get(i++).key));
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
			assertThat("i " + i, iter.curKey(), is(keys.get(i).key));
			assertTrue(btree.remove(keys.get(i)));
			++i;
		}
		assertThat(i, is(keys.size()));
	}

	@Test
	public void iter_remove_bug() {
		btree.add(key("test0"));
		btree.add(key("test1"));
		btree.add(key("test2"));
		btree.add(key("test3"));
		btree.add(key("test4"));
		btree.add(key("test5"));
		Btree.Iter iter = btree.iterator(rec("test2"), rec("test4"));
		iter.next();
		assertThat(iter.curKey().getString(0), is("test2"));
		assertTrue(btree.remove(iter.cur()));
		iter.next();
		assertThat(iter.curKey().getString(0), is("test3"));
		assertTrue(btree.remove(iter.cur()));
		iter.next();
		assertThat(iter.curKey().getString(0), is("test4")); // in next node
		assertTrue(btree.remove(iter.cur()));
		iter.next();
		assertTrue(iter.eof());
	}

	@Test
	public void iter_remove_reverse_bug() {
		btree.add(key("test0"));
		btree.add(key("test1"));
		btree.add(key("test2"));
		btree.add(key("test3"));
		btree.add(key("test4"));
		btree.add(key("test5"));
		Btree.Iter iter = btree.iterator(rec("test2"), rec("test4"));
		iter.prev();
		assertThat(iter.curKey().getString(0), is("test4"));
		assertTrue(btree.remove(iter.cur()));
		iter.prev();
		assertThat(iter.curKey().getString(0), is("test3"));
		assertTrue(btree.remove(iter.cur()));
		iter.prev();
		assertThat(iter.curKey().getString(0), is("test2")); // in next node
		assertTrue(btree.remove(iter.cur()));
		iter.prev();
		assertTrue(iter.eof());
	}

	@Test
	public void iter_update_bug() {
		btree.add(key("test0"));
		btree.add(key("test1"));
		btree.add(key("test2"));
		btree.add(key("test3"));
		btree.add(key("test4"));
		btree.add(key("test5"));
		Btree.Iter iter = btree.iterator();
		iter = next(iter);
		assertThat("i 0", iter.curKey().getString(0), is("test0"));
		iter = next(iter);
		assertThat("i 1", iter.curKey().getString(0), is("test1"));
		assert btree.remove(key("test1"));
		assert btree.add(key("test1", btree.tran.refToInt(new Object())), true);
		iter = next(iter);
		assertThat("i 2", iter.curKey().getString(0), is("test2"));
	}
	// simulate cursor using new transactions
	private Btree.Iter next(Btree.Iter iter) {
		btree = new Btree4(new Tran(stor, null), btree.info());
		iter = btree.iterator(iter);
		iter.next();
		return iter;
	}

	@Test
	public void iter_delete_bug() {
		btree.add(key("test0"));
		btree.add(key("test1"));
		btree.add(key("test2"));
		btree.add(key("test3"));
		btree.add(key("test4"));
		btree.add(key("test5"));
		Btree.Iter iter = btree.iterator();
		iter = next(iter);
		assertThat("i 0", iter.curKey().getString(0), is("test0"));
		iter = next(iter);
		assertThat("i 1", iter.curKey().getString(0), is("test1"));
		assert btree.remove(key("test1"));
		iter = next(iter);
		assertThat("i 2", iter.curKey().getString(0), is("test2"));
	}

	@Test
	public void unique() {
		rand = new Random(1291681);
		add(NKEYS);
		for (BtreeKey key : keys)
			assertFalse(btree.add(key, true));
	}

	@Test
	public void update() {
		rand = new Random(456978);
		add(NKEYS);
		Collections.shuffle(keys, rand);
		update(1);
		check();
		checkIterate();
	}

	@Test
	public void update_many() {
		rand = new Random(456978);
		add(NKEYS);
		Collections.shuffle(keys, rand);
		for (int i = 0; i < 200; ++i) {
			BtreeKey oldkey = keys.get(1);
			BtreeKey newkey = new BtreeKey(oldkey.key, oldkey.adr() + 1);
			keys.set(1, newkey);
			assertThat(btree.update(oldkey, newkey, true), is(Update.OK));
		}
		check();
		checkIterate();
	}

	private void update(int n) {
		for (int i = 0; i < n; ++i) {
			int k = rand.nextInt(NKEYS);
			BtreeKey oldkey = keys.get(k);
			BtreeKey newkey = updateKey(oldkey);
			keys.set(k, newkey);
			assertThat(btree.update(oldkey, newkey, true), is(Update.OK));
		}
	}

	private BtreeKey updateKey(BtreeKey oldkey) {
		if ((rand.nextInt() % 2) == 0)
			return randomKey(rand);
		else {
			// new address must be larger than old one
			return new BtreeKey(oldkey.key, oldkey.adr() + 1);
		}
	}

	@Test
	public void intref_adr_should_be_greater_than_db_offset() {
		BtreeKey intref = key("hello", 123 | IntRefs.MASK);
		BtreeKey offset = key("hello", 567);
		assert intref.compareTo(offset) > 0;
		assert offset.compareTo(intref) < 0;
	}

	@Test
	public void duplicate() {
		btree.add(key("tables", 123));
		btree.add(key("columns", 127));
		btree.add(key("indexes", 130));
		btree.add(key("views", 170));
		btree.add(key("tbl", 277));
		assertFalse(btree.add(key("tbl", 406), true));
		assertFalse(btree.add(key("columns", 127), false));
	}

	@Test
	public void rangefrac_one_node() {
		btree = new Btree(tran); // normal node size
		btree.add(key("0"));
		btree.add(key("1"));
		btree.add(key("2"));
		btree.add(key("3"));
		btree.add(key("4"));
		btree.add(key("5"));
		btree.add(key("6"));
		btree.add(key("7"));
		btree.add(key("8"));
		btree.add(key("9"));
		assertThat(btree.treeLevels(), is(0));
		assertThat((double) btree.rangefrac(rec("0"), rec("90")),
				closeTo(1.0, .01));
		assertThat((double) btree.rangefrac(rec("0"), rec("40")),
				closeTo(.5, .01));
		assertThat((double) btree.rangefrac(rec("0"), rec("00")),
				closeTo(.1, .01));
		assertThat((double) btree.rangefrac(rec("4"), rec("40")),
				closeTo(.1, .01));
		assertThat((double) btree.rangefrac(rec("9"), rec("90")),
				closeTo(.1, .01));
	}

	@Test
	public void rangefrac_multiple_big_nodes() {
		btree = new Btree(tran); // normal node size
		for (int i = 10; i < 210; i += 2)
			btree.add(key(i));
		assertThat(btree.treeLevels(), greaterThan(0));
		assertThat((double) btree.rangefrac(rec(11), rec(211)),
				closeTo(1.0, .01));
		assertThat((double) btree.rangefrac(rec(10), rec(30)),
				closeTo(.1, .01));
		assertThat((double) btree.rangefrac(rec(40), rec(180)),
				closeTo(.7, .01));
		assertThat((double) btree.rangefrac(rec(60), rec(160)),
				closeTo(.5, .01));
		assertThat((double) btree.rangefrac(rec(10), rec(11)),
				closeTo(.01, .01));
		assertThat((double) btree.rangefrac(rec(100), rec(100)),
				closeTo(0, .01));
	}

	@Test
	public void rangefrac_multiple_small_nodes() {
		for (int i = 10; i < 1034; i += 4)
			btree.add(key(i));
		assertThat(btree.treeLevels(), greaterThan(0));
		assertThat((double) btree.rangefrac(rec(0), rec(0)),
				closeTo(0.0, .01));
		assertThat((double) btree.rangefrac(rec(10), rec(9999)),
				closeTo(1.0, .01));
		assertThat((double) btree.rangefrac(rec(10), rec(111)),
				closeTo(.1, .01));
		assertThat((double) btree.rangefrac(rec(10), rec(715)),
				closeTo(.7, .01));
		assertThat((double) btree.rangefrac(rec(210), rec(714)),
				closeTo(.5, .01));
		assertThat((double) btree.rangefrac(rec(10), rec(111)),
				closeTo(.1, .01));
		assertThat((double) btree.rangefrac(rec(55), rec(55)),
				closeTo(0, .01));
	}

	@Test
	public void iterate_range() {
		add(100);
		Collections.sort(keys);

		Record from = keys.get(25).key;
		Record to = keys.get(75).key;
		Btree.Iter iter = btree.iterator(from, to);
		int i = 25;
		for (iter.next(); ! iter.eof(); iter.next())
			assertThat(iter.curKey(), is(keys.get(i++).key));
		assertThat(i, is(76));

		iter = btree.iterator(from, to);
		i = 75;
		for (iter.prev(); ! iter.eof(); iter.prev())
			assertThat("i " + i, iter.curKey(), is(keys.get(i--).key));
		assertThat(i, is(24));

		from = new RecordBuilder().add(from.getRaw(0)).build();
		to = new RecordBuilder().add(to.getRaw(0)).build();
		iter = btree.iterator(from, to);
		i = 25;
		for (iter.next(); ! iter.eof(); iter.next())
			assertThat(iter.curKey(), is(keys.get(i++).key));
		assertThat(i, is(76));

		iter = btree.iterator(from, to);
		i = 75;
		for (iter.prev(); ! iter.eof(); iter.prev())
			assertThat("i " + i, iter.curKey(), is(keys.get(i--).key));
		assertThat(i, is(24));
	}

	@Test
	public void totalSize() {
		assertThat(btree.totalSize(), is(0));
		add(100);
		for (BtreeKey key : keys)
			assertTrue(btree.remove(key));
		assertTrue(btree.isEmpty());
		assertThat(btree.totalSize(), is(0));
	}

	@Test
	public void iter_from_iter() {
		add(100);
		Collections.sort(keys);
		Record from = keys.get(25).key;
		Record to = keys.get(75).key;
		Btree.Iter iterOrig = btree.iterator(from, to);
		Btree.Iter iter = btree.iterator(iterOrig);
		int i = 25;
		for (iter.next(); ! iter.eof(); iter.next())
			assertThat("i " + i, iter.curKey(), is(keys.get(i++).key));
		assertThat(i, is(76));

		iterOrig.next();
		iter = btree.iterator(iterOrig);
		i = 25;
		for (; ! iter.eof(); iter.next())
			assertThat("i " + i, iter.curKey(), is(keys.get(i++).key));
		assertThat(i, is(76));
	}

	@Test
	public void seek_nonexistent() {
		btree.add(key("andy"));
		btree.add(key("zack"));

		Btree.Iter iter = btree.iterator(rec("fred"));
		iter.next();
		assertTrue(iter.eof());

		iter = btree.iterator(rec("fred"));
		iter.prev();
		assertTrue(iter.eof());

		iter = btree.iterator(rec("aaa"));
		iter.prev();
		assertTrue(iter.eof());

		iter = btree.iterator(rec("zzz"));
		iter.next();
		assertTrue(iter.eof());
	}

	@Test
	public void numeric_order() {
		btree.add(key(-1));
		btree.add(key(0));
		Btree.Iter iter = btree.iterator();
		iter.next();
		assertThat(iter.curKey(), is(key(-1).key));
		iter.next();
		assertThat(iter.curKey(), is(key(0).key));
		iter.next();
		assertTrue(iter.eof());
	}

	@Test
	public void composite_range() {
		btree.add(key(43, "a", 123));
		btree.add(key(43, "b", 127));
		btree.add(key(43, "c", 130));
		btree.add(key(43, "d", 170));
		btree.add(key(43, "e", 277));
		Record from = new RecordBuilder().add(43).build();
		Record to = new RecordBuilder().add(43).addMax().build();
		Btree.Iter iter = btree.iterator(from, to);
		iter.next();
		assertThat(iter.curKey().getString(1), is("a"));
	}

	@Test
	public void lookup() {
		btree.add(key("A", 1));
		btree.add(key("B", 2));
		btree.add(key("C", 3));
		btree.add(key("D", 4));
		btree.add(key("E", 5));
		btree.add(key("F", 6));
		assertThat(btree.get(rec("E")), is(5));

		Btree.Iter iter = btree.iterator(rec("E"));
		iter.next();
		assertFalse(iter.eof());
		assertThat(iter.keyadr(), is(5));
	}

	@Test
	public void lots_of_duplicates() {
		for (int i = 0; i < NKEYS; ++i) {
			BtreeKey key = key("abc", i + 1);
			btree.add(key, false);
			keys.add(key);
		}
		check();
		for (int i = NKEYS - 1; i >= 0; i -= 2)
			assertTrue(btree.remove(keys.remove(i)));
		check();
		checkIterate();
		for (BtreeKey k : keys) {
			BtreeKey newkey = key("abc", k.adr() + 100);
			assertThat(btree.update(k, newkey, false), is(Update.OK));
		}
	}

	@Test
	public void unique_check_failure_when_tree_key() {
		btree.add(key("a", 1));
		btree.add(key("b", 2));
		btree.add(key("c", 3));
		btree.add(key("d", 4));
		btree.add(key("e", 5));
		// "d" should now be tree key
		assertFalse(btree.add(key("d", 6), true));
	}

	@Test
	public void update_during_iteration() {
		btree.add(key("a"));
		btree.add(key("c"));
		Btree.Iter iter = btree.iterator();
		iter.next();
		BtreeKey newkey = key("b", tran.refToInt(rec("")));
		assertThat(btree.update(iter.cur(), newkey, true), is(Update.OK));
		iter.next();
		assertThat(iter.curKey().getString(0), is("c"));
	}

//	@Test
//	public void update_during_reverse_iteration() {
//		btree.add(key("a"));
//		btree.add(key("c"));
//		Btree.Iter iter = btree.iterator();
//		iter.prev();
//		assertThat(iter.curKey().getString(0), is("c"));
//		BtreeKey newkey = key("b", tran.refToInt(rec("")));
//		assertThat(btree.update(iter.cur(), newkey, true), is(Update.OK));
//		iter.prev();
//		assertThat(iter.curKey().getString(0), is("a"));
//	}

/*
 	// update during iteration of duplicates is NOT handled
	@Test
	public void update_iteration() {
		rand = new Random(89876);
		add(NKEYS);
		// make 5 duplicates of every 5th key
		for (int i = NKEYS - 1; i >= 0; i -= 5) {
			Record key = keys.get(i);
			for (int j = 0; j < 5; ++j) {
				Record newkey = key(key.getString(0), key.getInt(1) + j + 1);
				keys.add(newkey);
				assertTrue(btree.add(newkey, false));
			}
		}
		Collections.sort(keys);

		int i = 0;
		Btree2.Iter iter = btree.iterator();
		for (iter.next(); ! iter.eof(); iter.next(), ++i) {
			Record oldkey = iter.curKey();
			assertThat(oldkey, is(keys.get(i)));
			Record newkey = key(oldkey.getString(0), tran.refToInt(new Object()));
			assertThat(btree.update(oldkey, newkey, false), is(Update.OK));
		}
		assertThat(i, is(keys.size()));
	}
*/

	@Test
	public void update_iteration_dups() {
		assertTrue(btree.add(key("fred", 1), false));
		assertTrue(btree.add(key("suzy", 1), false));
		assertTrue(btree.add(key("suzy", 2), false));
		assertTrue(btree.add(key("suzy", 3), false));
		assertTrue(btree.add(key("suzy", 4), false));
		assertTrue(btree.add(key("suzy", 5), false));
		assertTrue(btree.add(key("zoe", 1), false));
		Btree.Iter iter = btree.iterator();
		iter.next();
		iter.next();
		iter.next();
		iter.next();
		assertThat(iter.curKey(), is(key("suzy", 3).key));
		assertThat(btree.update(iter.cur(), key("suzy", 6), false), is(Update.OK));
		iter.next();
		assertThat(iter.curKey(), is(key("suzy", 4).key));
	}

	@Test
	public void range_starts_at_end_of_node() {
		btree.add(key("aa", 1));
		btree.add(key("bb", 2));
		btree.add(key("cc", 3));
		btree.add(key("dd", 4));
		btree.add(key("ed", 5));
		// "dd" should now be tree key
		btree.remove(key("dd", 4));
		Btree.Iter iter = btree.iterator(rec("d"), rec("z"));
		iter.next();
		assertThat(iter.curKey(), is(rec("ed")));
	}

	@Test
	public void switch_direction() {
		add(10);
		Collections.sort(keys);
		Btree.Iter iter;

		// iterators stick at eof
		iter = btree.iterator();
		testNext(iter, 0);
		testPrev(iter, -1);
		testPrev(iter, -1);
		testNext(iter, -1);

		// just reading last item doesn't trigger eof, can still reverse
		iter = btree.iterator();
		testPrev(iter, 9);
		testPrev(iter, 8);
		testPrev(iter, 7);
		testNext(iter, 8);
		testNext(iter, 9); // last
		testPrev(iter, 8);
	}
	private void testPrev(TranIndex.Iter iter, int i) {
		iter.prev();
		test(iter, i);
	}
	private void testNext(TranIndex.Iter iter, int i) {
		iter.next();
		test(iter, i);
	}
	private void test(TranIndex.Iter iter, int i) {
		if (i == -1)
			assertTrue(iter.eof());
		else
			assertEquals(keys.get(i).key, iter.curKey());
	}

	//--------------------------------------------------------------------------

	public static List<BtreeKey> randomKeys(Random rand, int n) {
		List<BtreeKey> keys = new ArrayList<BtreeKey>();
		for (int i = 0; i < n; ++i)
			keys.add(randomKey(rand));
		return keys;
	}

	public static BtreeKey randomKey(Random rand) {
		int n = 4 + rand.nextInt(5);
		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < n; ++i)
			sb.append((char) ('a' + rand.nextInt(26)));
		final int UPDATE_ALLOWANCE = 10000;
		return key(sb.toString(), rand.nextInt(Integer.MAX_VALUE - UPDATE_ALLOWANCE));
	}

	static BtreeKey key(String s) {
		return new RecordBuilder().add(s).btreeKey(123);
	}

	private static BtreeKey key(String s, int adr) {
		return new RecordBuilder().add(s).btreeKey(adr);
	}

	static BtreeKey key(int n) {
		return new RecordBuilder().add(n).btreeKey(n * 10);
	}

	static BtreeKey key(int n, String s, int adr) {
		return new RecordBuilder().add(n).add(s).btreeKey(adr);
	}

	static Record rec(Object... data) {
		RecordBuilder rb = new RecordBuilder();
		for (Object x : data)
			if (x instanceof String)
				rb.add(x);
			else if (x instanceof Integer)
				rb.add((int) (Integer) x);
		return rb.build();
	}

}
