package suneido.language.jsdi.type;

import java.util.ArrayList;
import java.util.Iterator;

import javax.annotation.concurrent.NotThreadSafe;

@NotThreadSafe
public final class TypeList implements Iterable<TypeList.Entry> {

	//
	// TYPES
	//

	public static final class Entry {
		private final String name;
		private final Type type;

		public Entry(String name, Type type) {
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

	private final ArrayList<Entry> entries = new ArrayList<Entry>();
	private boolean isClosed = true;

	//
	// ACCESSORS
	//

	public boolean isClosed() {
		return isClosed;
	}

	//
	// MUTATORS
	//

	public void add(String name, Type type) {
		assert type != null;
		entries.add(new Entry(name, type));
		isClosed &= TypeId.BASIC == type.getTypeId();
	}

	final void resolve() throws ProxyResolveException {
		if (!isClosed) {
			for (Entry entry : entries) {
				final TypeId typeId = entry.type.getTypeId();
				if (typeId == TypeId.PROXY) {
					try {
						((Proxy) entry.type).resolve();
					} catch (ProxyResolveException e) {
						e.setMemberName(entry.name);
						throw e;
					}
				} else
					assert TypeId.BASIC == typeId : "Invalid type list entry";
			}
		}
	}

	//
	// INTERFACE: Iterable<Entry>
	//

	@Override
	public Iterator<Entry> iterator() {
		return new EntryIterator(entries.iterator());
	}
}
