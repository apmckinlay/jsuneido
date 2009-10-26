package suneido.database;

import static suneido.Suneido.verify;
import static suneido.util.Util.lowerBound;
import static suneido.util.Util.upperBound;

import java.nio.ByteBuffer;

import suneido.SuException;

/**
 * Btree implementation.
 * Uses {@link Slots} to store nodes.
 * @see BtreeIndex
 * @author Andrew McKinlay
 * <p><small>Copyright 2008 Suneido Software Corp. All rights reserved. Licensed under GPLv2.</small>
 */
public class Btree {

	public final static int MAXLEVELS = 20;
	public final static long TREENODE_PREV = (long) Integer.MAX_VALUE << Mmfile.SHIFT;
	private final static int BLOCKSIZE = 4096;
	public final static int NODESIZE = BLOCKSIZE - Mmfile.OVERHEAD;
	enum Insert { OK, DUP, WONT_FIT };	// return values for insert

	private Destination dest;
	private long root_;
	private int treelevels;	// not including leaves
	private int nnodes;
	private long modified = 0;

	/** Create new */
	public Btree(Destination dest) {
		this.dest = dest;
		root_ = 0; // lazy creation
		treelevels = 0;
		nnodes = 0;
	}

	/** Open existing */
	public Btree(Destination dest, long root, int treelevels, int nnodes) {
		this.dest = dest;
		verify(root >= 0);
		root_ = root;
		verify(0 <= treelevels && treelevels < MAXLEVELS);
		this.treelevels = treelevels;
		verify(nnodes > 0);
		this.nnodes = nnodes;
	}

	/** Copy constructor */
	public Btree(Btree bt, Destination dest) {
		this.dest = dest;
		root_ = bt.root_;
		treelevels = bt.treelevels;
		nnodes = bt.nnodes;
	}

	// used when completing transactions
	public boolean update(Btree btOld, Btree btNew) {
		if (root_ != btOld.root_)
			return false;
		assert treelevels == btOld.treelevels;
		btNew.nnodes = nnodes + (btNew.nnodes - btOld.nnodes);
		return true;
	}

	public boolean differsFrom(Btree bt) {
		return root_ != bt.root_ || treelevels != bt.treelevels
				|| nnodes != bt.nnodes;
	}

	public int treelevels() {
		return treelevels;
	}

	public int nnodes() {
		return nnodes;
	}

	public void setDest(Destination dest) {
		this.dest = dest;
	}

	public boolean isEmpty() {
		if (nnodes == 0)
			return true;
		if (treelevels > 0)
			return false;
		verify(nnodes == 1);
		return new LeafNode(root()).isEmpty();
	}

	// not void so we can call it with assert
	public boolean isValid() {
		long[] links = new long[] { 0, -1 };
		isValid(root(), 0, links);
		assert links[1] == 0; // final next should be 0
		return true;
	}
	// links is checked and then set by each leaf node
	private void isValid(long adr, int level, long[] links) {
		if (level < treelevels) {
			TreeNode tn = new TreeNode(adr);
			tn.isValid(Mode.OPEN);
			for (int i = 0; i < tn.slots.size(); ++i)
				isValid(tn.slots.get(i).adrs[0], level + 1, links);
			isValid(tn.next(), level + 1, links);
		} else {
			LeafNode ln = new LeafNode(adr);
			ln.isValid(Mode.OPEN);
			assert links[0] == ln.prev(); // our prev is wrong
			assert links[1] == -1 || links[1] == adr; // prev's next was wrong
			links[0] = adr;
			links[1] = ln.next();
		}
	}

