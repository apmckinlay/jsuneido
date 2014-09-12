/* Copyright 2014 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido.debug;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

import suneido.SuInternalError;
import suneido.runtime.SuCallable;
import suneido.util.Errlog;

import com.sun.jdi.AbsentInformationException;
import com.sun.jdi.ArrayReference;
import com.sun.jdi.Bootstrap;
import com.sun.jdi.ClassNotLoadedException;
import com.sun.jdi.ClassType;
import com.sun.jdi.Field;
import com.sun.jdi.IncompatibleThreadStateException;
import com.sun.jdi.IntegerValue;
import com.sun.jdi.InvalidTypeException;
import com.sun.jdi.Location;
import com.sun.jdi.Method;
import com.sun.jdi.ObjectReference;
import com.sun.jdi.ReferenceType;
import com.sun.jdi.StackFrame;
import com.sun.jdi.ThreadReference;
import com.sun.jdi.VMDisconnectedException;
import com.sun.jdi.Value;
import com.sun.jdi.VirtualMachine;
import com.sun.jdi.VirtualMachineManager;
import com.sun.jdi.connect.AttachingConnector;
import com.sun.jdi.connect.Connector;
import com.sun.jdi.connect.IllegalConnectorArgumentsException;
import com.sun.jdi.event.BreakpointEvent;
import com.sun.jdi.event.Event;
import com.sun.jdi.event.EventQueue;
import com.sun.jdi.event.EventSet;
import com.sun.jdi.event.VMDisconnectEvent;
import com.sun.jdi.request.BreakpointRequest;
import com.sun.jdi.request.EventRequest;
import com.sun.jdi.request.EventRequestManager;

/**
 * <p>
 * Implements a client thread that connects to a JDWP agent <em>running in the
 * same virtual machine</em> using Sun's JDI (Java Debug Interface). This client
 * is intended as a fallback alternative to using a native
 * <strong>jsdebug</strong> shared object-based JVMTI agent in situations where
 * there is no <strong>jsdebug</strong> agent library built for the current
 * {@link suneido.boot.Platform platform} but there is a JDWP agent available.
 * </p>
 *
 * <p>
 * The client code uses the same logic as the <strong>jsdebug</strong> agent to
 * store stack frame data onto the {@link StackInfo} object. <strong>It it thus
 * <em>imperative</em> that both this client and the jsdebug code be kept in
 * sync.</strong> Any change to the <strong>jsdebug</strong> code must be
 * mirrored in this class, and any change to this class must be mirrored in the
 * <strong>jsdebug</strong> code.
 * </p>
 *
 * @author Victor Schappert
 * @since 20140910
 */
final class JDWPAgentClient {

	//
	// DATA
	//

	private final String port;
	private final Client actualClient;
	private final Object lock;
	private volatile Thread runningThread;

	//
	// CONSTRUCTORS
	//

	JDWPAgentClient(String port) {
		this.port = port;
		this.actualClient = new Client();
		this.lock = new Object();
		this.runningThread = null;
	}

	//
	// MUTATORS
	//

	/**
	 * Starts running the client thread.
	 *
	 * @see #stop()
	 * @see #isRunning()
	 * @throws SuInternalError
	 *             If the client thread is already running
	 */
	public void start() {
		synchronized (lock) {
			if (isRunning()) {
				throw new SuInternalError("agent controller is already running");
			}
			runningThread = new Thread(actualClient);
			actualClient.mustStop = false;
			runningThread.start();
		}
	}

	/**
	 * Stops the client thread from running.
	 *
	 * @see #start()
	 * @see #isRunning()
	 * @throws SuInternalError
	 *             If the client thread is not currently running
	 */
	public void stop() {
		synchronized (lock) {
			if (!isRunning()) {
				throw new SuInternalError("agent controller is not running");
			}
			actualClient.mustStop = true;
			runningThread.interrupt();
			while (isRunning()) {
				try {
					lock.wait();
				} catch (InterruptedException e) {
					Thread.currentThread().interrupt();
					throw new SuInternalError("interrupted while waiting for "
							+ getClass().getSimpleName() + " thread to stop");
				}
			}
		}
	}

