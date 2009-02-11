package suneido;

public class SuFunction extends SuValue {
	private final String s;

	public SuFunction(String s) {
		this.s = s.trim();
	}

	@Override
	public String toString() {
		return s;
	}

}
