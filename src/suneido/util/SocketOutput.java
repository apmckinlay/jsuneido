package suneido.util;

import static suneido.Suneido.fatal;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.util.Arrays;

import javax.annotation.concurrent.NotThreadSafe;

/**
 * avoid problems with Nagle
 * by doing a single gathering write instead of multiple small writes
 * @author Andrew McKinlay
 */
@NotThreadSafe
public class SocketOutput {
	private final SocketChannel channel;
	private final int MAXSIZE = 8;
	private final ByteBuffer[] queue = new ByteBuffer[MAXSIZE];
	private int n = 0;

	public SocketOutput(SocketChannel channel) {
		this.channel = channel;
	}

	public void add(ByteBuffer buf) {
		queue[n++] = buf;
	}

	public void write() {
		try {
			synchronized(SocketOutput.class) {
				channel.write(queue, 0, n);
			}
		} catch (IOException e) {
			fatal("network write error", e); // TODO
		}
		Arrays.fill(queue, null);
		n = 0;
	}

	public void write(ByteBuffer buf) {
		try {
			channel.write(buf);
		} catch (IOException e) {
			fatal("network write error", e); // TODO
		}
	}
}