	/**
	 * Indicates whether the client thread is running.
	 *
	 * @return True iff the client thread is running
	 */
	public boolean isRunning() {
		return null != runningThread;
	}

	/**
	 * <p>
	 * Makes this JDWP agent client aware of a new stack trace repository
	 * object.
	 * </p>
	 *
	 * <p>
	 * The reason for doing this is get a <em>direct</em> reference to the stack
	 * trace repository so that we don't have to modify its fields indirectly
	 * via JDI. This enables us to avoid one of the pitfalls of using JDI/JDWP,
	 * which is that unless all of the threads in a JVM are suspended&dagger;,
	 * we can't be certain that any mirrored objects we create via JDI will
	 * survive garbage collection long enough for us to do anything useful with
	 * them. See <a href="http://stackoverflow.com/q/25793688/1911388">this
	 * StackOverflow question</a> for more information.
	 * </p>
	 *
	 * <p>
	 * &dagger;: It is hopefully obvious why we can't allow this to occur: our
	 * JDI client thread is itself <em>in</em> the "target" JVM (<i>ie</i> the
	 * JVM that is running Suneido). If we suspend all the JVM threads, we
	 * suspend the JDI client thread and the whole process will deadlock.
	 * </p>
	 *
	 * @param stackInfo
	 *            Repository that will receive stack trace information when the
	 *            breakpoint in its {@link StackInfo#fetchInfo()} method is
	 *            triggered
	 */
	public void addRepo(StackInfo stackInfo) {
		synchronized (lock) {
			if (actualClient.repoMap.containsKey(stackInfo.id)) {
				throw new SuInternalError(errorMessage(
						" detected duplicate id: " + stackInfo.id, null));
			}
			actualClient.repoMap.put(stackInfo.id, stackInfo);
		}
	}

	//
	// INTERNALS
	//

	private static AttachingConnector getConnector() {
		VirtualMachineManager vmManager = Bootstrap.virtualMachineManager();
		AttachingConnector result = null;
		for (Connector connector : vmManager.attachingConnectors()) {
			if ("com.sun.jdi.SocketAttach".equals(connector.name())) {
				result = (AttachingConnector) connector;
			}
		}
		if (null == result)
			throw new SuInternalError(
					"can't find AttachingConnector to attach to this JVM");
		return result;
	}

	private static VirtualMachine connect(AttachingConnector connector,
			String port) throws IllegalConnectorArgumentsException, IOException {
		Map<String, Connector.Argument> args = connector.defaultArguments();
		Connector.Argument portArgument = args.get("port");
		if (null == portArgument) {
			throw new SuInternalError("AttachingConnector has no port argument");
		}
		portArgument.setValue(port);
		return connector.attach(args);
	}

	private ReferenceType getReferenceType(
			IdentityHashMap<Class<?>, Object> gcStopper, VirtualMachine vm,
			Class<?> clazz) {
		// Make an instance of the class and store it in a reachable location so
		// we can ensure the class will remain loaded through the lifetime of
		// this client.
		try {
			gcStopper.put(clazz, clazz.newInstance());
		} catch (InstantiationException | IllegalAccessException e) {
			throw new SuInternalError(errorMessage("failed to instantiate "
					+ clazz + " for reachability purposes", e), e);
		}
		// For array types, you have to use the canonical name or
		// classesByName(...) will fail.
		String className = clazz.getCanonicalName();
		List<ReferenceType> classes = vm.classesByName(className);
		if (1 != classes.size())
			throw new SuInternalError(errorMessage(
					"expects exactly 1 loaded class called '" + className
							+ "' but found " + classes.size(), null));
		return classes.get(0);
	}

	private ClassType getClassType(IdentityHashMap<Class<?>, Object> gcStopper,
			VirtualMachine vm, Class<?> clazz) {
		final ReferenceType classRef = getReferenceType(gcStopper, vm, clazz);
		if (!(classRef instanceof ClassType)) {
			throw new SuInternalError(errorMessage(
					"expects '" + clazz.getCanonicalName() + "' to be a class",
					null));
		}
		return (ClassType) classRef;
	}

