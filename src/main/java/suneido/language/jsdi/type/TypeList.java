package suneido.language.jsdi.type;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.TreeSet;

import javax.annotation.concurrent.NotThreadSafe;

import suneido.SuContainer;
import suneido.language.FunctionSpec;
import suneido.language.Ops;
import suneido.language.jsdi.*;

/**
 * Immutable list of <code>&lt;name, {@link Type}&gt;</code> tuples which
 * represent the parameters of a <code>dll</code> or <code>callback</code>
 * function, or the members of a <code>struct</code>.
 * 
 * @author Victor Schappert
 * @since 20130625
 */
@DllInterface
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
		private int numMarshallableToJSDILong;

		public Args(String memberType, int size) {
			this.memberType = memberType;
			this.entries = new ArrayList<Entry>(size);
			this.names = new TreeSet<String>();
			this.isClosed = true;
			this.isUsed = false;
			this.numMarshallableToJSDILong = 0;
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
			isClosed &= type.isClosed();
			if (type.isMarshallableToJSDILong()) {
				++numMarshallableToJSDILong;
			}
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
	private final int     numMarshallableToJSDILong;
	private MarshallPlan  marshallPlan;

	//
	// CONSTRUCTORS
	//

	public TypeList(Args args) {
		args.isUsed = true;
		this.entries = args.entries.toArray(new Entry[args.entries.size()]);
		this.isClosed = args.isClosed;
		this.numMarshallableToJSDILong = args.numMarshallableToJSDILong;
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

	public int size() {
		return entries.length;
	}

	/**
	 * <p>
	 * Indicates whether this type list contains only 'closed' types.
	 * </p>
	 * <p>
	 * The marshall plan for a 'closed' type is fixed at compile time. The type
	 * doesn't contain any proxies which need to be resolved at runtime.
	 * </p>
	 * 
	 * @return Whether this type list contains only closed types
	 * @see Type#isClosed()
	 */
	public boolean isClosed() {
		return isClosed;
	}

	public boolean isFastMarshallable() {
		return numMarshallableToJSDILong == entries.length;
	}

	public MarshallPlan getMarshallPlan() {
		return marshallPlan;
	}

	// TODO: docs
	public void marshallInParams(Marshaller marshaller, Object[] args) {
		final int N = entries.length;
		assert N == args.length;
		for (int k = 0; k < N; ++k) {
			entries[k].type.marshallIn(marshaller, args[k]);
		}
	}

	// TODO: docs -- since 20130719
	public void marshallOutParams(Marshaller marshaller, Object[] args) {
		final int N = entries.length;
		assert N == args.length;
		for (int k = 0; k < N; ++k) {
			final Type type = entries[k].type;
			if (TypeId.BASIC != type.getTypeId()) {
				type.marshallOut(marshaller, args[k]);
			}
		}
	}
	
	public int[] marshallInParamsFast(Object[] args) {
		final int N = entries.length;
		assert N == args.length && N == numMarshallableToJSDILong;
		int[] marshalledArgs = new int[N];
		for (int k = 0; k < N; ++k) {
			entries[k].type.marshallInToJSDILong(marshalledArgs, k, args[k]);
		}
		return marshalledArgs;
	}

	// TODO: docs -- since 20130717
	public void marshallInMembers(Marshaller marshaller, Object value) {
		final SuContainer c = Ops.toContainer(value);
		if (null == c) {
			marshaller.skipComplexArrayElements(1, marshallPlan);
		} else for (Entry entry : entries) {
			entry.type.marshallIn(marshaller, c.get(entry.name));
		}
	}

	// TODO: docs -- since 20130718
	public Object marshallOutMembers(Marshaller marshaller, Object oldValue) {
		final SuContainer c = ObjectConversions.containerOrThrow(oldValue, 0);
		if (c == oldValue) {
			for (Entry entry : entries) {
				oldValue = c.getIfPresent(entry.name);
				Object newValue = entry.type.marshallOut(marshaller, oldValue);
				if (! newValue.equals(oldValue)) {
					c.put(entry.name, newValue);
				}
			}
		} else {
			for (Entry entry : entries) {
				Object newValue = entry.type.marshallOut(marshaller, null);
				c.put(entry.name, newValue);
			}
		}
		return c;
	}

	/**
	 * Returns an array containing entry names.
	 * @return String array containing the entry names
	 * @since 20130715
	 * @see FunctionSpec#params()
	 * @see #toParamsTypeString()
	 */
	public String[] getEntryNames() {
		final int N = entries.length;
		final String[] paramNames = new String[N];
		for (int k = 0; k < N; ++k) {
			paramNames[k] = entries[k].name;
		}
		return paramNames;
	}

	/**
	 * Makes a string representation of the type list as a parameter list
	 * containing both the parameter types and parameter names.
	 * @return A string representing the parameter names.
	 * @since 20130711
	 * @see FunctionSpec#params()
	 * @see #getEntryNames()
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