	public boolean insert(Slot x) { // returns false for duplicate key
		TreeNode[] nodes = new TreeNode[MAXLEVELS];

		// search down the tree
		long adr = root();
		int i;
		for (i = 0; i < treelevels; ++i) {
			nodes[i] = new TreeNode(adr);
			adr = nodes[i].find(x.key);
		}
		LeafNode leaf = new LeafNode(adr);

		// insert the key & data into the leaf
		Insert status = leaf.insert(x);
		if (status != Insert.WONT_FIT)
			return status == Insert.OK;
		// split
		++modified;
		LeafNode left = leaf.split(x);
		++nnodes;
		if (!(Insert.OK == (x.compareTo(left.slots.back()) <= 0
				? left.insert(x) : leaf.insert(x))))
			throw new SuException("index entry too large to insert");
		Record key = left.slots.back().key.dup();
		adr = left.adr;

		// insert up the tree as necessary
		for (--i; i >= 0; --i) {
			if (nodes[i].insert(key, adr))
				return true;
			// else split
			TreeNode tleft = nodes[i].split(key);
			++nnodes;
			if (!(tleft.slots.back().key.compareTo(key) < 0 ? nodes[i].insert(
					key, adr) : tleft.insert(key, adr)))
				throw new SuException("index entry too large to insert");
			key = tleft.slots.back().key.dup();
			tleft.setNext(tleft.slots.back().adrs[0]);
			tleft.slots.removeLast();
			adr = tleft.adr;
		}
		newRoot(key, adr);
		return true ;
	}
	private void newRoot(Record key, long off) {
		long roff = dest.alloc(NODESIZE, Mmfile.OTHER);
		TreeNode r = new TreeNode(roff, Mode.CREATE);
		++nnodes;
		r.insert(key, off);
		r.setNext(root_);
		root_ = roff;
		++treelevels;
		verify(treelevels < MAXLEVELS);
	}

	public boolean remove(Record key) {
		TreeNode[] nodes = new TreeNode[MAXLEVELS];

		// search down the tree
		int i;
		long off = root();
		for (i = 0; i < treelevels; ++i) {
			nodes[i] = new TreeNode(off);
			off = nodes[i].find(key);
		}
		// erase the key and data from the leaf
		LeafNode leaf = new LeafNode(off);
		if (! leaf.erase(new Slot(key)))
			return false;
		if (! leaf.isEmpty() || treelevels == 0)
			return true;	// this is the usual path
		leaf.unlink();
		--nnodes;
		++modified;
		// erase up the tree
		for (--i; i > 0; --i) {
			nodes[i].erase(key, i + 1 < treelevels ? nodes[i + 1].adr : off);
			if (!nodes[i].isEmpty())
				return true;
			--nnodes;
		}
		nodes[0].erase(key, i + 1 < treelevels ? nodes[i + 1].adr : off);
		// remove roots with only single pointer (no keys)
		for (; treelevels > 0; --treelevels) {
			TreeNode node = new TreeNode(root_);
			if (node.slots.size() > 0)
				return true;
			root_ = node.next();
			--nnodes;
		}
		return true;
	}

	public float rangefrac(Record from, Record to) {
		// from is inclusive, end is exclusive
		if (treelevels == 0)
			{
			LeafNode node = new LeafNode(root());
			if (node.isEmpty())
				return 0;
			Slots slots = node.slots;
			int org = lowerBound(slots, new Slot(from));
			int end = lowerBound(slots, new Slot(to));
			return (float) (end - org) / slots.size();
			}
		else
			{
			TreeNode node = new TreeNode(root());
			Slots slots = node.slots;
			int org = lowerBound(slots, new Slot(from));
			long fromadr = org < slots.size() ? slots.get(org).adrs[0] : node.next();
			int end = lowerBound(slots, new Slot(to));
			long toadr = end < slots.size() ? slots.get(end).adrs[0] : node.next();
			int n = slots.size() + 1;
			if (n > 20)
				return (float) (end - org) / n;
			// not enough keys in root, look at next level
			float pernode = (float) 1 / n;
			float result = keyfracpos(toadr, to, 1, (float) end / n, pernode) -
				keyfracpos(fromadr, from, 1, (float) org / n, pernode);
			return result < 0 ? 0 : result;
			}
	}
	private float keyfracpos(long adr, Record key,
		int level,		// to determine if tree or leaf
		float start,	// the fraction into the file where this node starts
		float nodefrac) {	// the fraction of the file under this node
		if (level < treelevels)
			{
			TreeNode node = new TreeNode(adr);
			Slots slots = node.slots;
			int i = lowerBound(slots, new Slot(key));
			assert slots.size() > 0;
			return start + (nodefrac * i) / slots.size();
			}
		else
			{
			LeafNode node = new LeafNode(adr);
			Slots slots = node.slots;
			int i = lowerBound(slots, new Slot(key));
			assert slots.size() > 0;
			return start + (nodefrac * i) / slots.size();
			}
		}

