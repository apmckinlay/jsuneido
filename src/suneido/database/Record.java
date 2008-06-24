package suneido.database;

import suneido.Packable;

public interface Record extends Packable {
	void add(byte[] data);
	byte[] get(int i);
}
