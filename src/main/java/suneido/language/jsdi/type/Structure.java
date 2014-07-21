/* Copyright 2013 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido.language.jsdi.type;

import java.util.Map;

import suneido.SuContainer;
import suneido.SuValue;
import suneido.language.BuiltinMethods;
import suneido.language.Ops;
import suneido.language.Params;
import suneido.language.SuCallable;
import suneido.language.jsdi.Buffer;
import suneido.language.jsdi.CallGroup;
import suneido.language.jsdi.DllInterface;
import suneido.language.jsdi.JSDIException;
import suneido.language.jsdi.MarshallPlan;
import suneido.language.jsdi.MarshallPlanBuilder;
import suneido.language.jsdi.Marshaller;
import suneido.language.jsdi.NumberConversions;
import suneido.language.jsdi.ObjectConversions;
import suneido.language.jsdi.PrimitiveSize;

/**
 * <p>
 * Implements the JSDI {@code struct} type.
 * </p>
 * <p>
 * This class is <em>not</em> immutable because certain internal characteristics
 * may change depending on the current resolved value of any members that are
 * proxies.
 * </p>
 * @author Victor Schappert
 * @since 20130625
 */
@DllInterface
public abstract class Structure extends ComplexType {

	//
	// DATA
	//

	private MarshallPlan marshallPlan;
	private CallGroup    callGroup;

	//
	// CONSTRUCTORS
	//

	protected Structure(String valueName, TypeList members) {
		super(TypeId.STRUCT, valueName, members);
		if (members.isEmpty()) {
			throw new JSDIException("structure must have at least one member");
		}
		if (members.isClosed()) {
			marshallPlan = members.makeMembersMarshallPlan();
			callGroup = CallGroup.fromTypeList(members, true);
		}
	}

	//
	// INTERNALS
	//

	protected final MarshallPlan getMarshallPlan() {
		if (resolve(0) || null == marshallPlan) {
			marshallPlan = typeList.makeMembersMarshallPlan();
			callGroup = CallGroup.fromTypeList(typeList, true);
		}
		return marshallPlan;
	}

	protected final CallGroup getCallGroup() {
		// Only valid to be called after getMarshallPlan()
		return callGroup;
	}

	protected abstract Object copyOut(long structAddr);

	protected abstract Buffer toBuffer(SuContainer value, MarshallPlan plan);

	private Buffer toBuffer(SuContainer value) {
		final MarshallPlan p = getMarshallPlan();
		if (!p.isDirectOnly()) {
			throw new JSDIException(
					String.format(
							"jSuneido does not support %s(object) because structure %1$s contains pointers",
							valueName()
					));
		}
		return toBuffer(value, p);
	}

	protected abstract Object fromBuffer(Buffer data, MarshallPlan plan);

	private Object fromBuffer(Buffer data) {
		final MarshallPlan p = getMarshallPlan();
		if (!p.isDirectOnly()) {
			throw new JSDIException(
					String.format(
							"jSuneido does not support %s(Buffer) because structure %1$s contains pointers",
							valueName()
					));
		} else if (data.length() < getSizeDirectIntrinsic()) {
			throw new JSDIException(
					String.format(
							"Buffer has length %d but size of %s is %d",
							data.length(), valueName(), getSizeDirectIntrinsic()
					));
		} else {
			return fromBuffer(data, p);
		}
	}

	//
	// ANCESTOR CLASS: ComplexType
	//

	final boolean resolve(int level) {
		try {
			return typeList.resolve(level);
		} catch (ProxyResolveException e) {
			e.setMemberType("member");
			e.setParentName(valueName());
			throw new JSDIException(e);
		}
	}

	//
	// ANCESTOR CLASS: Type
	//

	@Override
	public final boolean isClosed() {
		return typeList.isClosed();
	}

	@Override
	public final String getDisplayName() {
		StringBuilder sb = new StringBuilder(128);
		sb.append("struct { ");
		for (TypeList.Entry entry : typeList) {
			sb.append(entry.getType().getDisplayName());
			sb.append(' ');
			sb.append(entry.getName().toString());
			sb.append("; ");
		}
		sb.append('}');
		return sb.toString();
	}

