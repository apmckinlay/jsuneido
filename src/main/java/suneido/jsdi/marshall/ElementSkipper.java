/* Copyright 2013 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido.jsdi.marshall;

import suneido.jsdi.DllInterface;

/**
 * For a container type such as a {@code struct} or an array of {@code struct},
 * tells the {@link Marshaller} how to omit an instance of the type.
 * 
 * @author Victor Schappert
 * @since 20130725
 */
@DllInterface
public final class ElementSkipper {

	//
	// DATA
	//

	int nPos;
	int nPtr;

	//
	// CONSTRUCTORS
	//

	public ElementSkipper(int nPos, int nPtr) {
		this.nPos = nPos;
		this.nPtr = nPtr;
	}

	//
	// ANCESTOR CLASS: Object
	//

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder(24);
		sb.append("ElementSkipper[");
		sb.append(nPos);
		sb.append(", ");
		sb.append(nPtr);
		sb.append(']');
		return sb.toString();
	}
}
