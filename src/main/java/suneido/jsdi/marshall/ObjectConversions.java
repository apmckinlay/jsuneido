/* Copyright 2013 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido.jsdi.marshall;

import suneido.SuContainer;
import suneido.jsdi.DllInterface;
import suneido.jsdi.JSDIException;
import suneido.runtime.Ops;

/**
 * Utility methods to help marshalling code convert Java {@code Object}
 * references to Suneido {@code Object()} (<i>ie</i> Java {@link SuContainer})
 * references.
 *
 * @author Victor Schappert
 * @since 20130718
 */
@DllInterface
public final class ObjectConversions {

	private static void throwCantConvert(Object value) throws JSDIException {
		throw new JSDIException("can't convert " + Ops.typeName(value)
				+ " to object");
	}

	/**
	 * <p>
	 * Converts a Java {@link Object} reference, including a {@code null}
	 * reference, to an {@link SuContainer} reference using the algorithm
	 * described below.
	 * </p>
	 *
	 * <p>
	 * <ol>
	 * <li>
	 * if {@code value} is a {@code null} reference, returns a reference to a
	 * newly instantiated {@link SuContainer} whose vector capacity is
	 * {@code minCapacity};</li>
	 * <li>
	 * if {@code value} is convertible to an {@link SuContainer} reference via
	 * via {@link Ops#toContainer(Object) Ops.toContainer(value)}, returns the
	 * resulting container reference;</li>
	 * <li>
	 * otherwise throws a can't convert exception.</li>
	 * </ol>
	 * </p>
	 *
	 * @param value
	 *            Value to convert (or {@code null}
	 * @param vecCapacity
	 *            Vector capacity to use when creating a new container (
	 *            <em>only applies if {@code value} is {@code null})
	 * @return Container reference that may, or may not, be reference-equal to
	 *         {@code value}
	 * @since 20130718
	 * @throws JSDIException
	 *             If {@code value} is neither {@code null} nor a value that can
	 *             convert to {@link SuContainer}
	 * @see #containerOrThrow(Object)
	 */
	public static SuContainer containerOrThrow(Object value, int vecCapacity) {
		if (null == value) {
			return new SuContainer(vecCapacity);
		} else {
			return containerOrThrow(value);
		}
	}

	/**
	 * <p>
	 * Converts a Java {@link Object} reference that is expected to be non-
	 * {@code null} to an SuContainer reference via
	 * {@link Ops#toContainer(Object)}.
	 * </p>
	 *
	 * @param value
	 *            Reference to convert to a container reference
	 * @return Container reference that is reference-equal to {@code value}
	 * @throws JSDIException
	 *             If {@code value} is {@code null} or
	 *             {@link Ops#toContainer(Object) Ops.toContainer(value)}
	 *             returns {@code null}
	 */
	public static SuContainer containerOrThrow(Object value) {
		assert null != value;
		final SuContainer c = Ops.toContainer(value);
		if (null == c) {
			throwCantConvert(value);
		}
		return c;
	}
}
