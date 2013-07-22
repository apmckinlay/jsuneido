package suneido.language.jsdi;

import suneido.SuContainer;
import suneido.language.Ops;

/**
 * TODO: docs
 * @author Victor Schappert
 * @since 20130718
 */
@DllInterface
public final class ObjectConversions {

	public static SuContainer containerOrThrow(Object value, int vecCapacity) {
		if (null == value) {
			return new SuContainer(vecCapacity);
		} else {
			final SuContainer c = Ops.toContainer(value);
			if (null != c) {
				return c;
			}
			else {
				throw new JSDIException("can't convert " + Ops.typeName(value)
						+ " to object");
			}
		}
	}
}