	private class LeafNode {
		Slots slots;
		long adr;
		boolean forWrite = false;

		LeafNode(long adr) {
			this(adr, Mode.OPEN);
		}
		LeafNode(long adr, Mode mode) {
			this.adr = adr;
			forWrite = mode == Mode.CREATE;
			ByteBuffer buf = forWrite ? dest.adrForWrite(adr) : dest.adr(adr);
			slots = new Slots(buf, mode);
			//assert isValid(mode);
		}
		void forWrite() {
			if (forWrite)
				return;
			forWrite = true;
			ByteBuffer buf = dest.adrForWrite(adr);
			assert ! buf.isReadOnly();
			slots = new Slots(buf);
		}
		Insert insert(Slot x)
			{
			forWrite();
			int i = lowerBound(slots, x);
			if (i < slots.size() && slots.get(i).equals(x))
				return Insert.DUP;
			else if (! slots.insert(i, x))
				return Insert.WONT_FIT;
			return Insert.OK;
			}
		boolean erase(Slot x)
			{
			forWrite();
			int i = lowerBound(slots, x);
			if (i >= slots.size() || ! slots.get(i).equals(x))
				return false;
			slots.remove(i);
			return true;
			}
		LeafNode split(Slot x)
			{
			// variable split
			int percent = 50;
			if (x.compareTo(slots.front()) < 0)
				percent = 75;
			else if (x.compareTo(slots.back()) > 0)
				percent = 25;
			LeafNode left = new LeafNode(dest.alloc(NODESIZE, Mmfile.OTHER),
					Mode.CREATE);

			forWrite();
			int rem = (left.slots.remaining() * percent) / 100;
			while (left.slots.remaining() > rem) {
				left.slots.add(slots.front());
				slots.remove(0);
			}

			// maintain linked list of leaves
			left.setPrev(prev());
			left.setNext(adr);
			setPrev(left.adr);
			if (left.prev() != 0)
				Slots.setBufNext(dest.adrForWrite(left.prev()), left.adr);
			return left;
			}
		void unlink() {
			if (prev() != 0)
				Slots.setBufNext(dest.adrForWrite(prev()), next());
			if (next() != 0)
				Slots.setBufPrev(dest.adrForWrite(next()), prev());
		}
		boolean isEmpty() {
			return slots.isEmpty();
		}
		long next() {
			return slots.next();
		}
		long prev() {
			return slots.prev();
		}
		void setNext(long next) {
			slots.setNext(next);
		}
		void setPrev(long prev) {
			slots.setPrev(prev);
		}

		// not void so we can call it with assert
		private boolean isValid(Mode mode) {
			assert prev() != TREENODE_PREV;
			int n = slots.size();
			assert n != 0 || mode == Mode.CREATE || adr == root_;
			if (n == 0)
				return true;
			Slot prev = slots.get(0);
			for (int i = 1; i < n; ++i) {
				Slot x = slots.get(i);
				assert prev.compareTo(slots.get(i)) < 0;
				prev = x;
			}
			return true;
		}
	} // end LeafNode

