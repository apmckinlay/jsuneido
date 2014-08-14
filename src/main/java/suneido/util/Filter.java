package suneido.util;

/**
 * Decides whether a value should be included in a set.
 *
 * @author Victor Schappert
 * @since 20140814
 * @param <T> Type of values that will be filtered
 */
public interface Filter<T> {

	/**
	 * Tests a value, returning {@code true} if it should be included in the set
	 * and {@code false} if it should be excluded.
	 *
	 * @param value Value to test
	 * @return True iff {@code value} should be included in the set
	 */
	public boolean include(T value);
}
