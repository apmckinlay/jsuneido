/* Copyright 2013 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido.jsdi.type;

import static suneido.jsdi.marshall.VariableIndirectInstruction.RETURN_JAVA_STRING;

import javax.annotation.concurrent.Immutable;

import suneido.jsdi.Buffer;
import suneido.jsdi.DllInterface;
import suneido.jsdi.marshall.Marshaller;
import suneido.jsdi.marshall.VariableIndirectInstruction;

/**
 * <p>
 * Implements the JSDI {@code string} type.
 * </p>
 *
 * @author Victor Schappert
 * @since 20130718
 * @see BufferType
 * @see InString
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
