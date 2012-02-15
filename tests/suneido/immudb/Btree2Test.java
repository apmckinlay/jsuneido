/* Copyright 2012 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido.immudb;

import static org.hamcrest.Matchers.closeTo;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static suneido.immudb.BtreeNode.adr;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

import org.junit.Test;

import suneido.immudb.TranIndex.Update;

import com.google.common.collect.Lists;

public class Btree2Test {
	private final Storage stor = new MemStorage(1024, 64);
	private Random rand = new Random(123456);
	private Tran tran = new Tran(stor);
	private Btree2 btree = new Btree4(tran);
	private List<Record> keys = Lists.newArrayList();
	private int NKEYS = 100;
	private BtreeInfo info;
	private int redirs;

	private static class Btree4 extends Btree2 {
		@Override public int splitSize() { return 4; }
		public Btree4(Tran tran) {
			super(tran);
		}
		public Btree4(Tran tran, BtreeInfo info) {
			super(tran, info);
		}
	}

	@Test
	public void add_one() {
		btree.add(key("test"));
	}

	@Test
	public void empty() {
		assertTrue(btree.isEmpty());
		assertThat(btree.get(key("hello", 123)), is(0));
	}

	@Test
	public void add1() {
		btree.add(key("hello", 123));
		assertThat(btree.get(key("hello", 123)), is(123));
	}

	@Test
	public void first_leaf_split_end() {
		btree.add(rec("a", 1));
		btree.add(rec("b", 2));
		btree.add(rec("c", 3));
		btree.add(rec("d", 4));
		btree.add(rec("e", 5));
		btree.check();
	}

	@Test
	public void first_leaf_split_left() {
		btree.add(rec("a", 1));
		btree.add(rec("c", 3));
		btree.add(rec("d", 4));
		btree.add(rec("e", 5));
		btree.add(rec("b", 2));
		btree.check();
	}

	@Test
	public void first_leaf_split_right() {
		btree.add(rec("a", 1));
		btree.add(rec("b", 2));
		btree.add(rec("c", 3));
		btree.add(rec("e", 5));
		btree.add(rec("d", 4));
		btree.check();
	}

	@Test
	public void first_leaf_insert() {
		btree.add(rec("a", 1));
		btree.add(rec("b", 2));
		btree.add(rec("c", 3));
		btree.add(rec("d", 4));
		btree.add(rec("e", 5));
		btree.add(rec("f", 5));
		btree.add(rec("g", 5));
		btree.add(rec("h", 5));
		btree.add(rec("i", 5));
		btree.check();
	}

	@Test
	public void first_leaf_split() {
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
			Record key = keys.get(i);
			assertThat("key " + key, btree.get(key), is(adr(key)));
		}
		for (int i = 0; i < NKEYS/2; ++i)
			btree.add(keys.get(i));
		check();

		Record min = key("", 0);
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
	public void add_remove_one() {
		btree.add(key("foo"));
		assertFalse(btree.remove(key("bar")));
		assertTrue(btree.remove(key("foo")));
		assertTrue(btree.isEmpty());
	}

	@Test
	public void add_remove_treeLevel() {
		keys = randomKeys(rand, 5);
		Collections.sort(keys);
		for (Record key : keys)
			btree.add(key);
		assertFalse(btree.remove(key("dkfjsdkfjds")));
		for (Record key : keys)
			assertTrue(btree.remove(key));
		assertTrue(btree.isEmpty());
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

	private void removeAndCheck(
			int NKEYS, Random rand, List<Record> keys, Btree2 btree) {
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
		Record minKey = key("", 0);
		btree.add(minKey);
		Record maxKey = key("zzzzzzz", Integer.MAX_VALUE);
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
		assertTrue(btree.isEmpty());
	}

//	@Test
//	public void update_and_store() {
//		rand = new Random(789456);
//		store();
//
//		tran = new Tran(stor);
//		btree = new Btree4(tran, info);
//		addRemoveAndStore(2);
//		assertThat("levels", btree.treeLevels(), is(0));
//
//		tran = new Tran(stor);
//		btree = new Btree4(tran, info);
//		addRemoveAndStore(3);
//		assertThat("levels", btree.treeLevels(), is(1));
//
//		tran = new Tran(stor, redirs);
//		btree = new Btree4(tran, info);
//		addRemoveAndStore(400);
//		assertThat("levels", btree.treeLevels(), is(6));
//
//		tran = new Tran(stor, redirs);
//		btree = new Btree4(tran, info);
//		check();
//		checkIterate();
//
//		store();
//	}

	private void addRemoveAndStore(int n) {
//System.out.println("--------------- before update");
//btree.print();
		check();
		add(2 * n);
		remove(n);
//System.out.println("--------------- after update");
//btree.print();
		check();
//		store();
//System.out.println("--------------- after store");
//btree.print();
	}

	private void add(int n) {
		for (int i = 0; i < n; ++i) {
			Record key = randomKey(rand);
			btree.add(key);
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
		Btree2.store(tran);
		info = btree.info();
		redirs = tran.storeRedirs();
		tran.endStore();
		tran = null;
	}

	@Test
	public void iterate_empty() {
		Btree2.Iter iter = btree.iterator();
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
		Btree2.Iter iter = btree.iterator();
		for (iter.prev(); ! iter.eof(); iter.prev())
			assertThat("i " + i, iter.curKey(), is(keys.get(--i)));
		assertThat(i, is(0));
		iter.prev();
		assertTrue(iter.eof());
	}

	public Btree2.Iter checkIterate() {
		Collections.sort(keys);

		int i = 0;
		Btree2.Iter iter = btree.iterator();
		for (iter.next(); ! iter.eof(); iter.next())
			assertThat("i " + i, iter.curKey(), is(keys.get(i++)));
		assertThat(i, is(keys.size()));
		return iter;
	}

	@Test
	public void iterate_delete_behind() {
		rand = new Random(546453);
		add(NKEYS);
		Collections.sort(keys);

		int i = 0;
		Btree2.Iter iter = btree.iterator();
		for (iter.next(); ! iter.eof(); iter.next()) {
			assertThat(iter.curKey(), is(keys.get(i)));
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
		Btree2.Iter iter = btree.iterator();
		for (iter.next(); ! iter.eof(); iter.next()) {
			assertThat(iter.curKey(), is(keys.get(i++)));
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
		Btree2.Iter iter = btree.iterator();
		for (iter.next(); ! iter.eof(); iter.next()) {
			assertThat(iter.curKey(), is(keys.get(i)));
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
		Btree2.Iter iter = btree.iterator(rec("test2"), rec("test4"));
		iter.next();
		assertThat(iter.curKey().getString(0), is("test2"));
		assertTrue(btree.remove(iter.curKey()));
		iter.next();
		assertThat(iter.curKey().getString(0), is("test3"));
		assertTrue(btree.remove(iter.curKey()));
		iter.next();
		assertThat(iter.curKey().getString(0), is("test4")); // in next node
		assertTrue(btree.remove(iter.curKey()));
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
		Btree2.Iter iter = btree.iterator(rec("test2"), rec("test4"));
		iter.prev();
		assertThat(iter.curKey().getString(0), is("test4"));
		assertTrue(btree.remove(iter.curKey()));
		iter.prev();
		assertThat(iter.curKey().getString(0), is("test3"));
		assertTrue(btree.remove(iter.curKey()));
		iter.prev();
		assertThat(iter.curKey().getString(0), is("test2")); // in next node
		assertTrue(btree.remove(iter.curKey()));
		iter.prev();
		assertTrue(iter.eof());
	}

	@Test
	public void unique() {
		rand = new Random(1291681);
		add(10);//NKEYS);
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

//		store();
//		tran = new Tran(stor);
//		btree = new Btree4(tran, info);
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
				.add(adr(oldkey) + 1)
				.build();
		}
	}

	@Test
	public void intref_adr_should_be_greater_than_db_offset() {
		Record intref = key("hello", 123 | IntRefs.MASK);
		Record offset = key("hello", 567);
		assert intref.compareTo(offset) > 0;
		assert offset.compareTo(intref) < 0;
	}

//	@Test
//	public void translate_data_refs() {
//		Record rec = rec("a data record");
//		rec.tblnum = 123;
//		int intref = tran.refToInt(rec);
//		Record key = key("hello", intref);
//		btree.add(key);
//		tran.startStore();
//		DataRecords.store(tran);
//		int adr = tran.getAdr(intref);
//		assert adr != 0;
//		Btree2.store(tran);
//		info = btree.info();
//
//		tran = new Tran(stor);
//		btree = new Btree4(tran, info);
//		assertThat(btree.get(rec("hello")), is(adr));
//	}

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
		btree = new Btree2(tran); // normal node size
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
	}

	@Test
	public void rangefrac_multiple_nodes() {
		btree = new Btree2(tran); // normal node size
		for (int i = 10; i < 210; i += 2)
			btree.add(key(i));
		assertThat(btree.treeLevels(), greaterThan(0));
		assertThat((double) btree.rangefrac(key(11), key(211)),
				closeTo(1.0, .01));
		assertThat((double) btree.rangefrac(key(10), key(30)),
				closeTo(.1, .01));
		assertThat((double) btree.rangefrac(key(40), key(180)),
				closeTo(.7, .01));
		assertThat((double) btree.rangefrac(key(60), key(160)),
				closeTo(.5, .01));
	}

	@Test
	public void iterate_range() {
		add(100);
		Collections.sort(keys);

		Record from = keys.get(25);
		Record to = keys.get(75);
		Btree2.Iter iter = btree.iterator(from, to);
		int i = 25;
		for (iter.next(); ! iter.eof(); iter.next())
			assertThat(iter.curKey(), is(keys.get(i++)));
		assertThat(i, is(76));

		iter = btree.iterator(from, to);
		i = 75;
		for (iter.prev(); ! iter.eof(); iter.prev())
			assertThat("i " + i, iter.curKey(), is(keys.get(i--)));
		assertThat(i, is(24));

		from = new RecordBuilder().add(from.getRaw(0)).build();
		to = new RecordBuilder().add(to.getRaw(0)).build();
		iter = btree.iterator(from, to);
		i = 25;
		for (iter.next(); ! iter.eof(); iter.next())
			assertThat(iter.curKey(), is(keys.get(i++)));
		assertThat(i, is(76));

		iter = btree.iterator(from, to);
		i = 75;
		for (iter.prev(); ! iter.eof(); iter.prev())
			assertThat("i " + i, iter.curKey(), is(keys.get(i--)));
		assertThat(i, is(24));
	}

	@Test
	public void totalSize() {
		assertThat(btree.totalSize(), is(0));
		add(100);
		for (Record key : keys)
			assertTrue(btree.remove(key));
		assertTrue(btree.isEmpty());
		assertThat(btree.totalSize(), is(0));
	}

	@Test
	public void iter_from_iter() {
		add(100);
		Collections.sort(keys);
		Record from = keys.get(25);
		Record to = keys.get(75);
		Btree2.Iter iterOrig = btree.iterator(from, to);
		Btree2.Iter iter = btree.iterator(iterOrig);
		int i = 25;
		for (iter.next(); ! iter.eof(); iter.next())
			assertThat("i " + i, iter.curKey(), is(keys.get(i++)));
		assertThat(i, is(76));

		iterOrig.next();
		iter = btree.iterator(iterOrig);
		i = 25;
		for (; ! iter.eof(); iter.next())
			assertThat("i " + i, iter.curKey(), is(keys.get(i++)));
		assertThat(i, is(76));
	}

	@Test
	public void seek_nonexistent() {
		btree.add(key("andy"));
		btree.add(key("zack"));

		Btree2.Iter iter = btree.iterator(key("fred"));
		iter.next();
		assertTrue(iter.eof());

		iter = btree.iterator(key("fred"));
		iter.prev();
		assertTrue(iter.eof());

		iter = btree.iterator(key("aaa"));
		iter.prev();
		assertTrue(iter.eof());

		iter = btree.iterator(key("zzz"));
		iter.next();
		assertTrue(iter.eof());
	}

	@Test
	public void numeric_order() {
		btree.add(key(-1));
		btree.add(key(0));
		Btree2.Iter iter = btree.iterator();
		iter.next();
		assertThat(iter.curKey(), is(key(-1)));
		iter.next();
		assertThat(iter.curKey(), is(key(0)));
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
		Btree2.Iter iter = btree.iterator(from, to);
		iter.next();
		assertThat(iter.curKey().getString(1), is("a"));
	}

	@Test
	public void lookup() {
		btree.add(rec("A", -1, 1));
		btree.add(rec("B", -1, 2));
		btree.add(rec("C", -1, 3));
		btree.add(rec("D", -1, 4));
		btree.add(rec("E", -1, 5));
		btree.add(rec("F", -1, 6));
		assertThat(btree.get(rec("E", -1)), is(5));

		Btree2.Iter iter = btree.iterator(rec("E", -1));
		iter.next();
		assertFalse(iter.eof());
		assertThat(iter.keyadr(), is(5));
	}

	@Test
	public void lots_of_duplicates() {
		final int NKEYS = 100;
		for (int i = 0; i < NKEYS; ++i) {
			Record key = key("abc", i);
			btree.add(key, false);
			keys.add(key);
		}
		check();
		for (int i = NKEYS - 1; i >= 0; i -= 2)
			assertTrue(btree.remove(keys.remove(i)));
		check();
		checkIterate();
		for (Record k : keys) {
			Record newkey = key("abc", k.getInt(1) + 100);
			assertThat(btree.update(k, newkey, false), is(Update.OK));
		}
	}

	@Test
	public void unique_check_failure_when_tree_key() {
		btree.add(rec("a", 1));
		btree.add(rec("b", 2));
		btree.add(rec("c", 3));
		btree.add(rec("d", 4));
		btree.add(rec("e", 5));
		// "d" should now be tree key
		assertFalse(btree.add(rec("d", 6), true));
	}

	@Test
	public void update_during_iteration() {
		btree.add(key("a"));
		btree.add(key("c"));
		Btree2.Iter iter = btree.iterator();
		iter.next();
		Record oldkey = iter.curKey();
		Record newkey = key("b", tran.refToInt(new Object()));
		assertThat(btree.update(oldkey, newkey, true), is(Update.OK));
		iter.next();
		assertThat(iter.curKey().getString(0), is("c"));
	}

	@Test
	public void update_during_reverse_iteration() {
		btree.add(key("a"));
		btree.add(key("c"));
		Btree2.Iter iter = btree.iterator();
		iter.prev();
		Record oldkey = iter.curKey();
		Record newkey = key("b", tran.refToInt(new Object()));
		assertThat(btree.update(oldkey, newkey, true), is(Update.OK));
		iter.prev();
		assertThat(iter.curKey().getString(0), is("a"));
	}

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
		Btree2.Iter iter = btree.iterator();
		iter.next();
		iter.next();
		iter.next();
		iter.next();
		assertThat(iter.curKey(), is(key("suzy", 3)));
		assertThat(btree.update(iter.curKey(), key("suzy", 6), false), is(Update.OK));
		iter.next();
		assertThat(iter.curKey(), is(key("suzy", 4)));
	}

	@Test
	public void range_starts_at_end_of_node() {
		btree.add(rec("aa", 1));
		btree.add(rec("bb", 2));
		btree.add(rec("cc", 3));
		btree.add(rec("dd", 4));
		btree.add(rec("ed", 5));
		// "dd" should now be tree key
		btree.remove(rec("dd", 4));
		Btree2.Iter iter = btree.iterator(rec("d"), rec("z"));
		iter.next();
		assertThat(iter.curKey(), is(rec("ed", 5)));
	}

	//--------------------------------------------------------------------------

	public static List<Record> randomKeys(Random rand, int n) {
		List<Record> keys = new ArrayList<Record>();
		for (int i = 0; i < n; ++i)
			keys.add(randomKey(rand));
		return keys;
	}

	public static Record randomKey(Random rand) {
		int n = 4 + rand.nextInt(5);
		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < n; ++i)
			sb.append((char) ('a' + rand.nextInt(26)));
		final int UPDATE_ALLOWANCE = 10000;
		return key(sb.toString(), rand.nextInt(Integer.MAX_VALUE - UPDATE_ALLOWANCE));
	}

	static Record key(String s) {
		return new RecordBuilder().add(s).adduint(123).build();
	}

	private static Record key(String s, int adr) {
		return new RecordBuilder().add(s).adduint(adr).build();
	}

	static Record key(int n) {
		return new RecordBuilder().add(n).adduint(n * 10).build();
	}

	static Record key(int n, String s, int adr) {
		return new RecordBuilder().add(n).add(s).adduint(adr).build();
	}

	static Record rec(Object... data) {
		RecordBuilder rb = new RecordBuilder();
		for (Object x : data)
			if (x instanceof String)
				rb.add(x);
			else if (x instanceof Integer)
				rb.add(x);
		return rb.build();
	}

}
