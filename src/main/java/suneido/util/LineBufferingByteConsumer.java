package suneido.util;

import java.io.Closeable;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.util.function.Consumer;
import java.util.function.ObjIntConsumer;

import suneido.SuInternalError;

/**
 * Consumes bytes of input, buffers up lines, and sends complete lines to a line
 * consumer. Never blocks.
 *
 * @author Victor Schappert
 * @since 20140828
 */
public final class LineBufferingByteConsumer implements ObjIntConsumer<byte[]>,
		Closeable {

	//
	// DATA
	//

	private final Consumer<CharSequence> lineConsumer;
	private final StringBuilder lineBuilder;
	private final char[] buffer;
	private final PipedOutputStream byteSink;
	private final InputStreamReader reader;
	private boolean prevCR;

	private static final int BUFFER_SIZE = 256;

	//
	// CONSTRUCTORS
	//

	/**
	 * Constructs a new line buffering byte consumer.
	 *
	 * @param lineConsumer
	 *            Accepts buffered lines of input; must be able to accept a
	 *            {@code null} {@link CharSequence}, which signifies
	 *            end-of-file.
	 */
	public LineBufferingByteConsumer(Consumer<CharSequence> lineConsumer) {
		if (null == lineConsumer) {
			throw new NullPointerException();
		}
		this.lineConsumer = lineConsumer;
		this.lineBuilder = new StringBuilder(BUFFER_SIZE);
		this.buffer = new char[BUFFER_SIZE];
		this.byteSink = new PipedOutputStream();
		PipedInputStream pis = null;
		try {
			pis = new PipedInputStream(byteSink);
		} catch (IOException e) {
			throw new SuInternalError("can't create PipedInputStream", e);
		}
		this.reader = new InputStreamReader(pis);
		this.prevCR = false;
	}

	//
	// INTERNALS
	//

	private void finishedLine(int start, int end) {
		lineBuilder.append(buffer, start, end);
		lineConsumer.accept(lineBuilder);
		lineBuilder.delete(0, lineBuilder.length());
	}

	private void eof() {
		finishedLine(0, 0);
		lineConsumer.accept(null);
	}

	private void newChars(int numChars) {
		int start = 0;
		for (int k = 0; k < numChars; ++k) {
			final char c = buffer[k];
			if ('\r' == c) {
				finishedLine(start, k);
				prevCR = true;
				start = k + 1;
			} else if ('\n' == c) {
				if (!prevCR) {
					finishedLine(start, k);
				}
				prevCR = false;
				start = k + 1;
			} else {
				prevCR = false;
			}
		}
		lineBuilder.append(buffer, start, numChars);
	}

	//
	// INTERFACE: ObjIntConsumer<byte[]>
	//

	@Override
	public void accept(byte[] newBytes, int numBytes) {
		if (numBytes < 0) {
			eof();
		} else if (0 < numBytes) {
			try {
				byteSink.write(newBytes, 0, numBytes);
			} catch (IOException e) {
				throw new SuInternalError(
						"failed to write to PipedOutputStream", e);
			}
			try {
				while (reader.ready()) {
					int read = reader.read(buffer, 0, BUFFER_SIZE);
					assert 0 < read;
					newChars(read);
				}
			} catch (IOException e) {
				throw new SuInternalError("error reading InputStreamReader", e);
			}
		}
	}

	//
	// INTERFACE: Closeable
	//

	@Override
	public void close() {
		if (0 < lineBuilder.length()) {
			eof();
		}
		// NOTE: The IOExceptions below should never happen because these are
		//       strictly in-memory readers...
		IOException error = null;
		String message = null;
		try {
			byteSink.close();
		} catch (IOException e) {
			message = "failed to close PipedOutputStream";
			error = e;
		}
		try {
			reader.close();
		} catch (IOException e) {
			if (null == error) {
				message = "failed to close InputStreamReader";
				error = e;
			}
		}
		if (null != error) {
			throw new SuInternalError(message, error);
		}
	}
}
