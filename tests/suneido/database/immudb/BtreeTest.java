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

	@Test
	public void empty() {
		Btree btree = new Btree(tran);
		assertThat(btree.get(record("hello", 1234)), is(0));
	}

	@Test
	public void main() {
		List<DbRecord> keys = new ArrayList<DbRecord>();
		int NKEYS = 1000;
		Random rand = new Random(1234);
		for (int i = 0; i < NKEYS; ++i)
			keys.add(randomKey(rand));

		Btree btree = new Btree(tran);
		for (DbRecord key : keys)
			btree.add(key);

		Collections.shuffle(keys, rand);
		for (DbRecord key : keys)
			assertThat(btree.get(key), is(adr(key)));
	}

	@Test
	public void store() {
		List<DbRecord> keys = new ArrayList<DbRecord>();
		Random rand = new Random(90873);

		Btree btree = new Btree(tran);
		store(btree);

		tran = new Tran(stor);
		btree = new Btree(tran, root, levels);
		addAndStore(10, rand, keys, btree);
		assertThat("levels", btree.treeLevels(), is(0));

		tran = new Tran(stor);
		btree = new Btree(tran, root, levels);
		addAndStore(100, rand, keys, btree);
		assertThat("levels", btree.treeLevels(), is(1));

		tran = new Tran(stor);
		tran.setRedirs(new Redirects(DbHashTree.from(stor, redirs)));
		btree = new Btree(tran, root, levels);
		addAndStore(200, rand, keys, btree);
		assertThat("levels", btree.treeLevels(), is(2));

		tran = new Tran(stor);
		tran.setRedirs(new Redirects(DbHashTree.from(stor, redirs)));
		btree = new Btree(tran, root, levels);
		check(keys, rand, btree);
	}

	private void check(List<DbRecord> keys, Random rand, Btree btree) {
		Collections.shuffle(keys, rand);
		for (DbRecord key : keys)
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

	private void addAndStore(int n, Random rand, List<DbRecord> keys, Btree btree) {
//System.out.println("--------------- before add");
//btree.print();
		check(keys, rand, btree);
		add(n, rand, keys, btree);
//System.out.println("--------------- after add");
//btree.print();
		check(keys, rand, btree);
		store(btree);
	}

	private void add(int n, Random rand, List<DbRecord> keys, Btree btree) {
		for (int i = 0; i < n; ++i) {
			DbRecord key = randomKey(rand);
			btree.add(key);
			keys.add(key);
		}
	}

	@Test
	public void intref_adr_should_be_greater_than_db_offset() {
		DbRecord intref = record("hello", 123 | IntRefs.MASK);
		DbRecord offset = record("hello", 567);
		assert intref.compareTo(offset) > 0;
		assert offset.compareTo(intref) < 0;
	}

	@Test
	public void translate_data_refs() {
		Btree btree = new Btree(tran);
		DbRecord rec = record("a data record");
		int intref = tran.refRecordToInt(rec);
		DbRecord key = record("hello", intref);
		btree.add(key);
		tran.startStore();
		tran.storeDataRecords();
		int adr = tran.getAdr(intref);
		assert adr != 0;
		btree.store();
		int root = btree.root();
		int levels = btree.treeLevels();

		tran = new Tran(stor);
		btree = new Btree(tran, root, levels);
		assertThat(btree.get(record("hello")), is(adr));
	}

	private int adr(DbRecord key) {
		return Btree.getAddress(key);
	}

	public DbRecord randomKey(Random rand) {
		int n = 1 + rand.nextInt(5);
		String s = "";
		for (int i = 0; i < n; ++i)
			s += (char) ('a' + rand.nextInt(26));
		return record(s, rand.nextInt(Integer.MAX_VALUE));
	}

	private DbRecord record(String s) {
		return new RecordBuilder().add(s).build();
	}

	private DbRecord record(String s, int n) {
		return new RecordBuilder().add(s).add(n).build();
	}

}
