package suneido.language.jsdi.type;

import static suneido.language.jsdi.VariableIndirectInstruction.NO_ACTION;

import javax.annotation.concurrent.Immutable;

import suneido.language.jsdi.Buffer;
import suneido.language.jsdi.DllInterface;
import suneido.language.jsdi.JSDIException;
import suneido.language.jsdi.Marshaller;

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
		} else if (value instanceof CharSequence) {
			// Don't permit the conversion from string --> buffer because it is
			// silly: the whole point of a buffer is to allow the dll call to
			// modify it, but strings are immutable!
			throw new JSDIException("can't marshall a string into a buffer");
		} else {
			putNullStringPtrOrThrow(marshaller, value, NO_ACTION);
		}
	}

	@Override
	public Object marshallOut(Marshaller marshaller, Object value) {
		final Buffer buffer = value instanceof Buffer ? (Buffer)value : null;
		return marshaller.getStringPtrAlwaysByteArray(buffer);
	}
}
