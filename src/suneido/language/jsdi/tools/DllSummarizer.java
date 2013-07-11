package suneido.language.jsdi.tools;

import java.io.PrintStream;

import suneido.Suneido;
import suneido.language.jsdi.JSDI;
import suneido.language.jsdi.dll.Dll;
import suneido.language.jsdi.dll.DllFactory.DllMakeObserver;

public final class DllSummarizer implements DllMakeObserver {

	//
	// DATA
	//

	private static final String[] BUCKETS =
	{
		"MARSHALL PLAN",
		"MARSHALL PLAN + RETURN TYPE",
		"SIGNATURE"
	};

	@SuppressWarnings("rawtypes")
	private final SummaryBucket[] buckets;

	//
	// CONSTRUCTORS
	//

	public DllSummarizer() {
		buckets = new SummaryBucket[BUCKETS.length];
		for (int k = 0; k < buckets.length; ++k)
			buckets[k] = new SummaryBucket<String>();
	}

	//
	// ACCESSORS
	//

	public void printSummary(PrintStream ps, int number) {
		String sep = new String(new byte[80]).replace('\0', '=');
		for (int k = 0; k < BUCKETS.length; ++k) {
			ps.println(sep);
			ps.println(BUCKETS[k]);
			ps.println(sep);
			buckets[k].top(ps,  number);
			ps.println();
			ps.println();
		}
	}

	//
	// INTERFACE: DllMakeObserver
	//

	@SuppressWarnings("unchecked")
	@Override
	public void madeDll(Dll dll) {
		final String mps = dll.getMarshallPlan().toString();
		buckets[0].tally(mps);
		buckets[1].tally(mps + ":" + dll.getReturnType().getDisplayName());
		buckets[2].tally(dll.getSignature());
	}

	//
	// Main Program
	//

	public static void main(String[] args) {
		try {
			final DllSummarizer summarizer = new DllSummarizer();
			JSDI.getInstance().getDllFactory().addObserver(summarizer);
			Runtime.getRuntime().addShutdownHook(new Thread() {
				public void run() {
					summarizer.printSummary(System.out, 20);
				}
			});
			Suneido.main(args);
		} catch (RuntimeException e) {
			e.printStackTrace();
		}
	}
}
