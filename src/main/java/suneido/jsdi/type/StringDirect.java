/* Copyright 2013 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido.jsdi.type;

import javax.annotation.concurrent.Immutable;

import suneido.jsdi.*;
import suneido.jsdi.marshall.Marshaller;
import suneido.jsdi.marshall.PrimitiveSize;

/**
 * <p>
 * Type implementing the Suneido constructs {@code string[x]} and
 * {@code buffer[x]} for some positive integer {@code x}.
 * </p>
 * <p>
 * Suneido treats these entities like a special case. Where {@code string} or
 * {@code buffer} behave like <em>pointers</em> to zero-terminated or non-zero-
 * terminated strings, respectively, {@code string[x]} and {@code buffer[x]}
 * behave like {@code char[x]} (<em>ie</em> an in-place array) <em>except</em>
 * that they are marshalled to/from Suneido string or {@code Buffer} objects
 * instead of to/from Suneido {@code Object}-based arrays of one-character
 * strings.
 * </p>
 * 
 * @author Victor Schappert
 * @since 20130710
 * @see StringIndirect
 */
@DllInterface
@Immutable
public final class StringDirect extends StringType {

	//
	// DATA
	//

	private final int numChars;
	private final boolean isZeroTerminated;

	//
	// CONSTRUCTORS
	//

	/**
	 * <p>
	 * Constructs a type able to marshall a zero- or non-zero-terminated buffer
	 * of a fixed size.
	 * </p> 
	 *
	 * @param numChars Size of the string buffer, in 8-bit characters
	 * @param isZeroTerminated Whether the buffer is zero-terminated; if so,
	 *        the constructed instance has {@code string[numChars]} semantics;
	 *        if not, it is a {@code buffer[numChars]}
	 */
	public StringDirect(int numChars, boolean isZeroTerminated) {
		super(TypeId.STRING_DIRECT, StorageType.ARRAY);
		// NOTE: If we ever introduce wide-character strings and buffers, this
		//       class can probably handle it just by parameterizing the basic
		//       type.
		assert 0 < numChars : "String or buffer must have at least one character";
		this.numChars = numChars;
		this.isZeroTerminated = isZeroTerminated;
	}

	//
	// ANCESTOR CLASS: Type
	//

	@Override
	public String getDisplayName() {
		return new StringBuilder(12)
				.append(isZeroTerminated ? IDENTIFIER_STRING
						: IDENTIFIER_BUFFER).append('[').append(numChars)
				.append(']').toString();
	}

	@Override
	public int getSizeDirect() {
		return numChars * PrimitiveSize.INT8;
	}

	@Override
	public int getAlignDirect() {
		return PrimitiveSize.INT8;
	}

	@Override
	public void marshallIn(Marshaller marshaller, Object value) {
		if (null != value) {
			if (value instanceof Buffer) {
				final Buffer buffer = (Buffer)value;
				if (isZeroTerminated) {
					marshaller.putZeroTerminatedStringDirect(buffer, numChars);
				} else {
					marshaller.putNonZeroTerminatedStringDirect(buffer, numChars);
				}
			} else {
				final String str = value.toString();
				if (isZeroTerminated) {
					marshaller.putZeroTerminatedStringDirect(str, numChars);
				} else {
					// TODO: What if they pass a buffer instance?
					marshaller.putNonZeroTerminatedStringDirect(str, numChars);
				}
			}
		} else {
			marshaller.skipBasicArrayElements(1);
		}
	}

	@Override
	public Object marshallOut(Marshaller marshaller, Object oldValue) {
		return isZeroTerminated ? marshaller
				.getZeroTerminatedStringDirect(numChars) : marshaller
				.getNonZeroTerminatedStringDirect(numChars,
						oldValue instanceof Buffer ? (Buffer) oldValue : null);
	}
}