	private Field getFieldByName(ClassType classType, String fieldName) {
		final Field field = classType.fieldByName(fieldName);
		if (null == field) {
			throw new SuInternalError(errorMessage(
					"expects '" + classType.name()
							+ "' to have a field called '" + fieldName + "'",
					null));
		}
		return field;
	}

	private Location getLocationOfClassMethod(
			IdentityHashMap<Class<?>, Object> gcStopper, VirtualMachine vm,
			Class<?> clazz, String methodName) {
		final ClassType classRef = getClassType(gcStopper, vm, clazz);
		final List<Method> methods = classRef.methodsByName(methodName);
		if (1 != methods.size())
			throw new SuInternalError(errorMessage(
					"expect classs " + clazz.getCanonicalName()
							+ " to have exactly 1 method called '" + methodName
							+ "'", null));
		return methods.get(0).location();
	}

	private static BreakpointRequest setBreakpoint(VirtualMachine vm,
			Location location) {
		EventRequestManager erm = vm.eventRequestManager();
		BreakpointRequest bkr = erm.createBreakpointRequest(location);
		bkr.setSuspendPolicy(EventRequest.SUSPEND_EVENT_THREAD);
		return bkr;
	}

	private static void unsetBreakpoint(VirtualMachine vm, BreakpointRequest bkr) {
		bkr.setEnabled(false);
		vm.eventRequestManager().deleteEventRequest(bkr);
	}

	private SuInternalError connectError(String reason, Throwable cause) {
		return new SuInternalError(errorMessage(
				"can't connect to JDWP server in this JVM on port " + port
						+ " due to " + reason, null), cause);
	}

	private String errorMessage(String detail, Throwable cause) {
		final String baseMessage = getClass().getSimpleName() + ' ' + detail;
		if (null == cause) {
			return baseMessage;
		} else {
			return baseMessage + ": '" + cause.getMessage() + '\'';
		}
	}

	private void errlog(String detail, Throwable cause) {
		Errlog.errlog(errorMessage(detail, cause), cause);
	}

	private void todo_deleteme_dumpFrames(List<StackFrame> frames) /*
																	 * TODO:
																	 * delete
																	 * this
																	 * function
																	 */{
		System.out.println("+++++++++++++++++++++++++++++++"); // TODO: delete
																// function
		frames.forEach((StackFrame s) -> System.out.println(s));// TODO: delete
																// function
		System.out.println("+++++++++++++++++++++++++++++++");// TODO: delete
																// function
	}

	private StackInfo convertRepoIndirectToDirect(ObjectReference repoIndirect) {
		try {
			IntegerValue idValue = (IntegerValue) repoIndirect
					.getValue(actualClient.idField);
			int id = idValue.intValue();
			StackInfo repoDirect = null;
			synchronized (lock) {
System.out.println("@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@");
System.out.println(actualClient.repoMap);
System.out.println("@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@");
				repoDirect = actualClient.repoMap.get(id);
			}
			if (null == repoDirect) {
				throw new IllegalStateException("can't find repo with id: "
						+ id);
			}
			return repoDirect;
		} catch (Throwable t) {
			throw new SuInternalError(errorMessage(
					"failed to convert repo mirror to direct reference", t), t);
		}
	}

	private void handleBreakpointEvent(BreakpointEvent breakpointEvent) {
		final ThreadReference suspendedThread = breakpointEvent.thread();
		// Get the frames
		List<StackFrame> frames = null;
		Exception cause = null;
		try {
			frames = suspendedThread.frames();
		} catch (IncompatibleThreadStateException e) {
			errlog("can't get stack trace due to incompatible thread state", e);
			frames = Collections.emptyList();
			cause = null;
		}
		if (frames.isEmpty()) {
			throw new SuInternalError("empty stack trace", cause);
		}
		// Get the "this" object
		ObjectReference repoIndirect = frames.get(0).thisObject();
		StackInfo repoDirect = convertRepoIndirectToDirect(repoIndirect);
		todo_deleteme_dumpFrames(frames);
		handleStackTrace(repoDirect, repoIndirect, suspendedThread, frames);
	}

