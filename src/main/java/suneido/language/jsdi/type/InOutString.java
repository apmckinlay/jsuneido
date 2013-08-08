package suneido.language.jsdi.type;

import static suneido.language.jsdi.VariableIndirectInstruction.NO_ACTION;
import static suneido.language.jsdi.VariableIndirectInstruction.RETURN_JAVA_STRING;

import javax.annotation.concurrent.Immutable;

import suneido.language.Numbers;
import suneido.language.jsdi.Buffer;
import suneido.language.jsdi.DllInterface;
import suneido.language.jsdi.Marshaller;

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
		// values copied into a buffer. Thus the most likely branches are
		// either: (1) value is a Buffer, or (2) value is null or False
		if (null == value || Boolean.FALSE == value) {
			marshaller.putNullStringPtr(RETURN_JAVA_STRING);
		} else if (value instanceof Buffer) {
			marshaller.putStringPtr((Buffer)value, NO_ACTION);
		} else if (value instanceof CharSequence) {
			marshaller.putStringPtr(((CharSequence)value).toString(), RETURN_JAVA_STRING);
		} else if (Numbers.isZero(value)) {
			marshaller.putNullStringPtr(RETURN_JAVA_STRING);
		} else {
			super.marshallIn(marshaller, value);
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
}
