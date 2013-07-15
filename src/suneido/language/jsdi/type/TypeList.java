package suneido.language.jsdi.type;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.TreeSet;

import javax.annotation.concurrent.NotThreadSafe;

import suneido.SuContainer;
import suneido.language.jsdi.JSDIException;
import suneido.language.jsdi.MarshallPlan;
import suneido.language.jsdi.Marshaller;

/**
 * Immutable list of <code>&lt;name, {@link Type}&gt;</code> tuples which
 * represent the parameters of a <code>dll</code> or <code>callback</code>
 * function, or the members of a <code>struct</code>.
 * 
 * @author Victor Schappert
 * @since 20130625
 */
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

		@Override
		public String toString() {
			StringBuilder sb = new StringBuilder(128);
			return sb.append(getClass().getSimpleName()).append('[')
					.append(type.getDisplayName()).append(' ').append(name)
					.append(']').toString();
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

	private final class EntryIterator implements Iterator<Entry> {
		private int k = 0;

		@Override
		public boolean hasNext() {
			return k < entries.length;
		}

		@Override
		public Entry next() {
			return entries[k++];
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

	private final Entry[] entries;
	private final boolean isClosed;
	private MarshallPlan marshallPlan;

	//
	// CONSTRUCTORS
	//

	public TypeList(Args args) {
		args.isUsed = true;
		this.entries = args.entries.toArray(new Entry[args.entries.size()]);
		this.isClosed = args.isClosed;
		if (this.isClosed) {
			ArrayList<MarshallPlan> entryPlans = new ArrayList<MarshallPlan>(
					entries.length);
			for (Entry entry : entries) {
				entryPlans.add(entry.getType().getMarshallPlan());
			}
			this.marshallPlan = MarshallPlan.makeContainerPlan(entryPlans);
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

	/**
	 * TODO: docs
	 * @param values
	 * @return
	 * @see #countVariableIndirectMembers(SuContainer)
	 * @see Type#countVariableIndirect(Object)
	 */
	public int countVariableIndirectParams(Object[] values) {
		int count = 0;
		final int N = values.length;
		assert N == entries.length;
		for (int k = 0; k < N; ++k) {
			final Object value = values[k]; 
			if (null != value &&
				entries[k].type.getMarshallPlan().hasVariableIndirect()) {
				count += entries[k].type.countVariableIndirect(value);
			}
		}
		return count;
	}

	/**
	 * TODO: docs
	 * @param container
	 * @return
	 * @see #countVariableIndirectParams(Object[])
	 * @see Type#countVariableIndirect(Object)
	 */
	public int countVariableIndirectMembers(SuContainer container) {
		int count = 0;
		final int N = entries.length;
		for (int k = 0; k < N; ++k) {
			if (entries[k].type.getMarshallPlan().hasVariableIndirect()) {
				final Object value = container.mapGet(entries[k].name);
				if (null != value) {
					count += entries[k].type.countVariableIndirect(value);
				}
			}
		}
		return count;
	}

	public void marshallIn(Marshaller marshaller) {
		
	}

	/**
	 * Makes a string representation of the type list as a parameter list (names
	 * only, not types).
	 * @return A string representing the parameter names.
	 * @since 20130709
	 * @see FunctionSpec#params()
	 * @see #toParamsTypeString()
	 */
	public String toParamsNameString() {
		final int N = entries.length;
		if (0 < N) {
			StringBuilder result = new StringBuilder(10 * N);
			result.append('(').append(entries[0].name);
			for (int k = 1; k < N; ++k) {
				// Be consistent with FunctionSpec.params(), which doesn't
				// add a space after the comma...
				result.append(',').append(entries[k].name);
			}
			return result.append(')').toString();
		} else {
			return "()";
		}
	}

	/**
	 * Makes a string representation of the type list as a parameter list
	 * containing both the parameter types and parameter names.
	 * @return A string representing the parameter names.
	 * @since 20130711
	 * @see #toParamsNameString()
	 * @see FunctionSpec#params()
	 */
	public String toParamsTypeString() {
		final int N = entries.length;
		if (0 < N) {
			Entry entry = entries[0];
			StringBuilder result = new StringBuilder(20 * N);
			result.append('(').append(entry.type.getDisplayName()).append(' ')
					.append(entry.name);
			for (int k = 1; k < N; ++k) {
				entry = entries[k];
				result.append(", ").append(entry.type.getDisplayName())
						.append(' ').append(entry.name);
			}
			return result.append(')').toString();
		} else {
			return "()";
		}
	}

	//
	// MUTATORS
	//

	public final boolean resolve(int level) throws ProxyResolveException {
		boolean changed = false;
		if (!isClosed) {
			for (Entry entry : entries) {
				if (TypeId.PROXY == entry.type.getTypeId()) {
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
				}
			}
			if (changed || null == marshallPlan) {
				// Only need to remake the Marshall Plan for a non-closed list
				// of types where at least one of the members types has changed.
				ArrayList<MarshallPlan> entryPlans = new ArrayList<MarshallPlan>(
						entries.length);
				for (Entry entry : entries) {
					entryPlans.add(entry.getType().getMarshallPlan());
				}
				marshallPlan = MarshallPlan.makeContainerPlan(entryPlans);
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
		return new EntryIterator();
	}
}
