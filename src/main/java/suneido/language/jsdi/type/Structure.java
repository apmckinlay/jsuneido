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
import suneido.language.jsdi.*;
import suneido.language.jsdi.dll.CallGroup;

/**
 * TODO: docs
 * <p>
 * This class is <em>not</em> immutable because certain characteristics may
 * change depending on the current resolved value of any members that are
 * proxies.
 * </p>
 * @author Victor Schappert
 * @since 20130625
 */
@DllInterface
public final class Structure extends ComplexType {

	//
	// DATA
	//

	private MarshallPlan marshallPlan;
	private CallGroup    callGroup;

	//
	// CONSTRUCTORS
	//

	Structure(String valueName, TypeList members) {
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

	private MarshallPlan getMarshallPlan() {
		if (resolve(0) || null == marshallPlan) {
			marshallPlan = typeList.makeMembersMarshallPlan();
			callGroup = CallGroup.fromTypeList(typeList, true);
		}
		return marshallPlan;
	}

	private Object copyOut(long structAddr) {
		Marshaller m = getMarshallPlan().makeMarshaller();
		putMarshallOutInstruction(m);
		switch (callGroup) {
		case FAST:
		case DIRECT: // intentional fall through
			copyOutDirect(structAddr, m.getData(), marshallPlan.getSizeDirect());
			break;
		case INDIRECT:
			copyOutIndirect(structAddr, m.getData(), marshallPlan.getSizeDirect(),
					m.getPtrArray());
			break;
		case VARIABLE_INDIRECT:
			copyOutVariableIndirect(structAddr, m.getData(),
					marshallPlan.getSizeDirect(), m.getPtrArray(),
					m.getViArray(), m.getViInstArray());
			break;
		default:
			throw new IllegalStateException("unhandled CallGroup in switch");
		}
		m.rewind();
		return typeList.marshallOutMembers(m, null);
	}

	private Buffer toBuffer(SuContainer value) {
		MarshallPlan p = getMarshallPlan();
		if (!p.isDirectOnly()) {
			throw new JSDIException(
					String.format(
							"jSuneido does not support %s(object) because structure %1$s contains pointers",
							valueName()
					));
		}
		Marshaller m = p.makeMarshaller();
		typeList.marshallInMembers(m, value);
		byte[] data = m.getData();
		return new Buffer(data, 0, data.length);
	}

	private Object fromBuffer(Buffer data) {
		MarshallPlan p = getMarshallPlan();
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
			Marshaller m = p.makeUnMarshaller(data.getInternalData());
			return marshallOut(m, null);
		}
	}

	//
	// ANCESTOR CLASS: ComplexType
	//

	boolean resolve(int level) {
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
	public boolean isClosed() {
		return typeList.isClosed();
	}

	@Override
	public String getDisplayName() {
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
	public int getSizeDirectIntrinsic() {
		return typeList.getSizeDirectIntrinsic();
	}

	@Override
	public int getSizeDirectWholeWords() {
		return PrimitiveSize.sizeWholeWords(getSizeDirectIntrinsic());
	}

	@Override
	public int getSizeIndirect() {
		return typeList.getSizeIndirect();
	}

	@Override
	public int getVariableIndirectCount() {
		return typeList.getVariableIndirectCount();
	}

	@Override
	public void addToPlan(MarshallPlanBuilder builder, boolean isCallbackPlan) {
		builder.containerBegin();
		typeList.addToPlan(builder, isCallbackPlan);
		skipper = builder.containerEnd();
	}

	@Override
	public void marshallIn(Marshaller marshaller, Object value) {
		if (null == value) {
			marshaller.skipComplexElement(skipper);
		} else {
			typeList.marshallInMembers(marshaller,
					ObjectConversions.containerOrThrow(value));
		}
	}

	@Override
	public Object marshallOut(Marshaller marshaller, Object oldValue) {
		return typeList.marshallOutMembers(marshaller, oldValue);
	}

	@Override
	public void putMarshallOutInstruction(Marshaller marshaller) {
		typeList.putMarshallOutInstructions(marshaller);
	}

	//
	// ANCESTOR CLASS: SuValue
	//

	private static final Map<String, SuCallable> builtins = BuiltinMethods
			.methods(Structure.class);

	@Override
	public SuValue lookup(String method) {
		SuValue result = builtins.get(method);
		return null != result ? result : new SuValue.NotFound(method);
	}

	@Override
	public Object call1(Object arg) {
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
	public String toString() {
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
		throw new RuntimeException("not implemented");
	}

	//
	// NATIVE METHODS
	//

	static native void copyOutDirect(long structAddr, byte[] data,
			int sizeDirect);

	static native void copyOutIndirect(long structAddr, byte[] data,
			int sizeDirect, int[] ptrArray);

	static native void copyOutVariableIndirect(long structAddr, byte[] data,
			int sizeDirect, int[] ptrArray, Object[] viArray, int[] viInstArray);
}
