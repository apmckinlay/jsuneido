/* Copyright 2013 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido.jsdi.type;

import suneido.SuInternalError;
import suneido.jsdi.DllInterface;
import suneido.jsdi.JSDIException;
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
			throw new SuInternalError("suTypeName cannot be null");
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
	// ANCESTOR CLASS: Type
	//

	@Override
	public boolean bind(int level) {
		try {
			return typeList.bind(level);
		} catch (BindException e) {
			e.setParentName(valueName());
			throw new JSDIException(e);
		}
	}

	//
	// ANCESTOR CLASS: SuValue
	//

	@Override
	public final String valueName() {
		return valueName;
	}
}
