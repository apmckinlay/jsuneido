package suneido.language.jsdi.type;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.TreeSet;

import javax.annotation.concurrent.NotThreadSafe;

import suneido.language.jsdi.JSDIException;
import suneido.language.jsdi.MarshallPlan;

@NotThreadSafe
public final class TypeList implements Iterable<TypeList.Entry> {

	//
	// TYPES
	//

	public static final class Entry {
		private final String name;
		private final Type type;

		private Entry(String name, Type type) {
			this.name = name;
			this.type = type;
		}

		public String getName() {
			return name;
		}

		public Type getType() {
			return type;
		}
	}

	public static final class Args {
		private final String memberType;
		private final ArrayList<Entry> entries;
		private final TreeSet<String> names; // deliberate
		private boolean isClosed;
		private boolean isUsed;

		public Args(String memberType, int size) {
			this.memberType = memberType;
			this.entries = new ArrayList<Entry>(size);
			this.names = new TreeSet<String>();
			this.isClosed = false;
			this.isUsed = false;
		}

		public void add(String name, Type type) {
			if (isUsed) {
				throw new IllegalStateException(
						"This Args object has already been used to construct a TypeList");
			}
			if (!names.add(name)) {
				throw new JSDIException("Duplicate " + memberType + ": '"
						+ name + "'");
			}
			entries.add(new Entry(name, type));
			isClosed &= TypeId.BASIC == type.getTypeId();
		}
	}

	private static final class EntryIterator implements Iterator<Entry> {
		private final Iterator<Entry> actualIterator;

		public EntryIterator(Iterator<Entry> actualIterator) {
			this.actualIterator = actualIterator;
		}

		@Override
		public boolean hasNext() {
			return actualIterator.hasNext();
		}

		@Override
		public Entry next() {
			return actualIterator.next();
		}

		@Override
		public void remove() {
			throw new UnsupportedOperationException(
					"TypeList may not be modified through its iterator (or at all)");
		}

	}

	//
	// DATA
	//

	private final ArrayList<Entry> entries;
	private final boolean isClosed;
	private MarshallPlan marshallPlan;

	//
	// CONSTRUCTORS
	//

	public TypeList(Args args) {
		args.isUsed = true;
		this.entries = args.entries;
		this.isClosed = args.isClosed;
		if (this.isClosed) {
			ArrayList<MarshallPlan> entryPlans = new ArrayList<MarshallPlan>(
					entries.size());
			for (Entry entry : entries) {
				entryPlans.add(entry.getType().getMarshallPlan());
			}
			this.marshallPlan = new MarshallPlan(entryPlans);
		} else {
			this.marshallPlan = null;
		}
	}

	//
	// ACCESSORS
	//

	public boolean isClosed() {
		return isClosed;
	}

	public MarshallPlan getMarshallPlan() {
		return marshallPlan;
	}

	//
	// MUTATORS
	//

	final boolean resolve(int level) throws ProxyResolveException {
		boolean changed = false;
		if (!isClosed) {
			for (Entry entry : entries) {
				final TypeId typeId = entry.type.getTypeId();
				if (typeId == TypeId.PROXY) {
					try {
						if (100 < level) {
							throw new JSDIException(
									"Type nesting limit exceeded - possible cvcle?");
						}
						changed &= ((Proxy) entry.type).resolve(level + 1);
					} catch (ProxyResolveException e) {
						e.setMemberName(entry.name);
						throw e;
					}
				} else
					assert TypeId.BASIC == typeId : "Invalid type list entry";
			}
			if (changed || null == marshallPlan) {
				// Only need to remake the Marshall Plan for a non-closed list
				// of types where at least one of the members types has changed.
				ArrayList<MarshallPlan> entryPlans = new ArrayList<MarshallPlan>(
						entries.size());
				for (Entry entry : entries) {
					entryPlans.add(entry.getType().getMarshallPlan());
				}
				marshallPlan = new MarshallPlan(entryPlans);
			}
		}
		assert null != marshallPlan;
		return changed;
	}

	//
	// INTERFACE: Iterable<Entry>
	//

	@Override
	public Iterator<Entry> iterator() {
		return new EntryIterator(entries.iterator());
	}
}
