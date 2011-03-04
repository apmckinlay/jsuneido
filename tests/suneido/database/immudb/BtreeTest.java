/* Copyright 2010 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido.database.immudb;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

import java.io.File;
import java.util.*;

import org.junit.After;
import org.junit.Test;

public class BtreeTest {
	private Tran tran = new Tran();
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
		List<Record> keys = new ArrayList<Record>();
		int NKEYS = 1000;
		Random rand = new Random(1234);
		for (int i = 0; i < NKEYS; ++i)
			keys.add(randomKey(rand));

		Btree btree = new Btree(tran);
		for (Record key : keys)
			btree.add(key);

		Collections.shuffle(keys, rand);
		for (Record key : keys)
			assertThat(btree.get(key), is(adr(key)));
	}

	@Test
	public void persist() {
		List<Record> keys = new ArrayList<Record>();
		Random rand = new Random(90873);

		tran.mmf(new MmapFile("tmp1", "rw"));
		Btree btree = new Btree(tran);
		persist(btree);

		tran = new Tran();
		tran.mmf(new MmapFile("tmp1", "rw"));
		btree = new Btree(tran, root, levels);
		addAndPersist(10, rand, keys, btree);
		assertThat("levels", btree.treeLevels(), is(0));

		tran = new Tran();
		tran.mmf(new MmapFile("tmp1", "rw"));
		btree = new Btree(tran, root, levels);
		addAndPersist(100, rand, keys, btree);
		assertThat("levels", btree.treeLevels(), is(1));

		tran = new Tran();
		tran.mmf(new MmapFile("tmp1", "rw"));
		tran.setRedirs(new Redirects(DbHashTree.from(tran, redirs)));
		btree = new Btree(tran, root, levels);
		addAndPersist(200, rand, keys, btree);
		assertThat("levels", btree.treeLevels(), is(2));

		tran = new Tran();
		tran.mmf(new MmapFile("tmp1", "rw"));
		tran.setRedirs(new Redirects(DbHashTree.from(tran, redirs)));
		btree = new Btree(tran, root, levels);
		Collections.shuffle(keys, rand);
		for (Record key : keys)
			assertThat("key " + key, btree.get(key), is(adr(key)));
		tran.mmf().close();
	}

	private void persist(Btree btree) {
		tran.startPersist();
		btree.persist();
		root = btree.root();
		levels = btree.treeLevels();
		redirs = tran.persistRedirs();
		tran.mmf().close();
		tran = null;
	}

	private void addAndPersist(int n, Random rand, List<Record> keys, Btree btree) {
		for (int i = 0; i < n; ++i) {
			Record key = randomKey(rand);
			btree.add(key);
			keys.add(key);
		}
		persist(btree);
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
		tran.mmf(new MmapFile("tmp1", "rw"));
		Btree btree = new Btree(tran);
		Record rec = record("a data record");
		int intref = tran.refRecordToInt(rec);
		Record key = record("hello", intref);
		btree.add(key);
		tran.startPersist();
		tran.persistDataRecords();
		int adr = tran.getAdr(intref);
		assert adr != 0;
		btree.persist();
		int root = btree.root();
		int levels = btree.treeLevels();
		tran.mmf().close();

		tran = new Tran();
		tran.mmf(new MmapFile("tmp1", "rw"));
		btree = new Btree(tran, root, levels);
		assertThat(btree.get(record("hello")), is(adr));
		tran.mmf().close();
	}

	private int adr(Record key) {
		return Btree.getAddress(key);
	}

	public Record randomKey(Random rand) {
		int n = 1 + rand.nextInt(5);
		String s = "";
		for (int i = 0; i < n; ++i)
			s += (char) ('a' + rand.nextInt(26));
		return record(s, rand.nextInt());
	}

	private Record record(String s) {
		return new RecordBuilder().add(s).build();
	}

	private Record record(String s, int n) {
		return new RecordBuilder().add(s).add(n).build();
	}

	@After
	public void teardown() {
		new File("tmp1").delete();
	}

}
