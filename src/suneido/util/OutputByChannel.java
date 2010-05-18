package suneido.util;

import static suneido.Suneido.fatal;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.util.*;

import javax.annotation.concurrent.NotThreadSafe;

/**
 * avoid problems with Nagle
 * by doing a single gathering write instead of multiple small writes
 * @author Andrew McKinlay
 */
@NotThreadSafe
public class OutputByChannel implements NetworkOutput {
	private final SocketChannel channel;
	private ByteBuffer[] bufs = new ByteBuffer[0];
	private final List<ByteBuffer> queue = new ArrayList<ByteBuffer>();
	private int n;

	public OutputByChannel(SocketChannel channel) {
		this.channel = channel;
	}

	public void add(ByteBuffer buf) {
		queue.add(buf);
	}

	public void write() {
		bufs = queue.toArray(bufs);
		n = queue.size();
		queue.clear();
		try {
			while (!isEmpty())
				channel.write(bufs, 0, n);
		} catch (IOException e) {
			fatal("network write error", e); // TODO
		}
		Arrays.fill(bufs, null);
	}

	private boolean isEmpty() {
		for (int i = 0; i < n; ++i)
			if (bufs[i].remaining() > 0)
				return false;
		return true; // everything written
	}

	public void write(ByteBuffer buf) {
		try {
			while (buf.remaining() > 0)
				channel.write(buf);
		} catch (IOException e) {
			fatal("network write error", e); // TODO
		}
	}
}
