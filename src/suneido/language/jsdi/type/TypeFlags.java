package suneido.language.jsdi.type;

public final class TypeFlags {

	public static final int FLAG_CLOSED   = 0x01;
	public static final int FLAG_BASIC    = 0x02;
	public static final int FLAG_COMPLEX  = 0x04;
	public static final int FLAG_INT      = 0x08;
	public static final int FLAG_SIMPLE   = 0x10;
	public static final int FLAG_POINTER  = 0x20;
	public static final int FLAG_ARRAY    = 0x40;
	public static final int BASIC_FLAGS   = FLAG_BASIC | FLAG_CLOSED;
	public static final int INT_FLAGS     = BASIC_FLAGS | FLAG_INT;
	public static final int NUM_FLAG_BITS = 7;

	private TypeFlags() { }
}
