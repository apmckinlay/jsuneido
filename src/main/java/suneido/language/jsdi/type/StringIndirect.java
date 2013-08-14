package suneido.language.jsdi.type;

import static suneido.language.jsdi.VariableIndirectInstruction.NO_ACTION;
import suneido.language.jsdi.*;

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
	public void addToPlan(MarshallPlanBuilder builder, boolean isCallbackPlan) {
		builder.variableIndirectPtr();
	}

	@Override
	public void putMarshallOutInstruction(Marshaller marshaller) {
		marshaller
				.putViInstructionOnly(VariableIndirectInstruction.NO_ACTION);
	}
}
