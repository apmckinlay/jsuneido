package suneido;

public class Suneido {
	public static void main(String args[]) {
		System.out.println("hello world");
		
		SuValue vs = new SuString("ok");
		System.out.println("vs " + vs);
		
		SuValue vi = new SuInteger(1);
		System.out.println("vi " + vi);
		
		SuContainer c = new SuContainer();
		c.append(new SuString("zero"));
		c.putdata(new SuString("name"), new SuString("andrew"));
		c.putdata(vi, vs);
		System.out.println("c " + c);
	}
}
