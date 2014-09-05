package suneido.jsdi.type;

import suneido.jsdi.marshall.MarshallPlanBuilder;
import suneido.jsdi.marshall.Marshaller;


/**
 * <p>
 * Base class for a type that generally mimcs the behaviour of some other
 * underlying type.
 * </p>
 *
 * <p>
 * Do not confuse with {@link LateBinding}!
 * </p>
 *
 * @author Victor Schappert
 * @since 20140730
 */
public class Proxy extends Type {

	//
	// DATA
	//

	private final Type underlying;

	//
	// CONSTRUCTORS
	//

	protected Proxy(Type underlying) {
		super(underlying.getTypeId(), underlying.getStorageType());
		this.underlying = underlying;
	}

	//
	// ANCESTOR CLASS: Type
	//

	@Override
	public boolean isClosed() {
		return underlying.isClosed();
	}

	@Override
	public boolean isMarshallableToLong() {
		return underlying.isMarshallableToLong();
	}

	@Override
	public int getSizeDirect() {
		return underlying.getSizeDirect();
	}

	@Override
	public int getAlignDirect() {
		return underlying.getAlignDirect();
	}

	@Override
	public int getSizeIndirect() {
		return underlying.getSizeIndirect();
	}

	@Override
	public int getVariableIndirectCount() {
		return underlying.getVariableIndirectCount();
	}

	@Override
	public void addToPlan(MarshallPlanBuilder builder, boolean isCallbackPlan) {
		underlying.addToPlan(builder, isCallbackPlan);
	}

	@Override
	public void marshallIn(Marshaller marshaller, Object value) {
		underlying.marshallIn(marshaller, value);
	}

	@Override
	public Object marshallOut(Marshaller marshaller, Object oldValue) {
		return underlying.marshallOut(marshaller, oldValue);
	}

	@Override
	public void skipMarshalling(Marshaller marshaller) {
		underlying.skipMarshalling(marshaller);
	}

	@Override
	public void putMarshallOutInstruction(Marshaller marshaller) {
		underlying.putMarshallOutInstruction(marshaller);
	}

	@Override
	public void marshallInReturnValue(Marshaller marshaller) {
		underlying.marshallInReturnValue(marshaller);
	}

	@Override
	public Object marshallOutReturnValue(long returnValue, Marshaller marshaller) {
		return underlying.marshallOutReturnValue(returnValue, marshaller);
	}

	@Override
	public long marshallInToLong(Object value) {
		return underlying.marshallInToLong(value);
	}

	@Override
	public Object marshallOutFromLong(long marshalledData, Object oldValue) {
		return underlying.marshallOutFromLong(marshalledData, oldValue);
	}

	@Override
	public String getDisplayName() {
		return underlying.getDisplayName();
	}
}
