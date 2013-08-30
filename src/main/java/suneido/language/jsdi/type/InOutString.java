package suneido.language.jsdi.type;

import static suneido.language.jsdi.VariableIndirectInstruction.RETURN_JAVA_STRING;

import javax.annotation.concurrent.Immutable;

import suneido.language.jsdi.Buffer;
import suneido.language.jsdi.DllInterface;
import suneido.language.jsdi.Marshaller;
import suneido.language.jsdi.VariableIndirectInstruction;

/**
 * TODO: docs
 * @author Victor Schappert
 * @since 20130718
 */
@DllInterface
@Immutable
public final class InOutString extends StringIndirect {

	//
	// CONSTRUCTORS
	//

	private InOutString() { }

	//
	// SINGLETON INSTANCE
	//

	public static final InOutString INSTANCE = new InOutString();

	
	//
	// ANCESTOR CLASS: Type
	//

	@Override
	public String getDisplayName() {
		return IDENTIFIER_STRING;
	}

	@Override
	public void marshallIn(Marshaller marshaller, Object value) {
		// The 'string' type is mostly used to receive string constants or
		// values copied into a buffer.
		if (value instanceof Buffer) {
			marshallBufferAsString(marshaller, (Buffer)value);
		} else if (isNullPointerEquivalent(value)) {
			marshaller.putNullStringPtr(RETURN_JAVA_STRING);
		} else {
			marshaller.putStringPtr(value.toString(), RETURN_JAVA_STRING);
		}
	}

	@Override
	public Object marshallOut(Marshaller marshaller, Object oldValue) {
		if (oldValue instanceof Buffer) {
			return marshaller.getStringPtrMaybeByteArray((Buffer)oldValue);
		} else {
			return marshaller.getStringPtr();
		}
	}

	@Override
	public void putMarshallOutInstruction(Marshaller marshaller) {
		marshaller
				.putViInstructionOnly(VariableIndirectInstruction.RETURN_JAVA_STRING);
	}

	@Override
	public void marshallInReturnValue(Marshaller marshaller) {
		marshaller.putNullStringPtr(RETURN_JAVA_STRING);
	}

	@Override
	public Object marshallOutReturnValue(long returnValue, Marshaller marshaller) {
		return marshaller.getStringPtr();
	}
}