	private class TreeNode {
		Slots slots;
		long adr;
		boolean forWrite = false;

		TreeNode(long adr) {
			this(adr, Mode.OPEN);
		}
		TreeNode(long adr, Mode mode) {
			this.adr = adr;
			forWrite = mode == Mode.CREATE;
			ByteBuffer buf = forWrite ? dest.adrForWrite(adr) : dest.adr(adr);
			slots = new Slots(buf, mode);
			if (mode == Mode.CREATE)
				slots.setPrev(TREENODE_PREV);
			//assert isValid(mode);
		}
		void forWrite() {
			if (forWrite)
				return;
			forWrite = true;
			ByteBuffer buf = dest.adrForWrite(adr);
			assert ! buf.isReadOnly();
			slots = new Slots(buf);
		}

		// returns false if no room
		boolean insert(Record key, long off) {
			forWrite();
			Slot slot = new Slot(key, off);
			int i = lowerBound(slots, slot);
			verify(i == slots.size() || ! slots.get(i).key.equals(key)); // no dups
			return slots.insert(i, slot);
		}
		long find(Record key)
			{
			int i = lowerBound(slots, new Slot(key));
			return i < slots.size() ? slots.get(i).adrs[0] : slots.next();
			}

		void erase(Record key, long adr) {
			// NOTE: erases the first key >= key
			// this is so Btree erase can use target key
			//			if (slots.size() == 0) {
			//				slots.setNext(0);
			//				return false;
			//			}
			forWrite();
			if (slots.size() == 0) {
				slots.setNext(0);
				return;
			}
			int slot = lowerBound(slots, new Slot(key));
			if (slot == slots.size()) {
				assert adr == slots.next();
				--slot;
				slots.setNext(slots.get(slot).adrs[0]);
			} else
				assert adr == slots.get(slot).adrs[0];
			slots.remove(slot);
		}
		TreeNode split(Record key) {
			forWrite();
			int percent = 50;
			if (key.compareTo(slots.front().key) < 0)
				percent = 75;
			else if (key.compareTo(slots.back().key) > 0)
				percent = 25;
			long leftoff = dest.alloc(NODESIZE, Mmfile.OTHER);
			TreeNode left = new TreeNode(leftoff, Mode.CREATE); // create new treenode
			int n = slots.size();
			int nright = (n * percent) / 100;
			// move first part of right keys to left
			left.slots.add(slots, 0, slots.size() - nright);
			slots.remove(0, slots.size() - nright);
			return left;
			}
		boolean isEmpty() {
			return slots.isEmpty() && slots.next() == 0;
			}

		long next() {
			return slots.next();
		}
		void setNext(long next) {
			forWrite();
			slots.setNext(next);
		}

		private boolean isValid(Mode mode) {
			if (slots.prev() != TREENODE_PREV)
				return false;
			int n = slots.size();
			assert !isEmpty() || mode == Mode.CREATE;
			if (n == 0)
				return true;
			assert next() != 0;
			Slot prev = slots.get(0);
			for (int i = 1; i < n; ++i) {
				Slot x = slots.get(i);
				assert prev.compareTo(slots.get(i)) < 0;
				prev = x;
			}
			return true;
		}
	} // end TreeNode

	public long root() {
		if (root_ == 0)
			{
			verify(nnodes == 0);
			++nnodes;
			root_ = dest.alloc(NODESIZE, Mmfile.OTHER);
			new Slots(dest.adr(root_), Mode.CREATE);
			}
		verify(root_ >= 0);
		return root_;
	}

