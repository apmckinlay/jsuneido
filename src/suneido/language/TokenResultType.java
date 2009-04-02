package suneido.language;

public enum TokenResultType {
	N("(Lsuneido/SuValue;)Lsuneido/SuNumber;"),
	S("(Lsuneido/SuValue;)Lsuneido/SuString;"),
	B("(Lsuneido/SuValue;)Lsuneido/SuBoolean;");

	String type;

	TokenResultType(String s) {
		type = s;
	}
}
