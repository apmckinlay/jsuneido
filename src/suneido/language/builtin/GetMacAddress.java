package suneido.language.builtin;

import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.net.UnknownHostException;

import suneido.SuException;
import suneido.language.Args;
import suneido.language.FunctionSpec;
import suneido.language.SuFunction;

public class GetMacAddress extends SuFunction {

	@Override
	public Object call(Object... args) {
		Args.massage(FunctionSpec.noParams, args);
        try {
			return bytesToString(getMacAddress());
		} catch (UnknownHostException e) {
			throw new SuException(
					"GetMacAddress failed - UnknownHostException", e);
		} catch (SocketException e) {
			throw new SuException("GetMacAddress failed - SocketException", e);
		}
	}

	private static byte[] getMacAddress() throws UnknownHostException, SocketException {
		InetAddress address = InetAddress.getLocalHost();
		NetworkInterface ni = NetworkInterface.getByInetAddress(address);
		return ni.getHardwareAddress();
	}

	private static Object bytesToString(byte[] bytes) {
		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < bytes.length; i++)
			sb.append((char) (bytes[i] & 0xff));
		return sb.toString();
	}

}
