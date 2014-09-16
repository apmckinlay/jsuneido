package suneido.util;

import static org.junit.Assert.*;

import java.util.List;

import org.junit.Test;

import com.google.common.collect.Lists;

public class LineBufferingByteConsumerTest {

	@Test
	public void testMultiline() {
		String input = "a\nb\r\n\ncc\nddd\n\n\r\ne";
		String[] expected = { "a", "b", "", "cc", "ddd", "", "", "e", null };
		byte[] bytes = input.getBytes();
		// Try every possible chunk length from 1 to the entire size of the
		// input.
		for (int m = 1; m < bytes.length; ++m) {
			List<String> output = Lists.newArrayList();
			// Create a consumer for using with this chunk length... 
			try (LineBufferingByteConsumer lbbc = new LineBufferingByteConsumer(
			        (CharSequence s) -> output.add(null != s ? s.toString() : null))) {
				byte[] buffer = new byte[m];
				int n = 0;
				// Pass chunks to the consumer until the whole input byte array
				// is passed.
				while (n < bytes.length) {
					int amount = Math.min(bytes.length - n, m);
					System.arraycopy(bytes, n, buffer, 0, amount);
					lbbc.accept(buffer, amount);
					n += amount;
				}
				lbbc.accept(buffer, -1); // EOF
				// The output list of strings should be as expected.
				assertArrayEquals(expected, output.toArray());
			}
		}
	}
}
