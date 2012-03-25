/* Copyright 2011 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido.immudb.future;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.List;

import org.junit.Test;

import com.google.common.base.Strings;
import com.google.common.collect.ImmutableList;

public class RecordConvertTest {

	@Test
	public void convert() {
		String big = Strings.repeat("hello world", 100);
		Record r = new RecordBuilder().add(123).add(456).add(big).add("foo").build();
		List<String> fields = ImmutableList.of("num", "-", "name");
		suneido.intfc.database.Record rc = RecordConvert.newToOld(r, fields);
		ByteBuffer buf = rc.getBuffer().duplicate().order(ByteOrder.LITTLE_ENDIAN);
		Record r2 = RecordConvert.oldToNew(buf, rc.bufSize());
		assertThat(r2, is(new RecordBuilder().add(123).add(big).build()));
	}

}
