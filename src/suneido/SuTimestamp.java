/* Copyright 2023 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido;

import java.nio.ByteBuffer;

/**  extended timestamps
 */
public class SuTimestamp extends SuDate {
	byte extra;

	public SuTimestamp(int date, int time, byte extra) {
		super(date, time);
 		assert extra != 0;
        this.extra = extra;
	}

	public SuTimestamp(SuDate dt, byte extra) {
		super(dt);
        assert extra != 0;
        this.extra = extra;
	}

	@Override
	public String toString() {
		return String.format("#%04d%02d%02d.%02d%02d%02d%03d%03d",
				year(), month(), day(),
				hour(), minute(), second(), millisecond(),
				Byte.toUnsignedInt(extra));
	}

	@Override
	public boolean equals(Object other) {
		return other instanceof SuTimestamp ts && extra == ts.extra &&
				equals(this, ts);
	}

	@Override
	public int hashCode() {
		return 31 * super.hashCode() + extra;
	}

	@Override
	public int compareTo(SuDate that) {
		return compare(this, that);
	}

	// Packable

	@Override
	public int packSize(int nest) {
		return 10;
	}

	@Override
	public void pack(ByteBuffer buf) {
		super.pack(buf);
		buf.put(extra);
	}

	@Override
	public SuDate plus(int year, int month, int day,
			int hour, int minute, int second, int millisecond) {
		return new SuTimestamp(
			super.plus(year, month, day, hour, minute, second, millisecond),
			extra);
	}
}
