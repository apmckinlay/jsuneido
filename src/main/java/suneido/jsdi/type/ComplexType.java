/* Copyright 2013 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido.jsdi.type;

import suneido.jsdi.DllInterface;
import suneido.jsdi.StorageType;
import suneido.jsdi.marshall.ElementSkipper;

/**
 * <p>
 * Base class for user-defined types.
 * </p>
 *
 * <p>
 * User-defined types may be {@code struct} or {@code callback} types. They have
 * two commonalities. First, they may optionally be given a name by the Suneido
 * programmer. Second, they are composed of an order list of other types
 * ({@code struct} member types or {@code callback} parameter types).
 * </p>
 *
 * @author Victor Schappert
 * @since 20130625
 */
@DllInterface
public abstract class ComplexType extends Type {

	//
	// DATA
	//

	private   final String         valueName;
	protected final TypeList       typeList;
	protected       ElementSkipper skipper; // only valid to last addToPlan()

	//
	// CONSTRUCTORS
	//

	protected ComplexType(TypeId typeId, String valueName, TypeList typeList) {
		super(typeId, StorageType.VALUE);
		if (null == valueName)
			throw new IllegalArgumentException("suTypeName cannot be null");
		this.valueName = valueName;
		this.typeList = typeList;
	}

	//
	// ACCESSORS
	//

	/**
	 * Returns the element skipper needed to skip past the storage allocated for
	 * this type in a {@link suneido.jsdi.marshall.Marshaller Marshaller}.
	 *
	 * @return Marshalling element skipper
	 * @since 20130725
	 */
	public final ElementSkipper getElementSkipper() {
		return skipper;
	}

	//
	// MUTATORS
	//

	/**
	 * <p>
	 * Binds any late-binding types participating in the type tree rooted at
	 * this complex type to their underlying types and returns a value
	 * indicating whether the type tree has changed since the last bind
	 * operation.
	 * </p>
	 * 
	 * @param level
	 *            Level of the tree at which the bind operation was requested (0
	 *            being the root)&mdash;necessary for detecting cycles in the
	 *            type hierarchy
	 * @return If the type tree has changed since the last bind, {@code true};
	 *         otherwise, {@code false}
	 * @throws BindException
	 *             If any part of the type tree could not be bound to its
	 *             underlying type
	 * @see LateBinding
	 * @see LateBinding#bind(int)
	 */
	boolean bind(int level) throws BindException {
		return false;
	}

	//
	// ANCESTOR CLASS: SuValue
	//

	@Override
	public final String valueName() {
		return valueName;
	}
}
