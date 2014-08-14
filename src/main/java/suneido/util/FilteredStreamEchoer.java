package suneido.util;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;

/**
 * <p>
 * Reads an input stream line-by-line and in a buffered fashion. Tests each line
 * against a filter. If the filter indicates to include the line, writes the
 * line to an output stream.
 * </p>
 *
 * @author Victor Schappert
 * @since 20140814
 */
public final class FilteredStreamEchoer implements Runnable {

	//
	// DATA
	//

	private final BufferedReader in;
	private final OutputStreamWriter out;
	private final Filter<String> filter;

	//
	// CONSTRUCTORS
	//

	/**
	 * Constructs the stream echoer.
	 *
	 * @param in Input stream to read lines from in a buffered manner
	 * @param filter Filter that decides which lines to echo
	 * @param out Output stream to write lines that match the filter to
	 */
	public FilteredStreamEchoer(InputStream in, Filter<String> filter,
			OutputStream out) {
		if (null == in) {
			throw new IllegalArgumentException("input stream cannot be null");
		}
		if (null == filter) {
			throw new IllegalArgumentException("filter cannot be null");
		}
		if (null == out) {
			throw new IllegalArgumentException("output stream cannot be null");
		}
		this.in = new BufferedReader(new InputStreamReader(in));
		this.out = new OutputStreamWriter(out);
		this.filter = filter;
	}

	//
	// INTERFACE: Runnable
	//

	@Override
	public void run() {
		try {
		String line;
		while (null != (line = in.readLine())) {
			if (filter.include(line)) {
				out.write(line);
				out.write(System.lineSeparator());
				out.flush();
			}
		}
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}
}
