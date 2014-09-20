/* Copyright 2013 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido.jsdi.type;

import static suneido.SuInternalError.unhandledEnum;
import suneido.SuContainer;
import suneido.SuInternalError;
import suneido.SuValue;
import suneido.jsdi.Buffer;
import suneido.jsdi.DllInterface;
import suneido.jsdi.JSDIException;
import suneido.jsdi.marshall.ByteCopier;
import suneido.jsdi.marshall.MarshallPlan;
import suneido.jsdi.marshall.MarshallPlan.StorageCategory;
import suneido.jsdi.marshall.MarshallPlanBuilder;
import suneido.jsdi.marshall.Marshaller;
import suneido.jsdi.marshall.NumberConversions;
import suneido.jsdi.marshall.ObjectConversions;
import suneido.runtime.BuiltinMethods;
import suneido.runtime.Ops;
import suneido.runtime.Params;

/**
 * <p>
 * Implements the JSDI {@code struct} type.
 * </p>
 * <p>
 * This class is <em>not</em> immutable because certain internal characteristics
 * may change depending on the current bound value of any members that are
 * late-binding types.
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

	//
	// CONSTRUCTORS
	//

	public Structure(String valueName, TypeList members) {
		super(TypeId.STRUCT, valueName, members);
		if (members.isEmpty()) {
			throw new JSDIException("structure must have at least one member");
		}
		if (members.isClosed()) {
			marshallPlan = members.makeMembersMarshallPlan();
		}
	}

	//
	// INTERNALS
	//

	private final MarshallPlan getMarshallPlan() {
		if (bind(0) || null == marshallPlan) {
			marshallPlan = typeList.makeMembersMarshallPlan();
		}
		return marshallPlan;
	}

	private Object copyOut(long structAddr) {
		final MarshallPlan p = getMarshallPlan();
		final Marshaller m = p.makeMarshaller();
		putMarshallOutInstruction(m);
		final StorageCategory storageCategory = p.getStorageCategory();
		switch (storageCategory) {
		case DIRECT: // intentional fall through
			copyOutDirect(structAddr, m.getData(), p.getSizeDirect());
			break;
		case INDIRECT:
			copyOutIndirect(structAddr, m.getData(), p.getSizeDirect(),
					m.getPtrArray());
			break;
		case VARIABLE_INDIRECT:
			copyOutVariableIndirect(structAddr, m.getData(), p.getSizeDirect(),
					m.getPtrArray(), m.getViArray(), m.getViInstArray());
			break;
		default:
			throw unhandledEnum(storageCategory);
		}
		m.rewind();
		return typeList.marshallOutMembers(m, null);
	}

	private Buffer toBuffer(SuContainer value, MarshallPlan plan) {
		Marshaller m = plan.makeMarshaller();
		typeList.marshallInMembers(m, value);
		final int N = plan.getSizeDirect();
		final Buffer result = new Buffer(N);
		new ByteCopier(m.getData(), 0, result.getInternalData())
				.copyFromLongArr(N);
		return result;
	}

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

	private Object fromBuffer(Buffer data, MarshallPlan plan) {
		final int N = plan.getSizeDirect();
		// NOTE: For this case, it would be more efficient to make a dedicated
		//       byte[] marshaller in the normal jsdi package and use the
		//       Buffer's internal data they way we were doing before.
		final long[] longData = new long[plan.getSizeDirectLongAligned()
				/ Long.BYTES];
		new ByteCopier(longData, 0, data.getInternalData()).copyToLongArr(N);
		Marshaller m = plan.makeUnMarshaller(longData);
		return marshallOut(m, null);
	}

	private Object fromBuffer(Buffer data) {
		final MarshallPlan p = getMarshallPlan();
		if (!p.isDirectOnly()) {
			throw new JSDIException(
					String.format(
							"jSuneido does not support %s(Buffer) because structure %1$s contains pointers",
							valueName()
					));
		} else if (data.length() < getSizeDirect()) {
			throw new JSDIException(
					String.format(
							"Buffer has length %d but size of %s is %d",
							data.length(), valueName(), getSizeDirect()
					));
		} else {
			return fromBuffer(data, p);
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
	public int getSizeDirect() {
		return getMarshallPlan().getSizeDirect();
	}

	@Override
	public int getAlignDirect() {
		return getMarshallPlan().getAlignDirect();
	}

	@Override
	public int getSizeIndirect() {
		return getMarshallPlan().getSizeIndirect();
	}

	@Override
	public int getVariableIndirectCount() {
		return typeList.getVariableIndirectCount();
	}

	@Override
	public void addToPlan(MarshallPlanBuilder builder, boolean isCallbackPlan) {
		builder.containerBegin(getAlignDirect());
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

	// skipMarshalling() deliberately not overridden for the time being since
	// a structure is currently only accessible through a LateBinding, which
	// uses the structure's element skipper.

	@Override
	public void putMarshallOutInstruction(Marshaller marshaller) {
		typeList.putMarshallOutInstructions(marshaller);
	}

	//
	// ANCESTOR CLASS: SuValue
	//

	private static final BuiltinMethods builtins = new BuiltinMethods(
			Structure.class);

	@Override
	public SuValue lookup(String method) {
		return builtins.lookup(method);
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
	 * {@link suneido.runtime.BuiltinMethods}.
	 * @param self The structure
	 * @return Integer size of the structure in bytes
	 * @see suneido.runtime.BuiltinMethods
	 */
	public static Object Size(Object self) {
		Structure struct = (Structure)self;
		struct.bind(0);
		return struct.getSizeDirect();
	}

	/**
	 * Built-in member modification method. <em>eg</em>:
	 * <code>(struct { long x }).Replace(ptr, "x", 13))</code>. The
	 * requirements for built-in methods are documented in
	 * {@link suneido.runtime.BuiltinMethods}.
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
		throw new SuInternalError("not implemented");
	}

	//
	// NATIVE METHODS
	//

	private static native void copyOutDirect(long structAddr, long[] data,
			int sizeDirect);

	private static native void copyOutIndirect(long structAddr, long[] data,
			int sizeDirect, int[] ptrArray);

	private static native void copyOutVariableIndirect(long structAddr,
			long[] data, int sizeDirect, int[] ptrArray, Object[] viArray,
			int[] viInstArray);
}
