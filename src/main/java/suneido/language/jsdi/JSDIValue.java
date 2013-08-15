package suneido.language.jsdi;

import suneido.SuValue;

/**
 * Intermediate class implementing a JSDI-specific {@link SuValue}
 * functionality.
 *
 * @author Victor Schappert
 * @since 20130815
 */
@DllInterface
public class JSDIValue extends SuValue {

	//
	// ANCESTOR CLASS: SuValue
	//

	@Override
	public String typeName() {
		String result = super.typeName();
		return result.substring(5); // remove 'jsdi.'
	}
}
