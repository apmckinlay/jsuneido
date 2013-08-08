package suneido.language.jsdi.type;

import static suneido.language.jsdi.VariableIndirectInstruction.NO_ACTION;

import javax.annotation.concurrent.Immutable;

import suneido.language.Ops;
import suneido.language.jsdi.*;

/**
 * TODO: docs
 * @author Victor Schappert
 * @since 20130718
 */
@DllInterface
@Immutable
public final class BufferType extends StringIndirect {

	//
	// CONSTRUCTORS
	//

	private BufferType() { }

	//
	// SINGLETON INSTANCE
	//

	public static final BufferType INSTANCE = new BufferType();

	//
	// ANCESTOR CLASS: Type
	//

	@Override
	public String getDisplayName() {
		return IDENTIFIER_BUFFER;
	}

	@Override
	public void marshallIn(Marshaller marshaller, Object value) {
		if (value instanceof Buffer) {
			marshaller.putStringPtr((Buffer)value, NO_ACTION);
		} else if (isNullPointerEquivalent(value)) {
			marshaller.putNullStringPtr(NO_ACTION);
		} else {
			// Don't permit the conversion from string --> buffer because it is
			// silly: the whole point of a buffer is to allow the dll call to
			// modify it, but Suneido strings are immutable!
			throw new JSDIException("can't marshall a " + Ops.typeName(value)
					+ " into a " + getDisplayName());
		}
	}

	@Override
	public Object marshallOut(Marshaller marshaller, Object value) {
		final Buffer buffer = value instanceof Buffer ? (Buffer)value : null;
		return marshaller.getStringPtrAlwaysByteArray(buffer);
	}

	@Override
	public void addToPlan(MarshallPlanBuilder builder, boolean isCallbackPlan) {
		if (isCallbackPlan) {
			// It doesn't make sense for a dll function to send a 'buffer' to
			// a callback because there is no 'protocol' by which the dll can
			// tell Suneido how big the buffer is.
			throwNotValidForCallback();
		}
		super.addToPlan(builder, isCallbackPlan);
	}
}