	private static enum MethodName {
		// ---------------------------------------------------------------------
		// WARNING: This code MUST be kept in sync with the equivalent code in
		// the "jsdebug" shared object/dynamic link library as well as
		// the call stack decoding code in CallstackAll.java.
		// ---------------------------------------------------------------------
		UNKNOWN(0x000), EVAL(0x100), EVAL0(EVAL.value | 10), EVAL1(
				EVAL.value | 11), EVAL2(EVAL.value | 12), EVAL3(EVAL.value | 13), EVAL4(
				EVAL.value | 14), CALL(0x200), CALL0(CALL.value | 10), CALL1(
				CALL.value | 11), CALL2(CALL.value | 12), CALL3(CALL.value | 13), CALL4(
				CALL.value | 14);
		public final int value;

		private MethodName(int value) {
			this.value = value;
		}

		public boolean isCall() {
			return CALL.value == (CALL.value & this.value);
		}

		public static MethodName getMethodName(Method method) {
			final String name = method.name();
			if (name.startsWith("eval")) {
				if (4 == name.length()) {
					return EVAL;
				} else if (5 == name.length()) {
					switch (name.charAt(4)) {
					case '0':
						return EVAL0;
					case '1':
						return EVAL1;
					case '2':
						return EVAL2;
					case '3':
						return EVAL3;
					case '4':
						return EVAL4;
					}
				}
			} else if (name.startsWith("call")) {
				if (4 == name.length()) {
					return CALL;
				} else if (5 == name.length()) {
					switch (name.charAt(4)) {
					case '0':
						return CALL0;
					case '1':
						return CALL1;
					case '2':
						return CALL2;
					case '3':
						return CALL3;
					case '4':
						return CALL4;
					}
				}
			} /* if (name.startsWith(...)) */
			return UNKNOWN;
		} /* static MethodName getMethodName(...) */
	} /* enum MethodName */

	private static boolean isAssignableFrom(ReferenceType subClass,
			ClassType superClass) {
		return subClass instanceof ClassType ? isAssignableFrom(
				(ClassType) subClass, superClass) : false;
	}

	private static boolean isAssignableFrom(ClassType subClass,
			ClassType superClass) {
		// Attempt to mirror the JNI function IsAssignableFrom() described here:
		// http://docs.oracle.com/javase/8/docs/technotes/guides/jni/spec/functions.html#IsAssignableFrom
		// for the simplified case where both operands are classes.
		do {
			if (subClass.equals(superClass)) {
				return true;
			}
			subClass = subClass.superclass();
		} while (null != subClass);
		return false;
	}

	private void handleLocals(StackInfo repoDirect,
			ArrayReference localsValuesArrIndirect, StackFrame frame,
			List<String> nameScratch, List<Value> valueScratch, int frameIndex) {
		List<com.sun.jdi.LocalVariable> localVariables = null;
		// Fetch the locals. Handle an "absent information" exception by
		// reporting it and then using an empty list.
		try {
			localVariables = frame.visibleVariables();
		} catch (AbsentInformationException e) {
			errlog("frame has no local variable information", e);
			localVariables = Collections.emptyList();
		}
		// Add the "this" local variable. Unlike with pure JVMTI, the "this"
		// local variable isn't included in the local variables list.
		nameScratch.clear();
		valueScratch.clear();
		ObjectReference thisRef = frame.thisObject();
System.out.println("***************************************************************");
		if (null != thisRef) {
System.out.println("this    ^^^^^^^");
			nameScratch.add("this");
			valueScratch.add(thisRef);
		}
		// Get the remaining local variables.
		for (com.sun.jdi.LocalVariable var : localVariables) {
			Value value = frame.getValue(var);
			assert !"this".equals(var.name()) : "JDI doesn't include \"this\" in the local variables";
			if (value instanceof ObjectReference) {
System.out.println(var.name());
				nameScratch.add(var.name());
				valueScratch.add(value);
			}
		}
System.out.println("***************************************************************");
		// Store the local variable information
		repoDirect.localsNames[frameIndex] = nameScratch
				.toArray(new String[nameScratch.size()]);
		// Have to store the values indirectly through JDI because all we have
		// are mirrors (instances of the JDI Value class).
		repoDirect.localsValues[frameIndex] = new Object[valueScratch.size()];
		ArrayReference localsValuesAtFrameIndex = (ArrayReference) localsValuesArrIndirect
				.getValue(frameIndex);
		try {
			localsValuesAtFrameIndex.setValues(valueScratch);
		} catch (ClassNotLoadedException | InvalidTypeException e) {
			throw new SuInternalError(errorMessage(
					"failed to store locals values indirectly", e), e);
		}
	}

