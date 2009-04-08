package suneido.language;

public enum TokenResultType {
	I("Ljava/lang/Integer;"),
	N("Ljava/lang/Number;"),
	S("Ljava/lang/String;"),
	B("Ljava/lang/Boolean;");

	String type;

	TokenResultType(String s) {
		type = s;
	}
}