	public Iter first() {
		long adr = root();
		for (int i = 0; i < treelevels; ++i)
			adr = new TreeNode(adr).slots.front().adrs[0];
		LeafNode leaf = new LeafNode(adr);
		verify(leaf.prev() == 0);
		if (adr == root() && leaf.isEmpty())
			return new Iter();
		return new Iter(adr, leaf.slots.front());
	}
	public Iter last() {
		long adr = root();
		for (int i = 0; i < treelevels; ++i)
			adr = new TreeNode(adr).slots.next();
		LeafNode leaf = new LeafNode(adr);
		verify(leaf.next() == 0);
		if (adr == root() && leaf.isEmpty())
			return new Iter();
		return new Iter(adr, leaf.slots.back());
	}
	public Iter locate(Record key) {
		return new Iter(key);
	}
	public class Iter {
		long adr; // offset of current node
		Slot cur;
		long valid = modified;

		private Iter() { // end
			adr = 0;
			cur = null;
		}
		private Iter(long adr, Slot slot) {
			this.adr = adr;
			cur = slot.dup();
		}
		private Iter(Record key) {
			seek(key);
		}

		public boolean eof() {
			return adr == 0;
		}
		public void seteof() {
			adr = 0;
		}

		public Slot cur() {
			return cur;
		}
		public Record key() {
			return cur.key;
		}

		public Iter next() {
			if (adr == 0)
				return this;
			if (modified != valid && ! seek(cur.key))
				return this;	// key has been erased so we're on the next one
			LeafNode leaf = new LeafNode(adr);
			int t = upperBound(leaf.slots, cur);
			if (t < leaf.slots.size())
				cur = leaf.slots.get(t).dup();
			else if ((adr = leaf.next()) != 0)
				cur = new LeafNode(adr).slots.front().dup();
			return this;
		}
		public Iter prev() {
			if (adr == 0)
				return this;
			if (modified != valid)
				seek(cur.key);
			LeafNode leaf = new LeafNode(adr);
			int t = lowerBound(leaf.slots, cur);
			if (t > 0)
				cur = leaf.slots.get(--t).dup();
			else if ((adr = leaf.prev()) != 0)
				cur = new LeafNode(adr).slots.back().dup();
			return this;
		}
		public boolean seek(Record key) {
			adr = root();
			for (int i = 0; i < treelevels; ++i)
				adr = new TreeNode(adr).find(key);
			LeafNode leaf = new LeafNode(adr);
			if (treelevels == 0 && leaf.slots.size() == 0) {
				adr = 0; // empty btree
				return false;
			}
			int t = lowerBound(leaf.slots, new Slot(key));
			valid = modified;
			boolean found;
			if (t == leaf.slots.size()) {
				cur = leaf.slots.get(--t).dup();
				next();
				found = false;
			} else {
				found = (key == leaf.slots.get(t).key);
				cur = leaf.slots.get(t).dup();
			}
			return found;
		}
	}

	public void print() {
		print(root(), 0);
	}
	private void print(long adr, int level) {
		if (level < treelevels)
			{
			for (int j = 0; j < level; ++j)
				System.out.print("    ");
			System.out.println(adr + ":");
			TreeNode tn = new TreeNode(adr);
			int i = 0;
			for (; i < tn.slots.size(); ++i)
				{
				print(tn.slots.get(i).adrs[0], level + 1);
				for (int j = 0; j < level; ++j)
					System.out.print("    ");
				System.out.println(i + ": " + tn.slots.get(i).key.get(0));
				}
			if (i == 0)
				{
				for (int j = 0; j < level; ++j)
					System.out.print("    ");
				System.out.println("-o-");
				}
			print(tn.next(), level + 1);
			}
		else
			{
			LeafNode ln = new LeafNode(adr);
			for (int j = 0; j < level; ++j)
				System.out.print("    ");
			System.out.print(ln.prev() + " << " + ln.adr + " >> " + ln.next()
					+ "  ");
			int i = 0;
			for (; i < ln.slots.size(); ++i)
				System.out.print(ln.slots.get(i).key.get(0) + " ");
			System.out.println("");
			if (level == 0 && i == 0)
				System.out.print("- empty -");
			}
		if (level == 0)
			{ System.out.println(""); }
	}
}
