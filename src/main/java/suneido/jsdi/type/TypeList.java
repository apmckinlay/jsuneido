/* Copyright 2013 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido.jsdi.type;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.TreeSet;

import javax.annotation.concurrent.Immutable;
import javax.annotation.concurrent.NotThreadSafe;

import suneido.SuContainer;
import suneido.SuInternalError;
import suneido.jsdi.DllInterface;
import suneido.jsdi.Factory;
import suneido.jsdi.JSDIException;
import suneido.jsdi.marshall.MarshallPlan;
import suneido.jsdi.marshall.MarshallPlanBuilder;
import suneido.jsdi.marshall.Marshaller;
import suneido.jsdi.marshall.ObjectConversions;
import suneido.runtime.FunctionSpec;

/**
 * <p>
 * List of <code>&lt;name, {@link Type}&gt;</code> tuples which represent the
 * parameters of a <code>dll</code> or <code>callback</code> function, or the
 * members of a <code>struct</code>.
 * </p>
 * <p>
 * While some internal characteristics of a type list are subject to change over
 * its lifetime (<em>eg</em> as late-binding types are resolved), its external
 * characteristic, namely the list of {@link Entry} objects, is immutable.
 * </p>
 * 
 * @author Victor Schappert
 * @since 20130625
 */
@DllInterface
@NotThreadSafe
public abstract class TypeList implements Iterable<TypeList.Entry> {

	//
	// TYPES
	//

	/**
	 * Represents one <code>&lt;name, {@link Type}&gt;</code> tuple within a
	 * type list. An entry may represent a parameter to a {@code dll} or
	 * {@code callback}, or a member of a {@code struct}. 
	 *
	 * @author Victor Schappert
	 */
	@Immutable
	public static final class Entry {
		private final String  name;
		private       Type    type; // might be modified by a subclass
		private       boolean skip; // skip during marshall out param by value?

		private Entry(String name, Type type) {
			this.name = name;
			this.type = type;
			this.skip = false;
		}

		/**
		 * Returns the type list entry's name
		 * 
		 * @return Name
		 */
		public String getName() {
			return name;
		}

		/**
		 * Returns the type list entry's type
		 * 
		 * @return Type
		 */
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

	/**
	 * Mutable list of entries used to construct a type list. This "indirect"
	 * construction method is used because the type list's list of entries is
	 * immutable.
	 * 
	 * @author Victor Schappert
	 */
	@NotThreadSafe
	public static final class Args {
		private final ArrayList<Entry> entries;
		private final TreeSet<String> names; // deliberate
		private final boolean isParams;
		private boolean isClosed;
		private boolean isUsed;

		/**
		 * Constructs new type list arguments.
		 *
		 * @param isParams
		 *            Whether these arguments are being used to construct a
		 *            parameters type list ({@code true}) or a members type list
		 *            {@code false}.
		 * @param size
		 *            Hint indicating number of entries the type list
		 *            constructed from these Args will contain, for the purpose
		 *            of efficient memory allocation
		 */
		public Args(boolean isParams, int size) {
			this.isParams = isParams;
			this.entries = new ArrayList<>(size);
			this.names = new TreeSet<>();
			this.isClosed = true;
			this.isUsed = false;
		}

		/**
		 * Adds an entry to the arguments list of entries that will be used to
		 * construct the type list.
		 * 
		 * @param name
		 *            Name of the entry (<em>ie</em> parameter or member name)
		 * @param type
		 *            Type of the entry
		 */
		public void add(String name, Type type) {
			if (isUsed) {
				throw new SuInternalError(
						"this Args object has already been used to construct a TypeList");
			}
			if (!names.add(name)) {
				throw new JSDIException("duplicate "
						+ getMemberTypeString(isParams) + ": '" + name + "'");
			}
			entries.add(new Entry(name, type));
			isClosed &= type.isClosed();
		}
	
		/**
		 * Returns {@code true} iff this arugments list will construct a
		 * parameters type list.
		 *
		 * @return Whether these arguments are for parameters or members
		 * @since 20140801
		 */
		public boolean isParams() {
			return isParams;
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
			throw new SuInternalError(
					"TypeList may not be modified through its iterator (or at all)");
		}
	}

	//
	// DATA
	//

	private final Entry[] entries;
	private final boolean isClosed;
	private final boolean isParams; // true if params, false if members
	private int           variableIndirectCount;

	//
	// CONSTRUCTORS
	//

