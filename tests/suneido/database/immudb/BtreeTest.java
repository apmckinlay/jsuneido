/* Copyright 2010 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido.database.immudb;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

import java.io.File;
import java.util.*;

import org.junit.After;
import org.junit.Test;

public class BtreeTest {

	@Test
	public void empty() {
		Btree btree = new Btree();
		assertThat(btree.get(record("hello", 1234)), is(0));
	}

	@Test
	public void main() {
		List<Record> keys = new ArrayList<Record>();
		int NKEYS = 1000;
		Random rand = new Random(1234);
		for (int i = 0; i < NKEYS; ++i)
			keys.add(randomKey(rand));

		Btree btree = new Btree();
		for (Record key : keys)
			btree.add(key);

		Collections.shuffle(keys, rand);
		for (Record key : keys)
			assertThat(btree.get(key), equalTo(adr(key)));
	}

	@Test
	public void persist() {
		List<Record> keys = new ArrayList<Record>();
		Random rand = new Random(90873);

		Tran.mmf(new MmapFile("tmp1", "rw"));
		Btree btree = new Btree();
		btree.persist();
		int root = btree.root();
		int levels = btree.treeLevels();
		Tran.mmf().close();
		Tran.remove();

		Tran.mmf(new MmapFile("tmp1", "rw"));
		btree = new Btree(root, levels);
		int redirs = addAndPersist(10, rand, keys, btree);
		root = btree.root();
		levels = btree.treeLevels();
		assertThat("levels", btree.treeLevels(), is(0));
		Tran.mmf().close();
		Tran.remove();

		Tran.mmf(new MmapFile("tmp1", "rw"));
		btree = new Btree(root, levels);
		redirs = addAndPersist(100, rand, keys, btree);
		root = btree.root();
		levels = btree.treeLevels();
		assertThat("levels", btree.treeLevels(), is(1));
		Tran.mmf().close();
		Tran.remove();

		Tran.mmf(new MmapFile("tmp1", "rw"));
		Tran.setRedirs(new Redirects(redirs));
		btree = new Btree(root, levels);
		redirs = addAndPersist(200, rand, keys, btree);
		root = btree.root();
		levels = btree.treeLevels();
		assertThat("levels", btree.treeLevels(), is(2));
		Tran.mmf().close();
		Tran.remove();

		Tran.mmf(new MmapFile("tmp1", "rw"));
		Tran.setRedirs(new Redirects(redirs));
		btree = new Btree(root, levels);
		Collections.shuffle(keys, rand);
		for (Record key : keys)
			assertThat("key " + key, btree.get(key), equalTo(adr(key)));
		Tran.mmf().close();
	}

	private int addAndPersist(int n, Random rand, List<Record> keys, Btree btree) {
		for (int i = 0; i < n; ++i) {
			Record key = randomKey(rand);
			btree.add(key);
			keys.add(key);
		}
		btree.persist();
		return Tran.redirs().persist();
	}

	private int adr(Record key) {
		return (Integer) key.get(1);
	}

	public Record randomKey(Random rand) {
		int n = 1 + rand.nextInt(5);
		String s = "";
		for (int i = 0; i < n; ++i)
			s += (char) ('a' + rand.nextInt(26));
		return record(s, rand.nextInt());
	}

	private Record record(String s, int n) {
		return new RecordBuilder().add(s).add(n).build();
	}

	@After
	public void teardown() {
		Tran.remove();
		new File("tmp1").delete();
	}

}