	@Override
	public final int getSizeDirectIntrinsic() {
		return typeList.getSizeDirectIntrinsic();
	}

	@Override
	public final int getSizeDirectWholeWords() {
		return PrimitiveSize.sizeWholeWords(getSizeDirectIntrinsic());
	}

	@Override
	public final int getSizeIndirect() {
		return typeList.getSizeIndirect();
	}

	@Override
	public final int getVariableIndirectCount() {
		return typeList.getVariableIndirectCount();
	}

	@Override
	public final void addToPlan(MarshallPlanBuilder builder,
			boolean isCallbackPlan) {
		builder.containerBegin();
		typeList.addToPlan(builder, isCallbackPlan);
		skipper = builder.containerEnd();
	}

	@Override
	public final void marshallIn(Marshaller marshaller, Object value) {
		if (null == value) {
			marshaller.skipComplexElement(skipper);
		} else {
			typeList.marshallInMembers(marshaller,
					ObjectConversions.containerOrThrow(value));
		}
	}

	@Override
	public final Object marshallOut(Marshaller marshaller, Object oldValue) {
		return typeList.marshallOutMembers(marshaller, oldValue);
	}

	@Override
	public final void putMarshallOutInstruction(Marshaller marshaller) {
		typeList.putMarshallOutInstructions(marshaller);
	}

	//
	// ANCESTOR CLASS: SuValue
	//

	private static final Map<String, SuCallable> builtins = BuiltinMethods
			.methods(Structure.class);

	@Override
	public final SuValue lookup(String method) {
		SuValue result = builtins.get(method);
		return null != result ? result : new SuValue.NotFound(method);
	}

	@Override
	public final Object call1(Object arg) {
		if (arg instanceof Number) {
			long ptr = NumberConversions.toPointer64(arg);
			if (0 != ptr) {
				return copyOut(ptr);
			}
		} else if (arg instanceof Buffer) {
			return fromBuffer((Buffer)arg);
		} else if (arg instanceof String) {
			// Struct(string) is deliberately not supported because, while
			// Buffers are currently guaranteed to be 1 byte per character,
			// there is no such guarantee for jSuneido strings.
			throw new JSDIException("jSuneido does not support Struct(string) - use a Buffer");
		} else {
			final SuContainer c = Ops.toContainer(arg);
			if (null != c) {
				return toBuffer(c);
			}
		}
		return Boolean.FALSE;
	}

	//
	// ANCESTOR CLASS: Object
	//

	@Override
	public final String toString() {
		return getDisplayName(); // Can be result of Suneido 'Display' built-in.
	}

	//
	// BUILT-IN METHODS
	//

	/**
	 * Built-in size method. <em>eg</em>: <code>(struct { }).Size()</code>. The
	 * requirements for built-in methods are documented in
	 * {@link suneido.language.BuiltinMethods}.
	 * @param self The structure
	 * @return Integer size of the structure in bytes
	 * @see suneido.language.BuiltinMethods
	 */
	public static Object Size(Object self) {
		Structure struct = (Structure)self;
		struct.resolve(0);
		return struct.getSizeDirectIntrinsic();
	}

	/**
	 * Built-in member modification method. <em>eg</em>:
	 * <code>(struct { long x }).Replace(ptr, "x", 13))</code>. The
	 * requirements for built-in methods are documented in
	 * {@link suneido.language.BuiltinMethods}.
	 * @param self The structure
	 * @param address Integer that is a valid pointer returned from some
	 * {@code dll} function and known to point to a valid structure of this
	 * type
	 * @param memberName String identifying the member to be modified
	 * @param value Value to assign to the member to be modified 
	 */
	@Params("address, member_name, value")
	@Deprecated
	public static Object Modify(Object self, Object address, Object memberName,
			Object value) {
		// TODO: I think that we have eliminated the need for struct.Modify
		//       and this method should probably be removed (check with APM).
		// TODO: This hasn't been acted on as of 20140719: what's the status?
		//       Can it be removed?
		throw new JSDIException("not implemented");
	}
}
