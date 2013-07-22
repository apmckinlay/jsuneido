package suneido.language.jsdi.type;

import suneido.language.Numbers;
import suneido.language.jsdi.DllInterface;
import suneido.language.jsdi.MarshallPlan;
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
		super(TypeId.STRING_INDIRECT, StorageType.POINTER, MarshallPlan
				.makeVariableIndirectPlan());
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
}
