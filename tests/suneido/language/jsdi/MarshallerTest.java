package suneido.language.jsdi;

import static org.junit.Assert.*;

import javax.xml.bind.DatatypeConverter;

import org.junit.Test;

import suneido.language.jsdi.type.SizeDirect;

public class MarshallerTest {

	private static byte[] ba(String input) {
		return DatatypeConverter.parseHexBinary(input);
	}

	@Test
	public void testMarshallBool() {
		MarshallPlan mp = MarshallPlan.makeDirectPlan(SizeDirect.BOOL);
		Marshaller mr = mp.makeMarshaller(0);
		mr.putBool(true);
		assertArrayEquals(ba("01000000"), mr.getData()); // little-endian
		mr = mp.makeMarshaller(0);
		mr.putBool(false);
		assertArrayEquals(ba("00000000"), mr.getData());
	}

	@Test
	public void testMarshallChar() {
		MarshallPlan mp = MarshallPlan.makeDirectPlan(SizeDirect.CHAR);
		Marshaller mr = mp.makeMarshaller(0);
		mr.putChar((byte)0x21);
		assertArrayEquals(ba("21"), mr.getData());
	}

	@Test
	public void testMarshallShort() {
		MarshallPlan mp = MarshallPlan.makeDirectPlan(SizeDirect.SHORT);
		Marshaller mr = mp.makeMarshaller(0);
		mr.putShort((short)0x1982);
		assertArrayEquals(ba("8219"), mr.getData()); // little-endian
	}

	@Test
	public void testMarshallLong() {
		MarshallPlan mp = MarshallPlan.makeDirectPlan(SizeDirect.LONG);
		Marshaller mr = mp.makeMarshaller(0);
		mr.putLong(0x19820702);
		assertArrayEquals(ba("02078219"), mr.getData()); // little-endian
	}

	@Test
	public void testMarshallInt64() {
		MarshallPlan mp = MarshallPlan.makeDirectPlan(SizeDirect.INT64);
		Marshaller mr = mp.makeMarshaller(0);
		mr.putInt64(0x0123456789abcdefL);
		assertArrayEquals(ba("efcdab8967452301"), mr.getData()); // little-endian
	}

	@Test
	public void testMarshallFloat() {
		MarshallPlan mp = MarshallPlan.makeDirectPlan(SizeDirect.FLOAT);
		Marshaller mr = mp.makeMarshaller(0);
		mr.putFloat(1.0f); // IEEE 32-bit float binary rep => 0x3f800000
		assertArrayEquals(ba("0000803f"), mr.getData()); // little-endian
	}

	@Test
	public void testMarshallDouble() {
		MarshallPlan mp = MarshallPlan.makeDirectPlan(SizeDirect.DOUBLE);
		Marshaller mr = mp.makeMarshaller(0);
		mr.putDouble(19820207.0); // IEEE 64-bit double binary rep => 0x4172e6eaf0000000
		assertArrayEquals(ba("000000f0eae67241"), mr.getData()); // little-endian
	}

	
}
