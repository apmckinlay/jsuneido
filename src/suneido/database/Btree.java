package suneido.database;

import static suneido.Suneido.verify;
import java.nio.ByteBuffer;
import java.util.Iterator;

public class Btree implements Iterable<Slot> {
	final public static int MAXLEVELS = 20;
	final public static int NODESIZE = 4096 - Mmfile.OVERHEAD;
	enum Insert { OK, DUP, FULL };	// return values for insert
	
	private Destination dest;
	private long root_;
	private int treelevels;	// not including leaves
	private int nnodes;
	private long modified = 0;

	public Btree(Destination dest) {
		this.dest = dest;
		root_ = 0;
		treelevels = 0;
		nnodes = 0;
	}
	public Btree(Destination dest, long root, int treelevels, int nnodes) {
		this.dest = dest;
		verify(root >= 0); 
		root_ = root;
		verify(0 <= treelevels && treelevels < MAXLEVELS);
		this.treelevels = treelevels;
		verify(nnodes > 0);
		this.nnodes = nnodes;
	}

	boolean insert(Slot x) { // returns false for duplicate key
		TreeNode[] nodes = new TreeNode[MAXLEVELS];
	
		// search down the tree
		long off = root();
		int i; 
		for (i = 0; i < treelevels; ++i)
			{
			nodes[i] = new TreeNode(dest.adr(off));
			off = nodes[i].find(x.key);
			}
		LeafNode leaf = new LeafNode(off);
	
		// insert the key & data into the leaf
		Insert status = leaf.insert(x);
		if (status != Insert.FULL)
			return status == Insert.OK;
		// split
		++modified;
		LeafNode left = leaf.split(x, off);
		++nnodes;
		verify(Insert.OK == (x.compareTo(left.slots.back()) <= 0 ? left.insert(x) : leaf.insert(x)));
		BufRecord key = left.slots.back().key.dup();
		off = left.adr; 
	
		// insert up the tree as necessary
		for (--i; i >= 0; --i)
			{
			if (nodes[i].insert(key, off))
				return true;
			// else split
			left.adr = nodes[i].split(key);
			TreeNode tleft = new TreeNode(dest.adr(left.adr));
			++nnodes;
			verify(tleft.slots.back().key.compareTo(key) < 0 ? nodes[i].insert(key, off) : tleft.insert(key, off));
			key = tleft.slots.back().key.dup();
			tleft.setNext(tleft.slots.back().adrs[0]);
			tleft.slots.removeLast();
			off = left.adr;
			}
		newRoot(key, off);
		return true ;
	}
	private void newRoot(BufRecord key, long off) {
		long roff = dest.alloc(NODESIZE);
		TreeNode r = new TreeNode(dest.adr(roff), Mode.CREATE);
		++nnodes;
		r.insert(key, off);
		r.setNext(root_);
		root_ = roff;
		++treelevels;
		verify(treelevels < MAXLEVELS);
	}

	private class LeafNode {
		Slots slots;
		long adr;
		
		LeafNode(long adr) {
			this(adr, Mode.OPEN);
		}
		LeafNode(long adr, Mode mode) {
			this.adr = adr;
			slots = new Slots(dest.adr(adr), mode);
		}
		Insert insert(Slot x)
			{
			int i = slots.lower_bound(x);
			if (i < slots.size() && slots.get(i).equals(x))
				return Insert.DUP;
			else if (! slots.insert(i, x))
				return Insert.FULL;
			return Insert.OK;
			}
		boolean erase(Slot x)
			{
			int slot = slots.lower_bound(x);
			if (slot == slots.size() || ! slots.get(slot).equals(x))
				return false;
			slots.remove(slot);
			return true;
			}
		LeafNode split(Slot x, long off)
			{
			// variable split
			int percent = 50;
			if (x.compareTo(slots.front()) < 0)
				percent = 75;
			else if (x.compareTo(slots.back()) > 0)
				percent = 25;
			LeafNode left = new LeafNode(dest.alloc(NODESIZE), Mode.CREATE);
			int n = slots.size();
			int nright = (n * percent) / 100;
			// move first half of right keys to left
			left.slots.add(slots, 0, slots.size() - nright);
			slots.remove(0, slots.size() - nright);
			// maintain linked list of leaves
			left.setPrev(prev());
			left.setNext(off);
			setPrev(left.adr); 
			if (left.prev() != 0)
				Slots.setBufNext(dest.adr(left.prev()), left.adr);
			return left;
			}
		void unlink() {
			if (prev() != 0)
				Slots.setBufNext(dest.adr(prev()), next());
			if (next() != 0)
				Slots.setBufPrev(dest.adr(next()), prev());
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
	}
	
