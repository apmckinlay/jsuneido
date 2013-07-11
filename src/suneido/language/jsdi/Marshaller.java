package suneido.language.jsdi;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

public final class Marshaller {

	//
	// DATA
	//

	public final byte[]     data;
	public final ByteBuffer directBuffer;
	public final ByteBuffer indirectBuffer;

	//
	// CONSTRUCTORS
	//

	Marshaller(int sizeDirect, int sizeIndirect) {
		data = new byte[sizeDirect + sizeIndirect];
		directBuffer = ByteBuffer.wrap(data, 0, sizeDirect);
		indirectBuffer = ByteBuffer.wrap(data, 0, sizeIndirect);
		directBuffer.order(ByteOrder.nativeOrder());
		indirectBuffer.order(ByteOrder.nativeOrder());
	}
}