	private void handleStackTrace(StackInfo repoDirect,
			ObjectReference repoIndirect, ThreadReference suspendedThread,
			List<StackFrame> frames) {
		// ---------------------------------------------------------------------
		// WARNING: This code MUST be kept in sync with the equivalent code in
		// the "jsdebug" shared object/dynamic link library as well as
		// the call stack decoding code in CallstackAll.java.
		// ---------------------------------------------------------------------
		try {
			// Construct some scratch space
			final List<String> scratchLocalsNames = new ArrayList<>(16);
			final List<Value> scratchLocalsValues = new ArrayList<>(16);
			// Construct the arrays that will hold the stack data.
			final int NUM_FRAMES = frames.size();
			repoDirect.localsNames = new String[NUM_FRAMES][];
			repoDirect.localsValues = new Object[NUM_FRAMES][];
			repoDirect.isCall = new boolean[NUM_FRAMES];
			repoDirect.lineNumbers = new int[NUM_FRAMES];
			// Obtain a mirror of the repo object's localsValues array, since we can
			// only store mirrored values into it indirectly.
			ArrayReference localsValuesArrIndirect = (ArrayReference) repoIndirect
					.getValue(actualClient.localsValuesField);
			// Walk the stack looking for frames where the method's class is an
			// instance of STACK_FRAME_CLASS.
			MethodName methodNameAbove = null;
			MethodName methodNameCur = MethodName.UNKNOWN;
			ObjectReference thisRefAbove = null;
			ObjectReference thisRefCur = null;
			int frameIndex = 0;
			for (StackFrame frame : frames) {
				// Keep track of the method name and "this" value in the frame we
				// just looked at (the frame "above" the current frame in the stack
				// trace). This information is needed to determine which Java stack
				// frames actually constitute Suneido stack frames since it may take
				// 3-4 Java stack frames to invoke a Suneido callable.
				thisRefAbove = thisRefCur;
				thisRefCur = null;
				methodNameAbove = methodNameCur;
				methodNameCur = MethodName.UNKNOWN;
				// Get the frame method
				final Location location = frame.location();
				final Method method = location.method();
				// Skip native methods
				if (method.isNative())
					continue;
				// Skip non-public methods
				if (!method.isPublic())
					continue;
				// Skip static methods
				if (method.isStatic())
					continue;
				// If the declaring class is not assignable to STACK_FRAME_CLASS,
				// we don't want stack frame data from it.
				if (!isAssignableFrom(method.declaringType(),
						actualClient.stackFrameClassRef))
					continue;
				// Get the method name
				methodNameCur = MethodName.getMethodName(method);
				if (MethodName.UNKNOWN == methodNameCur)
					continue;
				// Get the "this" instance so we can determine if this stack frame
				// has the same "this" as the previous stack frame.
				thisRefCur = frame.thisObject();
				// If the "this" instance for this Java stack frame is the same as
				// the "this" instance of the immediately preceding Java stack
				// frame, both frames may logically be part of the same Suneido
				// callable invocation and we only want the top frame, which we have
				// alreadya seen...
				if (thisRefCur.equals(thisRefAbove)
						&& methodNameAbove != methodNameCur) {
					continue;
				}
				// Fetch the locals for this frame
				handleLocals(repoDirect, localsValuesArrIndirect, frame,
						scratchLocalsNames, scratchLocalsValues, frameIndex);
				// Tag methods that are calls
				if (methodNameCur.isCall()) {
					repoDirect.isCall[frameIndex] = true;
				}
				// Add the line number
				repoDirect.lineNumbers[frameIndex] = location.lineNumber();
				// Next frame
				++frameIndex;
			}
			// Mark the stack info repository as initialized.
			repoDirect.isInitialized = true;
			assert repoDirect.isInitialized();
		} finally {
			synchronized (lock) {
				actualClient.repoMap.remove(repoDirect.id);
			}
		}
	}

	//
	// INTERNAL CONSTANTS
	//