	private class TreeNode {
		Slots slots;
		
		TreeNode(ByteBuffer buf) {
			this(buf, Mode.OPEN);
		}
		TreeNode(ByteBuffer buf, Mode mode) {
			slots = new Slots(buf, mode);
		}
		
		// returns false if no room
		boolean insert(BufRecord key, long off) {
			int slot = slots.lower_bound(new Slot(key));
			verify(slot == slots.size() || slots.get(slot).key != key); // no dups
			return slots.insert(slot, new Slot(key, off));
		}
		long find(BufRecord key)
			{
			int slot = slots.lower_bound(new Slot(key));
			return slot < slots.size() ? slots.get(slot).adrs[0] : slots.next();
			}
		void erase(BufRecord key) {
			// NOTE: erases the first key >= key
			// this is so Btree erase can use target key
			if (slots.size() == 0) {
				slots.setNext(0);
				return ;
			}
			int slot = slots.lower_bound(new Slot(key));
			if (slot == slots.size()) {
				--slot;
				slots.setNext(slots.get(slot).adrs[0]);
			}
			slots.remove(slot);
		}
		long split(BufRecord key) {
			int percent = 50;
			if (key.compareTo(slots.front().key) < 0)
				percent = 75;
			else if (key.compareTo(slots.back().key) > 0)
				percent = 25;
			long leftoff = dest.alloc(NODESIZE);
			TreeNode left = new TreeNode(dest.adr(leftoff)); // create new treenode
			int n = slots.size();
			int nright = (n * percent) / 100;
			// move first part of right keys to left
			left.slots.add(slots, 0, slots.size() - nright);
			slots.remove(0, slots.size() - nright);
			return leftoff;
			}
		boolean isEmpty() { 
			return slots.isEmpty() && slots.next() == 0;
			}
		void setNext(long next) {
			slots.setNext(next);
		}
	}
	
	public long root() {
		if (root_ == 0)
			{
			verify(nnodes == 0);
			++nnodes;
			root_ = dest.alloc(NODESIZE);
			new Slots(dest.adr(root_), Mode.CREATE);
			}
		verify(root_ >= 0);
		return root_;
	}
	public Iterator<Slot> iterator() {
		long off = root();
		for (int i = 0; i < treelevels; ++i)
			off = new TreeNode(dest.adr(off)).slots.front().adrs[0];
		LeafNode leaf = new LeafNode(off);
		if (off == root() && leaf.isEmpty())
			return new Iter();
		return new Iter(off, leaf.slots.front());
	}
	private class Iter implements Iterator<Slot> {
		long adr; // offset of current node
		Slot cur;
		boolean cur_is_next; 
		
		Iter() {
			adr = 0; // end
		}
		Iter(long adr, Slot slot) {
			this.adr = adr;
			cur = slot;
			cur_is_next = true;
		}
		
		public boolean hasNext() {
			if (adr == 0)
				return false;
			if (cur_is_next)
				return true;
			advance();
			return adr != 0;
		}

		public Slot next() {
			verify(cur_is_next);
			cur_is_next = false;
			return cur;
		}
		
		public void advance() {
			cur_is_next = true;
//			if (modified != valid && ! seek(cur.key))
//				return ;	// key has been erased so we're on the next one
			LeafNode leaf = new LeafNode(adr);
			int t = leaf.slots.upper_bound(cur);
			if (t < leaf.slots.size())
				cur = leaf.slots.get(t);
			else if ((adr = leaf.next()) != 0)
				{
				leaf = new LeafNode(adr);
				cur = leaf.slots.front();
				}
		}

		public void remove() {
			throw new UnsupportedOperationException();
		}
	}
}
