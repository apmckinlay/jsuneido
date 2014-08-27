/* Copyright 2013 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido.jsdi.type;

import static suneido.jsdi.marshall.VariableIndirectInstruction.NO_ACTION;

import javax.annotation.concurrent.Immutable;

import suneido.jsdi.*;
import suneido.jsdi.marshall.MarshallPlanBuilder;
import suneido.jsdi.marshall.Marshaller;
import suneido.language.Ops;

/**
 * <p>
 * Implements the JSDI {@code buffer} type.
 * </p>
 *
 * @author Victor Schappert
 * @since 20130718
 * @see InOutString
 * @see InString
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
