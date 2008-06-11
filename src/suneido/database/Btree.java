package suneido.database;

import static suneido.Suneido.verify;
import java.nio.ByteBuffer;

public class Btree {
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
	
	private class LeafNode {
		Slots slots;
		
		LeafNode(Slots slots) {
			this.slots = slots;
			setNext(0);
			setPrev(0);
			}
		Insert insert(Slot x)
			{
			int slot = slots.lower_bound(x);
			if (slot < slots.size() && slots.get(slot) == x)
				return Insert.DUP;
			else if (! slots.insert(slot, x))
				return Insert.FULL;
			return Insert.OK;
			}
		boolean erase(Slot x)
			{
			int slot = slots.lower_bound(x);
			if (slot == slots.size() || slots.get(slot) != x)
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
	public long root() {
		if (root_ == 0)
			{
			verify(nnodes == 0);
			++nnodes;
			root_ = dest.alloc(NODESIZE);
			ByteBuffer buf = dest.adr(root_);
			Slots.setBufNext(buf, 0);
			Slots.setBufPrev(buf, 0);
			}
		verify(root_ >= 0);
		return root_;
	}
}