	/**
	 * <p>
	 * Constructs a new type list.
	 * </p>
	 * <p>
	 * Please use {@link Factory#makeTypeList(Args)} rather than this
	 * constructor.
	 * </p>
	 *
	 * @param args
	 *            Contains the <code>&lt;name, {@link Type}&gt;</code> tuples
	 *            (entries) for this type list
	 */
	public TypeList(Args args) {
		args.isUsed = true;
		this.entries = args.entries.toArray(new Entry[args.entries.size()]);
		this.isClosed = args.isClosed;
		this.isParams = args.isParams;
		if (isClosed) {
			updateStateAfterBind();
		} else {
			this.variableIndirectCount = -1;
		}
	}

	//
	// INTERNALS
	//

	private static String getMemberTypeString(boolean isParams) {
		return isParams ? "parameter" : "member";
	}

	private void countVariableIndirect() {
		int variableIndirectCount = 0;
		for (Entry entry : entries) {
			variableIndirectCount += entry.type.getVariableIndirectCount();
		}
		this.variableIndirectCount = variableIndirectCount;
	}

	private void figureOutSkippableParams() {
		for (Entry entry : entries) {
			// We can skip any value that doesn't directly OR indirectly point
			// to some other value.
			entry.skip = 0 == entry.type.getSizeIndirect()
					&& 0 == entry.type.getVariableIndirectCount();
		}
	}

	protected void updateStateAfterBind() {
		// Subclasses can overide this to update other things, but they need to
		// make sure they call superclass version.
		countVariableIndirect();
		if (isParams) {
			figureOutSkippableParams();
		}
	}

	protected final void modifyEntryType(int index, Type newType) {
		final Entry entry = entries[index];
		if (newType.isClosed() == entry.type.isClosed()
				&& newType.getTypeId() == entry.type.getTypeId()
				&& newType.getStorageType() == entry.type.getStorageType()) {
			entry.type = newType;
		} else {
			throw new SuInternalError(
					"new type must match closed, type id, and storage properties of old type");
		}
	}

	protected abstract MarshallPlanBuilder makeBuilder(
			int variableIndirectCount, boolean alignToWordBoundary);

	//
	// ACCESSORS
	//

	/**
	 * Indicates whether the type list is empty.
	 *
	 * @return {@code true} iff the type list contains no entries
	 * @since 20130725
	 */
	public final boolean isEmpty() {
		return 0 == entries.length;
	}

	/**
	 * Indicates whether this is a parameters type list.
	 *
	 * @return {@code true} iff this is a parameters list.
	 * @since 20140730
	 */
	public final boolean isParams() {
		return isParams;
	}

	/**
	 * <p>
	 * Indicates whether this type list contains only 'closed' types.
	 * </p>
	 * <p>
	 * The marshall plan for a 'closed' type list is fixed at Suneido language
	 * compile time. The list doesn't contain any late-binding types which need
	 * to be resolved at runtime.
	 * </p>
	 * 
	 * @return Whether this type list contains only closed types
	 * @see Type#isClosed()
	 */
	public final boolean isClosed() {
		return isClosed;
	}

	/**
	 * <p>
	 * Indicates whether this type list contains only types that can be
	 * marshalled to Java {@code long} values.
	 * </p>
	 **
	 * @return Whether this type list consists 100% of types that can be
	 *         marshalled to {@code long}
	 * @since 20140730
	 * @see Type#isMarshallableToLong()
	 */
	public final boolean isMarshallableToLong() {
		for (final Entry entry : entries) {
			if (! entry.type.isMarshallableToLong()) {
				return false;
			}
		}
		return true;
	}

	/**
	 * Returns the number of entries in the list.
	 *
	 * @return List size
	 * @since 20140801
	 * @see #get(int)
	 */
	public final int size() {
		return entries.length;
	}

	/**
	 * Subscripts the list of entries.
	 *
	 * @param i
	 *            Index of the entry to return: <code>code 0 &le; i &lt;</code>
	 *            {@link #size()}
	 * @return Returns the i<sup>th</sup> entry in the list.
	 * @since 20140801
	 * @see #size()
	 */
	public final Entry get(int i) {
		return entries[i];
	}

	/**
	 * <p>
	 * Returns the <em>current</em> total variable indirect count of the entries
	 * in this type list. Note that the return value may vary across calls to
	 * {@link #bind(int)} if this is not a closed list. 
	 * </p>
	 * 
	 * @return Sum of the variable indirect counts of all entries in the list
	 * @see Type#getVariableIndirectCount()
	 * @see #isClosed()
	 */
	public final int getVariableIndirectCount() {
		return variableIndirectCount;
	}

	/**
	 * Adds the entries in this list to a marshall plan.
	 *
	 * @param builder
	 *            Plan builder
	 * @param isCallbackPlan
	 *            Whether the plan is being built as the parameter list of a
	 *            {@code callback}
	 */
	public final void addToPlan(MarshallPlanBuilder builder,
			boolean isCallbackPlan) {
		for (Entry entry : entries)
			entry.type.addToPlan(builder, isCallbackPlan);
	}