	private static final Class<StackInfo> REPO_CLASS = StackInfo.class;
	private static final Class<SuCallable> STACK_FRAME_CLASS = SuCallable.class;
	private static final String ID_FIELD_NAME = "id";
	private static final String LOCALS_VALUE_FIELD_NAME = "localsValues";

	//
	// TYPES
	//

	private final class Client implements Runnable {
		//
		// DATA
		//

		public volatile boolean mustStop;
		private final IdentityHashMap<Class<?>, Object> gcStopper;
		private final Map<Integer, StackInfo> repoMap;
		private VirtualMachine vm;
		private BreakpointRequest breakpointRequest;
		private final EventQueue eventQueue;
		private final ClassType repoClassRef;
		private final ClassType stackFrameClassRef;
		private final Field idField;
		private final Field localsValuesField;

		//
		// CONSTRUCTORS
		//

		private Client() {
			mustStop = false;
			gcStopper = new IdentityHashMap<>();
			repoMap = new HashMap<>();
			AttachingConnector connector = getConnector();
			try {
				vm = connect(connector, JDWPAgentClient.this.port);
			} catch (IOException e) {
				throw connectError("I/O exception", e);
			} catch (IllegalConnectorArgumentsException e) {
				throw connectError("illegal connector arguments", e);
			}
			try {
				eventQueue = vm.eventQueue();
				repoClassRef = getClassType(gcStopper, vm, REPO_CLASS);
				stackFrameClassRef = getClassType(gcStopper, vm,
						STACK_FRAME_CLASS);
				idField = getFieldByName(repoClassRef, ID_FIELD_NAME);
				localsValuesField = getFieldByName(repoClassRef,
						LOCALS_VALUE_FIELD_NAME);
				final Location location = getLocationOfClassMethod(gcStopper,
						vm, REPO_CLASS, "fetchInfo");
				breakpointRequest = setBreakpoint(vm, location);
				breakpointRequest.setEnabled(true);
			} catch (Throwable t) {
				dispose();
				throw t;
			}
		}

		//
		// INTERNALS
		//

		private void dispose() {
			try {
				if (null != vm) {
					if (null != breakpointRequest) {
						unsetBreakpoint(vm, breakpointRequest);
					}
					vm.dispose();
					breakpointRequest = null;
					vm = null;
				}
			} catch (Throwable t) {
				Errlog.errlog(t.getMessage(), t);
			}
		}

		//
		// INTERFACE: Runnable
		//

		@Override
		public void run() {
			boolean interrupted = false;
			try {
				eventloop: while (!mustStop) {
					EventSet eventSet = null;
					try {
						// Wait forever to dequeue the next event. (May throw an
						// InterruptedException).
						eventSet = eventQueue.remove();
						for (Event event : eventSet) {
							if (event instanceof BreakpointEvent) {
								handleBreakpointEvent((BreakpointEvent) event);
							} else if (event instanceof VMDisconnectEvent) {
								// Next attempt to dequeue an event will throw
								// VMDisconnectedException, so stop loopping.
								vm = null;
								break eventloop;
							}
						}
					} catch (InterruptedException tracked) {
						// Thrown by eventQueue.remove(). The most likely cause
						// is the stop() method interrupted our thread, in which
						// case the loop condition will drop us out of the loop.
						// If interrupted by anyone else, loop will continue but
						// we always preserve interrupted state to set on
						// return.
						interrupted = true;
					} catch (VMDisconnectedException stopper) {
						// This exception can be thrown at any point in the
						// handleBreakpointEvent(...) process.
						errlog("event loop terminated due to VM disconnection",
								stopper);
						vm = null;
						break;
					} catch (Throwable logged) {
						errlog("event loop caught a throwable", logged);
					} finally {
						// Regardless of what else happens, resume the thread
						// that got suspended at the breakpoint.
						if (null != eventSet) {
							eventSet.resume();
						}
					}
				}
			} finally {
				dispose();
				synchronized (lock) {
					assert null != JDWPAgentClient.this.runningThread;
					JDWPAgentClient.this.runningThread = null;
					lock.notifyAll();
				}
				if (interrupted) {
					Thread.currentThread().interrupt();
				}
			}
		}
	}
}
