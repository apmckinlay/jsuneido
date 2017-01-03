/* Copyright 2016 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido.database.server;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;

import java.nio.ByteBuffer;

import org.junit.Test;

import com.google.common.collect.ImmutableList;

import suneido.SuContainer;
import suneido.SuInternalError;
import suneido.util.ByteBuffers;

public class SuChannelTest {
	private SuChannel server;
	private static final byte[] bytes = "foobar".getBytes();
	private static final ByteBuffer buf = ByteBuffers.stringToBuffer("hello world");
	private static final ImmutableList<Integer> ints = ImmutableList.of(1, 2, 3);
	private static final ImmutableList<String> strings = ImmutableList.of("one", "two");

	@Test
	public void test() {
		TestChannel channel = new TestChannel(this::serverHandler);
		SuChannel client = new SuChannel(channel);
		server = new SuChannel(channel);

		client.put(1);
		client.put(false);
		client.put(true);
		client.putByte((byte) 0);
		client.putByte(Byte.MIN_VALUE);
		client.putByte(Byte.MAX_VALUE);
		client.put(0);
		client.put(63);
		client.put(-63);
		client.put(12345);
		client.put(Integer.MIN_VALUE);
		client.put(Integer.MAX_VALUE);
		client.put(Long.MAX_VALUE);
		client.put("");
		client.put("hello world\000\177\377");
		client.put(bytes);
		client.put(buf.duplicate());
		client.put(buf.duplicate());
		client.putInts(ints);
		client.putStrings(strings);
		client.putPacked(container());
		client.write();

		assertThat(client.getString(), equalTo("response"));
		assertThat(client.getInt(), equalTo(12345));

		client.put(2).put("another request");
		client.write();
		assertThat(client.getString(), equalTo("another response"));

		client.close();
		server.close();
	}

	private void serverHandler() {
		switch (server.getInt()) {
		case 1:
			one();
			break;
		case 2:
			two();
			break;
		default:
			throw SuInternalError.unreachable();
		}
	}

	private void one() {
		assertThat(server.getBool(), equalTo(false));
		assertThat(server.getBool(), equalTo(true));
		assertThat(server.getByte(), equalTo((byte) 0));
		assertThat(server.getByte(), equalTo(Byte.MIN_VALUE));
		assertThat(server.getByte(), equalTo(Byte.MAX_VALUE));
		assertThat(server.getInt(), equalTo(0));
		assertThat(server.getInt(), equalTo(63));
		assertThat(server.getInt(), equalTo(-63));
		assertThat(server.getInt(), equalTo(12345));
		assertThat(server.getInt(), equalTo(Integer.MIN_VALUE));
		assertThat(server.getInt(), equalTo(Integer.MAX_VALUE));
		assertThat(server.getLong(), equalTo(Long.MAX_VALUE));
		assertThat(server.getString(), equalTo(""));
		assertThat(server.getString(), equalTo("hello world\000\177\377"));
		assertThat(server.getBytes(), equalTo(bytes));
		assertThat(server.getBuffer(), equalTo(buf));
		assertThat(server.getBuffer(), equalTo(buf));
		assertThat(server.getInts(), equalTo(ints));
		assertThat(server.getStrings(), equalTo(strings));
		assertThat(server.getPacked(), equalTo(container()));

		server.put("response").put(12345);
		server.write();
	}

	private void two() {
		assertThat(server.getString(), equalTo("another request"));
		server.put("another response");
		server.write();
	}

	private static SuContainer container() {
		SuContainer c = new SuContainer();
		c.add(123);
		c.put("foo", "bar");
		return c;
	}

}