	/**
	 * <p>
	 * Construct a {@link MarshallPlan} suitable for marshalling the entries of
	 * this type list as if they were parameters to a {@code stdcall} function.
	 * </p>
	 * <p>
	 * If a {@code dll} has a variable indirect return value (VS@20130809:
	 * currently, the only type supported is {@code string}; {@code resource}
	 * support could easily be added in principle), the parameter
	 * {@code hasViReturnValue} indicates that a variable indirect
	 * pseudo-parameter should be added to the end of the parameter list (it
	 * will occupy the very last position in the position list, pointer list,
	 * and variable indirect arrays). The native code places the return value
	 * in this pseudo-parameter. 
	 * </p>
	 *
	 * @param isCallbackPlan Whether the plan is being built for a
	 * {@code callback} rather than a {@code dll}; this parameter is needed to
	 * trigger an exception if an illegal type, such as {@code [in] string} is
	 * found as a direct or indirect argument to a {@code callback}
	 * @param hasViReturnValue Whether a pseudo-parameter should be added to
	 * receive a variable indirect return value (see discussion above)
	 * @return Marshall plan
	 * @see #makeMembersMarshallPlan()
	 */
	public final MarshallPlan makeParamsMarshallPlan(boolean isCallbackPlan,
			boolean hasViReturnValue) {
		assert isParams();
		int variableIndirectCount = getVariableIndirectCount();
		if (hasViReturnValue) {
			++variableIndirectCount;
		}
		final MarshallPlanBuilder builder = makeBuilder(variableIndirectCount,
				true);
		addToPlan(builder, isCallbackPlan);
		if (hasViReturnValue) {
			// Need to add another variable indirect slot for the variable
			// indirect return value pseudo-parameter.
			builder.ptrVariableIndirectPseudoParam();
		}
		return builder.makeMarshallPlan();
	}

	/**
	 * <p>
	 * Construct a {@link MarshallPlan} suitable for marshalling the members of
	 * this type list as if they were members of a C {@code struct}.
	 * </p>
	 * @return Marshall plan
	 * @since 20130812
	 * @see #makeParamsMarshallPlan(boolean, boolean)
	 */
	public final MarshallPlan makeMembersMarshallPlan() {
		assert !isParams();
		final MarshallPlanBuilder builder = makeBuilder(
				getVariableIndirectCount(), false);
		addToPlan(builder, false);
		return builder.makeMarshallPlan();
	}

	/**
	 * <p>
	 * Puts an argument list into a marshaller as if this type list represents a
	 * list of parameters.
	 * </p>
	 * 
	 * @param marshaller
	 *            Marshaller to put into
	 * @param args
	 *            List of arguments whose length is equal to the number of
	 *            entries in this type list
	 * @see #marshallOutParams(Marshaller, Object[])
	 * @see #marshallOutParams(Marshaller)
	 * @see #marshallInMembers(Marshaller, SuContainer)
	 */
	public final void marshallInParams(Marshaller marshaller, Object[] args) {
		final int N = entries.length;
		assert N == args.length;
		for (int k = 0; k < N; ++k) {
			entries[k].type.marshallIn(marshaller, args[k]);
		}
	}

	/**
	 * <p>
	 * Extracts an argument list from a marshaller as if this type list
	 * represents a list of parameters for a {@code dll} call.
	 * </p>
	 * <p>
	 * This method is used for marshalling out the arguments of a {@code dll}
	 * call, where the previous ("in") arguments are available. The companion
	 * method {@link #marshallOutParams(Marshaller)} is used for marshalling out
	 * the arguments of a {@code callback} invocation.
	 * </p>
	 * 
	 * @param marshaller
	 *            Marshaller to extract from
	 * @param args
	 *            List of arguments that was passed to
	 *            {@link #marshallInParams(Marshaller, Object[])}
	 * @since 20130719
	 * @see #marshallInParams(Marshaller, Object[])
	 * @see #marshallOutParams(Marshaller)
	 */
	public final void marshallOutParams(Marshaller marshaller, Object[] args) {
		final int N = entries.length;
		assert N == args.length;
		for (int k = 0; k < N; ++k) {
			final Entry entry = entries[k];
			final Type type = entry.type;
			if (entry.skip)
				type.skipMarshalling(marshaller);
			else
				type.marshallOut(marshaller, args[k]);
		}
	}

