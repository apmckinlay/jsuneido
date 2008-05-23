package suneido;

/**
 * Used by database to store field values.
 * Provides a "view" onto a ByteBuffer.
 * <p>Format is:<br>
 * - one byte type = 'c', 's', 'l'<br>
 * - short n = number of fields<br>
 * - size (also referenced as offset[-1])<br>
 * - array of offsets<br>
 * size and array elements are of the type 
 * 
 * @author Andrew McKinlay
 *
 */
abstract class Record {
	abstract void add(byte[] data);
	abstract ByteBuffer get(int i);
}
