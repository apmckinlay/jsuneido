package suneido.language.jsdi.type;

import static suneido.language.jsdi.VariableIndirectInstruction.NO_ACTION;

import javax.annotation.concurrent.Immutable;

import suneido.language.jsdi.Buffer;
import suneido.language.jsdi.DllInterface;
import suneido.language.jsdi.MarshallPlanBuilder;
import suneido.language.jsdi.Marshaller;

/**
 * TODO: docs
 * @author Victor Schappert
 * @since 20130718
 */
@DllInterface
@Immutable
public final class InString extends StringIndirect {

	//
	// CONSTRUCTORS
	//

	private InString() { }

	//
	// SINGLETON INSTANCE
	//

	public static final InString INSTANCE = new InString();

	
	//
	// ANCESTOR CLASS: Type
	//

	@Override
	public String getDisplayName() {
		return "[in] " + IDENTIFIER_STRING;
	}

	@Override
	public void marshallIn(Marshaller marshaller, Object value) {
		if (value instanceof Buffer) {
			marshallBufferAsString(marshaller, (Buffer)value);
		} else if (isNullPointerEquivalent(value)) {
			marshaller.putNullStringPtr(NO_ACTION);
		} else {
			marshaller.putStringPtr(value.toString(), NO_ACTION);
		}
	}

	@Override
	public Object marshallOut(Marshaller marshaller, Object oldValue) {
		// Do nothing, since we don't care about any changes to the value.
		marshaller.skipStringPtr();
		return null == oldValue ? Boolean.FALSE : oldValue;
	}

	@Override
	public void addToPlan(MarshallPlanBuilder builder, boolean isCallbackPlan) {
		if (isCallbackPlan) {
			// It doesn't make sense for a dll function to send an '[in] string'
			// to a callback since marshalling out an '[in] string' is a no-op.
			throwNotValidForCallback();
		}
		super.addToPlan(builder, isCallbackPlan);
	}
}
