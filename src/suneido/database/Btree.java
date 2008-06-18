package suneido.database;

import static suneido.Suneido.verify;
import java.util.Iterator;

public class Btree implements Iterable<Slot> {
	final public static int MAXLEVELS = 20;
	final public static int TREENODE_PREV = Integer.MAX_VALUE & ~3;
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
	public int treelevels() {
		return treelevels;
	}
	public int nnodes() {
		return nnodes;
	}
	public boolean isEmpty() {
		if (nnodes == 0)
			return true;
		if (treelevels > 0)
			return false;
		verify(nnodes == 1);
		LeafNode leaf = new LeafNode(root());
		return leaf.isEmpty();
	}
	/**
	 * @return true if the structure is valid, false if not.
	 * Can be used via assert(isValid()) to avoid overhead in production.
	 */
	public boolean isValid() {
		long[] links = new long[] { 0, -1 };
		return isValid(root(), 0, links) 
			&& links[1] == 0; // final next should be 0
	}
	// links is checked and then set by each leaf node
	private boolean isValid(long adr, int level, long[] links) {
		if (level < treelevels)
			{
			TreeNode tn = new TreeNode(adr);
			if (! tn.isValid())
				return false;
			int i = 0;
			for (; i < tn.slots.size(); ++i)
				{
				if (! isValid(tn.slots.get(i).adrs[0], level + 1, links))
					return false;
				}
			if (! isValid(tn.next(), level + 1, links))
				return false;
			}
		else
			{
			LeafNode ln = new LeafNode(adr);
			if (links[0] != ln.prev())
				return false; // our prev is wrong
			if (links[1] != -1 && links[1] != adr)
				return false; // previous leaf node's next was wrong
			if (! ln.isValid())
				return false;
			links[0] = adr;
			links[1] = ln.next();
			}
		return true;
	}

	boolean insert(Slot x) { // returns false for duplicate key
		TreeNode[] nodes = new TreeNode[MAXLEVELS];
	
		// search down the tree
		long adr = root();
		int i; 
		for (i = 0; i < treelevels; ++i)
			{
			nodes[i] = new TreeNode(adr);
			adr = nodes[i].find(x.key);
			}
		LeafNode leaf = new LeafNode(adr);
	
		// insert the key & data into the leaf
		Insert status = leaf.insert(x);
		if (status != Insert.FULL)
			return status == Insert.OK;
		// split
		++modified;
		LeafNode left = leaf.split(x);
		++nnodes;
		verify(Insert.OK == (x.compareTo(left.slots.back()) <= 0 ? left.insert(x) : leaf.insert(x)));
		BufRecord key = left.slots.back().key.dup();
		adr = left.adr; 
	
		// insert up the tree as necessary
		for (--i; i >= 0; --i)
			{
			if (nodes[i].insert(key, adr))
				return true;
			// else split
			TreeNode tleft = nodes[i].split(key);
			++nnodes;
			verify(tleft.slots.back().key.compareTo(key) < 0 ? nodes[i].insert(key, adr) : tleft.insert(key, adr));
			key = tleft.slots.back().key.dup();
			tleft.setNext(tleft.slots.back().adrs[0]);
			tleft.slots.removeLast();
			adr = tleft.adr;
			}
		newRoot(key, adr);
		return true ;
	}
	private void newRoot(BufRecord key, long off) {
		long roff = dest.alloc(NODESIZE);
		TreeNode r = new TreeNode(roff, Mode.CREATE);
		++nnodes;
		r.insert(key, off);
		r.setNext(root_);
		root_ = roff;
		++treelevels;
		verify(treelevels < MAXLEVELS);
	}

	boolean erase(BufRecord key) {
		TreeNode[] nodes = new TreeNode[MAXLEVELS];
	
		// search down the tree
		long off = root();
		int i;
		for (i = 0; i < treelevels; ++i)
			{
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
		// erase empty nodes up the tree as necessary
		for (--i; i > 0; --i)
			{
			nodes[i].erase(key);
			if (! nodes[i].isEmpty())
				return true;
			--nnodes;
			}
		nodes[0].erase(key);
		off = root_;
		for (; treelevels > 0; --treelevels)
			{
			TreeNode node = new TreeNode(off);
			if (node.slots.size() > 0)
				break ;
			off = node.next();
			--nnodes;
			}
		root_ = off;
		return true;
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
			assert(isValid());
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
		LeafNode split(Slot x)
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
			left.setNext(adr);
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
		
		boolean isValid() {
			if (prev() == TREENODE_PREV)
				return false;
			int n = slots.size();
			if (n <= 1)
				return true;
			Slot prev = slots.get(0);
			for (int i = 1; i < n; ++i) {
				Slot x = slots.get(i);
				if (prev.compareTo(slots.get(i)) >= 0)
					return false;
				prev = x;
			}
			return true;
		}
	}
	
	private class TreeNode {
		Slots slots;
		long adr;
		
		TreeNode(long adr) {
			this(adr, Mode.OPEN);
		}
		TreeNode(long adr, Mode mode) {
			slots = new Slots(dest.adr(adr), mode);
			this.adr = adr;
			if (mode == Mode.CREATE)
				slots.setPrev(TREENODE_PREV);
			assert(isValid());
		}
		
		// returns false if no room
		boolean insert(BufRecord key, long off) {
			Slot slot = new Slot(key, off);
			int i = slots.lower_bound(slot);
			verify(i == slots.size() || ! slots.get(i).key.equals(key)); // no dups
			return slots.insert(i, slot);
		}
		long find(BufRecord key)
			{
			int i = slots.lower_bound(new Slot(key));
			return i < slots.size() ? slots.get(i).adrs[0] : slots.next();
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
		TreeNode split(BufRecord key) {
			int percent = 50;
			if (key.compareTo(slots.front().key) < 0)
				percent = 75;
			else if (key.compareTo(slots.back().key) > 0)
				percent = 25;
			long leftoff = dest.alloc(NODESIZE);
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
			slots.setNext(next);
		}

		boolean isValid() {
			if (slots.prev() != TREENODE_PREV)
				return false;
			int n = slots.size();
			if (n > 0 && next() == 0)
				return false;
			if (n <= 1)
				return true;
			Slot prev = slots.get(0);
			for (int i = 1; i < n; ++i) {
				Slot x = slots.get(i);
				if (prev.compareTo(slots.get(i)) >= 0)
					return false;
				prev = x;
			}
			return true;
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
		long adr = root();
		for (int i = 0; i < treelevels; ++i)
			adr = new TreeNode(adr).slots.front().adrs[0];
		LeafNode leaf = new LeafNode(adr);
		verify(leaf.prev() == 0);
		if (adr == root() && leaf.isEmpty()) {
			verify(leaf.next() == 0);
			return new Iter();
		}
		return new Iter(adr, leaf.slots.front());
	}
	private class Iter implements Iterator<Slot> {
		long adr; // offset of current node
		Slot cur;
		boolean cur_is_next; 
		
		Iter() {
			adr = 0; // end
			cur_is_next = false;
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
				System.out.println(i + ": " + tn.slots.get(i).key.getValue(1));
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
			System.out.print(ln.prev() + "<<" + ln.adr + ">>" + ln.next() + "  " );
			int i = 0;
			for (; i < ln.slots.size(); ++i)
				System.out.print(ln.slots.get(i).key.getValue(1) + " ");
			System.out.println("");
			if (level == 0 && i == 0)
				System.out.print("- empty -");
			}
		if (level == 0)
			{ System.out.println(""); }
	}
}
