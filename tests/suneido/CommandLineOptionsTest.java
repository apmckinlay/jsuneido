package suneido;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class CommandLineOptionsTest {

	@Test
	public void none() {
		assertEquals("SERVER",
				CommandLineOptions.parse(new String[0]).toString());
	}

	@Test
	public void just_rest() {
		assertEquals("SERVER rest: [foo, bar]",
				CommandLineOptions.parse(new String[] { "foo", "bar" }).toString());
	}

	@Test
	public void just_action() {
		assertEquals("DUMP",
				CommandLineOptions.parse(new String[] { "-dump" }).toString());
		assertEquals("SERVER",
				CommandLineOptions.parse(new String[] { "-s" }).toString());
	}

	@Test
	public void separator() {
		assertEquals("SERVER rest: [-foo, -bar]",
				CommandLineOptions.parse(new String[] { "--", "-foo", "-bar" }).toString());
	}

	@Test
	public void action_arg() {
		assertEquals("DUMP stdlib",
				CommandLineOptions.parse(new String[] { "-dump", "stdlib" }).toString());
	}

	@Test
	public void ip() {
		assertEquals("SERVER 192.168.1.123",
				CommandLineOptions.parse(new String[] { "-s", "192.168.1.123" }).toString());
	}

	@Test
	public void port() {
		assertEquals("SERVER port=1234",
				CommandLineOptions.parse(new String[] { "-port", "1234" }).toString());
		assertEquals("SERVER port=1234",
				CommandLineOptions.parse(new String[] { "-p", "1234" }).toString());
	}

	@Test(expected=SuException.class)
	public void two_actions() {
		CommandLineOptions.parse(new String[] { "-dump -load" });
	}

	@Test(expected=SuException.class)
	public void port_without_server() {
		CommandLineOptions.parse(new String[] { "-dump -port 123" });
	}

}