	/**
	 * <p>
	 * Extracts an argument list from a marshaller as if this type list
	 * represents a list of parameters for a {@code callback} invocation.
	 * </p>
	 * <p>
	 * This method is used for marshalling out the arguments of a
	 * {@code callback} invocation, where the previous ("in") arguments are not
	 * available because the invocation was made by native code. The companion
	 * method {@link #marshallOutParams(Marshaller, Object[])} is used for
	 * marshalling out the arguments of a {@code dll} call.
	 * </p>
	 * 
	 * @param marshaller
	 *            Marshaller to extract from
	 * @since 20130806
	 */
	public final Object[] marshallOutParams(Marshaller marshaller) {
		final int N = entries.length;
		Object[] result = new Object[N];
		for (int k = 0; k < N; ++k) {
			result[k] = entries[k].type.marshallOut(marshaller, null);
		}
		return result;
	}
	
	/**
	 * <p>
	 * Puts a Suneido {@code Object} into a marshaller as if this type list
	 * represents a list of structure members.
	 * </p>
	 * 
	 * @param marshaller
	 *            Marshaller to put into
	 * @param value
	 *            Suneido {@code Object}
	 * @since 20130717
	 * @see #marshallOutMembers(Marshaller, Object)
	 */
	public final void marshallInMembers(Marshaller marshaller, SuContainer value) {
		for (Entry entry : entries)
			entry.type.marshallIn(marshaller, value.mapGet(entry.name));
	}

	/**
	 * <p>
	 * Extracts a Suneido {@code Object} from a marshaller as if this type list
	 * represents a list of structure members.
	 * </p>
	 * 
	 * @param marshaller
	 *            Marshaller to extract from
	 * @param VALUE
	 *            Suneido {@code Object}
	 * @since 20130718
	 * @see #marshallInMembers(Marshaller, SuContainer)
	 */
	public final Object marshallOutMembers(Marshaller marshaller,
			Object oldValue) {
		final SuContainer c = ObjectConversions.containerOrThrow(oldValue, 0);
		if (c == oldValue) {
			for (Entry entry : entries) {
				oldValue = c.getIfPresent(entry.name);
				Object newValue = entry.type.marshallOut(marshaller, oldValue);
				if (!newValue.equals(oldValue)) {
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
	 * <p>
	 * In a situation in which a value is being marshalled out of the native
	 * side without being marshalled in first, ensures that the marshaller
	 * contains enough information to marshall the type out.
	 * </p>
	 *
	 * @param marshaller
	 * @see Type#putMarshallOutInstruction(Marshaller)
	 */
	public final void putMarshallOutInstructions(Marshaller marshaller) {
		for (Entry entry : entries)
			entry.type.putMarshallOutInstruction(marshaller);
	}

	/**
	 * Returns an array containing entry names.
	 * @return String array containing the entry names
	 * @since 20130715
	 * @see FunctionSpec#params()
	 * @see #toParamsTypeString()
	 */
	public final String[] getEntryNames() {
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
	public final String toParamsTypeString() {
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

	/**
	 * <p>
	 * Resolves any late-binding types directly-contained or
	 * indirectly-contained (via type trees rooted in directly-contained members
	 * of this list) in this type list to concrete types and returns a value
	 * indicating whether the concrete type tree has changed since the previous
	 * bind operation.
	 * </p>
	 * <p>
	 * Note that for closed type lists (<em>ie</em> {@link #isClosed()} returns
	 * {@code true}), this method will always do nothing and return
	 * {@code false}. This is because a closed type list by definition contains
	 * no late-binding types.
	 * </p>
	 *
	 * @param level
	 *            Nesting level, for prevention of infinite cycles such as would
	 *            occur if "X" is defined as <code>struct { Y y }</code> and "Y"
	 *            is defined as <code>struct { X x }</code>
	 * @return Whether the type tree has changed since the last call to this
	 *         method
	 * @throws BindException
	 *             If the name of a late-binding type contained in the type tree
	 *             of this type list cannot be resolved to a concrete type
	 * @see {@link LateBinding#bind(int)}
	 */
	public final boolean bind(int level) throws BindException {
		boolean changed = false;
		if (!isClosed) {
			for (Entry entry : entries) {
				if (TypeId.LATE_BINDING == entry.type.getTypeId()) {
					try {
						if (100 < level) {
							throw new JSDIException(
									"type nesting limit exceeded - possible cycle?");
						}
						changed |= entry.type.bind(level + 1);
					} catch (BindException e) {
						e.setMemberName(entry.name);
						e.setMemberType(getMemberTypeString(isParams));
						throw e;
					}
				}
			}
			if (changed) {
				// Only need to recompute state for a non-closed list of types
				// where at least one of the members types has changed.
				updateStateAfterBind();
			}
		}
		return changed;
	}

	//
	// INTERFACE: Iterable<Entry>
	//

	@Override
	public final Iterator<Entry> iterator() {
		return new EntryIterator();
	}
}
