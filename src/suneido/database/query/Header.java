package suneido.database.query;

import java.util.List;

public class Header {

	public int size() {
		// TODO Auto-generated method stub
		return 0;
	}

	public Header rename(List<String> from, List<String> to) {
		// TODO Auto-generated method stub
		return null;
	}

	public Header project(List<String> flds) {
		// TODO Auto-generated method stub
		return null;
	}

	public boolean equal(Row r1, Row r2) {
		for (String f : columns())
			if (r1.getraw(this, f) != r2.getraw(this, f))
				return false;
		return true;
	}

	public List<String> columns() {
		// TODO Auto-generated method stub
		return null;
	}

}
