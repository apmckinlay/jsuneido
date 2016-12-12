/* Copyright 2013 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido.jsdi.type;

import static suneido.jsdi.marshall.VariableIndirectInstruction.NO_ACTION;
import suneido.jsdi.*;
import suneido.jsdi.marshall.MarshallPlanBuilder;
import suneido.jsdi.marshall.Marshaller;
import suneido.jsdi.marshall.PrimitiveSize;
import suneido.jsdi.marshall.VariableIndirectInstruction;

/**
 * <p>
 * Ancestor class for the JSDI {@code string} and {@code [in] string} types.
 * </p>
 *
 * <p>
 * Note this and its ancestors implement types that, in C, are pointers to
 * zero-terminated strings. This class is not related to the classes that
 * implement zero- or non-zero-terminated strings in direct storage (<em>ie</em>
 * the JSDI types {@code string[n]} and {@code buffer[n]}). Those types are
 * implemented by {@link StringDirect}.
 * </p>
 * 
 * @author Victor Schappert
 * @since 20130710
 * @see InString
 * @see InOutString
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

	/**
	 * Marshalls a {@link Buffer} value into a {@code string} or 
	 * {@code [in] string} type. This is a potentially dangerous operation,
	 * since the underlying {@code dll} function will be expecting a zero-
	 * terminated string. Therefore, this method first checks the
	 * Buffer to ensure that it contains a zero character.
	 * @param marshaller Marshaller to marshall into
	 * @param buffer Non-{@code null} reference to a {@link Buffer}
	 * @since 20130808
	 */
	protected final void marshallBufferAsString(Marshaller marshaller,
			Buffer buffer) {
		if (! buffer.hasZero()) {
			throw new JSDIException(
					BufferType.IDENTIFIER_BUFFER
							+ " without zero terminator cannot safely be marshalled as "
							+ getDisplayName());
		}
		marshaller.putStringPtr(buffer, NO_ACTION);
	}

	//
	// ANCESTOR CLASS: Type
	//

	@Override
	public int getSizeDirect() {
		return PrimitiveSize.POINTER;
	}

	@Override
	public int getVariableIndirectCount() {
		return 1;
	}

	@Override
	public void addToPlan(MarshallPlanBuilder builder, boolean isCallbackPlan) {
		builder.ptrVariableIndirect();
	}

	@Override
	public void putMarshallOutInstruction(Marshaller marshaller) {
		marshaller
				.putViInstructionOnly(VariableIndirectInstruction.NO_ACTION);
	}
}
