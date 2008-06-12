package suneido.database;

import static suneido.Suneido.verify;
import java.nio.ByteBuffer;
import java.util.Iterator;

import suneido.SuException;

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

	//TODO
	boolean insert(Slot x) { // returns false for duplicate key
		TreeNode[] nodes = new TreeNode[MAXLEVELS];
	
		// search down the tree
		long off = root();
		int i; 
		for (i = 0; i < treelevels; ++i)
			{
//			nodes[i] = (TreeNode*) dest->adr(off);
//			off = nodes[i]->find(x.key);
			}
		LeafNode leaf = new LeafNode(dest.adr(off));
	
		// insert the key & data into the leaf
		Insert status = leaf.insert(x);
//		if (status != Insert.FULL)
			return status == Insert.OK;
		// split
//		++modified;
//		long leftoff = leaf->split(dest, x, off);
//		LeafNode left = (LeafNode*) dest->adr(leftoff);
//		++nnodes;
//		verify(Insert.OK == (x <= left.slots.back() ? left.insert(x) : leaf.insert(x)));
//		Key key = keydup(left.slots.back().key);
//		off = leftoff; 
//	
//		// insert up the tree as necessary
//		for (--i; i >= 0; --i)
//			{
//			if (nodes[i].insert(key, off))
//				return true;
//			// else split
//			long leftoff = nodes[i].split(dest, key);
//			TreeNode left = (TreeNode*) dest->adr(leftoff);
//			++nnodes;
//			verify(left.slots.back().key < key ? nodes[i].insert(key, off) : left.insert(key, off));
//			key = keydup(left.slots.back().key);
//			left.lastoff = left.slots.back().adr;
//			left.slots.pop_back();
//			off = leftoff;
//			}
//		// create new root
//		long roff = dest.alloc(NODESIZE);
//		TreeNode r = new(dest->adr(roff)) TreeNode;
//		++nnodes;
//		r.insert(key, off);
//		r.lastoff = root_;
//		root_ = roff;
//		++treelevels;
//		verify(treelevels < MAXLEVELS);
//		return true ;
	}

	private class LeafNode {
		Slots slots;
		
		LeafNode(ByteBuffer buf) {
			this(new Slots(buf));
		}
		LeafNode(Slots slots) {
			this.slots = slots;
			setNext(0);
			setPrev(0);
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
			slots.erase(slot);
			return true;
			}
		long split(Slot x, long off)
			{
			// variable split
			int percent = 50;
			if (x.compareTo(slots.front()) < 0)
				percent = 75;
			else if (x.compareTo(slots.back()) > 0)
				percent = 25;
			long leftoff = dest.alloc(NODESIZE);
			Slots leftslots = new Slots(dest.adr(leftoff));
			LeafNode left = new LeafNode(leftslots);
			int n = slots.size();
			int nright = (n * percent) / 100;
			// move first half of right keys to left
			left.slots.add(slots, 0, slots.size() - nright);
			slots.erase(0, slots.size() - nright);
			// maintain linked list of leaves
			left.setPrev(prev());
			left.setNext(off);
			setPrev(leftoff); 
			if (left.prev() != 0)
				Slots.setBufNext(dest.adr(left.prev()), leftoff);
			return leftoff;
			}
		void unlink() {
			if (prev() != 0)
				Slots.setBufNext(dest.adr(prev()), next());
			if (next() != 0)
				Slots.setBufPrev(dest.adr(next()), prev());
		}
		boolean empty() {
			return slots.empty();
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
		//TODO 
	}
	
	public long root() {
		if (root_ == 0)
			{
			verify(nnodes == 0);
			++nnodes;
			root_ = dest.alloc(NODESIZE);
			ByteBuffer buf = dest.adr(root_);
			new Slots(dest.adr(root_), Slots.Mode.INIT);
			Slots.setBufNext(buf, 0);
			Slots.setBufPrev(buf, 0);
			}
		verify(root_ >= 0);
		return root_;
	}
	public Iterator<Slot> iterator() {
		long off = root();
//TODO
//		for (int i = 0; i < treelevels; ++i)
//			off = ((TreeNode*) (dest->adr(off)))->slots.front().adr;
		LeafNode leaf = new LeafNode(dest.adr(off));
		if (off == root() && leaf.empty())
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
//TODO
//			if (bt->modified > valid && ! seek(cur.key))
//				return ;	// key has been erased so we're on the next one
			// get next key sequentially
			LeafNode leaf = new LeafNode(dest.adr(adr));
			int t = leaf.slots.upper_bound(cur);
			if (t < leaf.slots.size())
				cur = leaf.slots.get(t);
			else if ((adr = leaf.next()) != 0)
				{
				LeafNode node = new LeafNode(dest.adr(adr));
				cur = node.slots.front();
				}
			cur_is_next = true;
		}

		public void remove() {
			throw new UnsupportedOperationException();
		}
	}
}
