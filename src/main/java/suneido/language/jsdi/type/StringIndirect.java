package suneido.language.jsdi.type;

import suneido.language.Numbers;
import suneido.language.jsdi.DllInterface;
import suneido.language.jsdi.MarshallPlanBuilder;
import suneido.language.jsdi.Marshaller;
import suneido.language.jsdi.StorageType;

/**
 * TODO: docs
 * @author Victor Schappert
 * @since 20130710
 * @see StringDirect
 */
@DllInterface
public abstract class StringIndirect extends StringType {

	//
	// CONSTRUCTORS
	//

	protected StringIndirect() {
		super(TypeId.STRING_INDIRECT, StorageType.POINTER);
	}

	//
	// INTERNALS
	//

	protected final void putNullStringPtrOrThrow(Marshaller marshaller, Object value,
			boolean expectStringBack) {
		if (null == value || Boolean.FALSE == value || Numbers.isZero(value)) {
			marshaller.putNullStringPtr(expectStringBack);
		} else {
			super.marshallIn(marshaller, value); // will throw
		}
	}

	//
	// ANCESTOR CLASS: Type
	//


	@Override
	public int getSizeDirectIntrinsic() {
		return PrimitiveSize.POINTER;
	}

	@Override
	public int getSizeDirectWholeWords() {
		return PrimitiveSize.pointerWholeWordBytes();
	}

	@Override
	public int getVariableIndirectCount() {
		return 1;
	}

	@Override
	public void addToPlan(MarshallPlanBuilder builder) {
		builder.variableIndirectPtr();
	}
}
