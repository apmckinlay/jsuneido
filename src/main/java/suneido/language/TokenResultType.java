package suneido.language;

public enum TokenResultType {
	I("Ljava/lang/Integer;"),
	N("Ljava/lang/Number;"),
	S("Ljava/lang/String;"),
	B("Ljava/lang/Boolean;"),
	B_("Z"),
	O("Ljava/lang/Object;");

	String type;

	TokenResultType(String s) {
		type = s;
	}
}
