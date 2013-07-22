package suneido.language.jsdi;


/**
 * TODO: docs
 * 
 * @author Victor Schappert
 * @since 20130718
 */
@DllInterface
public final class StringConversions {

	public static byte[] stringToZeroTerminatedByteArray(String value) {
		final int N = value.length();
		byte[] b = new byte[N + 1];
		for (int k = 0; k < N; ++k) {
			b[k] = (byte)value.charAt(k);
		}
		return b;
	}
}
