package suneido.language;

public enum TokenResultType {
	I("Ljava/lang/Integer;"),
	N("Ljava/lang/Number;"),
	S("Ljava/lang/String;"),
	B("Ljava/lang/Boolean;"),
	O("Ljava/lang/Object;");

	String type;

	TokenResultType(String s) {
		type = s;
	}
}
