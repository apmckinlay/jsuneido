/* Copyright 2014 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido.debug;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import suneido.SuInternalError;
import suneido.runtime.SuCallable;
import suneido.util.Errlog;

import com.sun.jdi.AbsentInformationException;
import com.sun.jdi.ArrayReference;
import com.sun.jdi.ArrayType;
import com.sun.jdi.Bootstrap;
import com.sun.jdi.ClassNotLoadedException;
import com.sun.jdi.ClassType;
import com.sun.jdi.Field;
import com.sun.jdi.IncompatibleThreadStateException;
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
	private volatile Thread runningThread;

	//
	// CONSTRUCTORS
	//

	JDWPAgentClient(String port) {
		this.port = port;
		this.actualClient = new Client();
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
		synchronized (actualClient) {
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
		synchronized (actualClient) {
			if (!isRunning()) {
				throw new SuInternalError("agent controller is not running");
			}
			actualClient.mustStop = true;
			runningThread.interrupt();
			while (isRunning()) {
				try {
					actualClient.wait();
				} catch (InterruptedException e) {
					Thread.currentThread().interrupt();
					throw new SuInternalError("interrupted while waiting for "
							+ getClass().getSimpleName() + " thread to stop");
				}
			}
		}
	}

	public boolean isRunning() {
		return null != runningThread;
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

	private <T extends ReferenceType> T getReferenceType(VirtualMachine vm,
			Class<?> clazz, Class<T> destType) {
		return destType.cast(getReferenceType(vm, clazz));
	}

	private ReferenceType getReferenceType(VirtualMachine vm, Class<?> clazz) {
		// For array types, you have to use the canonical name or
		// classesByName(...) will fail.
		String className = clazz.getCanonicalName();
		List<ReferenceType> classes = vm.classesByName(className);
		// Sometimes the VM doesn't have the class loaded. This is somewhat
		// surprising, because the VM is really just a JDI/JDWP layer over THIS
		// JVM in THIS process and we have a live reference to the actual class,
		// since it is a parameter to THIS METHOD, which means it must be
		// loaded! In any event, I have found that the workaround in this case
		// is to create an instance of the class. Obviously this means the
		// class must be concrete and have a nullary constructor.
		if (classes.isEmpty()) {
			try {
				if (!clazz.isArray()) {
					Object instance = clazz.newInstance();
					classes = vm.classesByName(className);
					instance.equals(instance);
				}
				// NOTE: If we ever need this workaround for arrays, try
				// java.lang.reflect.Array.newInstance(clazz.componentType(), 0)
			} catch (Throwable swallowed) {
			}
		}
		if (1 != classes.size())
			throw new SuInternalError(errorMessage(
					"expects exactly 1 loaded class called '" + className
							+ "' but found " + classes.size(), null));
		return classes.get(0);
	}

	private ClassType getClassType(VirtualMachine vm, Class<?> clazz) {
		final ReferenceType classRef = getReferenceType(vm, clazz);
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

	private Location getLocationOfClassMethod(VirtualMachine vm,
			Class<?> clazz, String methodName) {
		final ClassType classRef = getClassType(vm, clazz);
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

	private void handleBreakpointEvent(BreakpointEvent breakpointEvent) {
		final ThreadReference thread = breakpointEvent.thread();
		List<StackFrame> frames = null;
		try {
			frames = thread.frames();
		} catch (IncompatibleThreadStateException e) {
			errlog("can't get stack trace due to incompatible thread state", e);
			frames = Collections.emptyList();
		}
		assert null != frames && !frames.isEmpty();
		todo_deleteme_dumpFrames(frames);
		handleStackTrace(frames);
	}

	private static final Class<StackInfo> REPO_CLASS = StackInfo.class;
	private static final Class<SuCallable> STACK_FRAME_CLASS = SuCallable.class;

	private static final String LOCALS_NAME_FIELD_NAME = "localsNames";
	private static final Class<String[][]> LOCALS_NAME_FIELD_CLASS = String[][].class;
	private static final String LOCALS_VALUE_FIELD_NAME = "localsValues";
	private static final Class<Object[][]> LOCALS_VALUE_FIELD_CLASS = Object[][].class;
	private static final String IS_CALL_FIELD_NAME = "isCall";
	private static final Class<boolean[]> IS_CALL_FIELD_CLASS = boolean[].class;
	private static final String LINE_NUMBERS_FIELD_NAME = "lineNumbers";
	private static final Class<int[]> LINE_NUMBERS_CLASS = int[].class;
	private static final String IS_INITIALIZED_FIELD_NAME = "isInitialized";

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

	private void handleLocals(StackFrame frame, List<Value> allLocalsNames,
			List<Value> allLocalsValues) {
		List<com.sun.jdi.LocalVariable> localVariables = null;
		// Fetch the locals. Handle an "absent information" exception by
		// reporting it and then using an empty list.
		try {
			localVariables = frame.visibleVariables();
		} catch (AbsentInformationException e) {
			errlog("frame has no local variable information", e);
			localVariables = Collections.emptyList();
		}
		// Put the locals into list form.
		List<Value> frameLocalsNames = new ArrayList<>(localVariables.size());
		List<Value> frameLocalsValues = new ArrayList<>(localVariables.size());
		for (com.sun.jdi.LocalVariable var : localVariables) {
			Value value = frame.getValue(var);
			if (null != value) {
				frameLocalsNames.add(actualClient.vm.mirrorOf(var.name()));
				frameLocalsValues.add(value);
			}
		}
		// Convert the lists into arrays
		ArrayReference frameLocalsName$ = actualClient.stringArrayType
				.newInstance(frameLocalsNames.size());
		ArrayReference frameLocalsValue$ = actualClient.objectArrayType
				.newInstance(frameLocalsValues.size());
		try {
			frameLocalsName$.setValues(frameLocalsNames);
			frameLocalsValue$.setValues(frameLocalsValues);
		} catch (ClassNotLoadedException | InvalidTypeException e) {
			throw new SuInternalError(errorMessage(
					"can't store values to frame locals array", e), e);
		}
		// Store the arrays into our lists of arrays
		allLocalsNames.add(frameLocalsName$);
		allLocalsValues.add(frameLocalsValue$);
	}

	private void initializeRepo(ObjectReference repoRef,
			List<Value> localsNames, List<Value> localsValues,
			List<Value> isCall, List<Value> lineNumbers) {
		final int N = localsNames.size();
		assert N == localsNames.size();
		assert N == localsValues.size();
		assert N == isCall.size();
		assert N == lineNumbers.size();
		@SuppressWarnings("unchecked")
		final List<Value>[] values = new List[] { localsNames, localsValues,
				isCall, lineNumbers };
		assert values.length == actualClient.repoArrayFieldTypes.length;
		assert values.length == actualClient.repoArrayFields.length;
		for (int k = 0; k < values.length; ++k) {
			ArrayReference array = actualClient.repoArrayFieldTypes[k]
					.newInstance(N);
			try {
				array.setValues(values[k]);
			} catch (ClassNotLoadedException | InvalidTypeException e) {
				throw new SuInternalError(errorMessage(
						"can't store values to repo array", e), e);
			}
			try {
				repoRef.setValue(actualClient.repoArrayFields[k], array);
				// FIXME: The above is throwing an ObjectCollectedException
				// because there aren't any strong references to the
				// array (the only one we have is, via JDI, through an
				// ArrayReference) so it gets garbage-collected as soon
				// as the JVM gets a chance. I think the solution is to
				// put methods in StackInfo to create the arrays and
				// immediately store them into fields. That way we can
				// just create the arrays by calling a method on the
				// StackInfo instance, which we know can't be garbage
				// collected since it is reachable from the suspended
				// thread and in fact is the "this" at the top of the
				// execution stack.
			} catch (ClassNotLoadedException | InvalidTypeException e) {
				throw new SuInternalError(errorMessage(
						"can't store repo array into repo field", e), e);
			}
		}
		// Mark the whole repo instance as initialized
		try {
			repoRef.setValue(
					getFieldByName(actualClient.repoClassRef,
							IS_INITIALIZED_FIELD_NAME), actualClient.vm
							.mirrorOf(true));
		} catch (ClassNotLoadedException | InvalidTypeException e) {
			throw new SuInternalError(errorMessage(
					"can't mark repo initialized", e), e);
		}
	}

	private void handleStackTrace(List<StackFrame> frames) {
		// ---------------------------------------------------------------------
		// WARNING: This code MUST be kept in sync with the equivalent code in
		// the "jsdebug" shared object/dynamic link library as well as
		// the call stack decoding code in CallstackAll.java.
		// ---------------------------------------------------------------------

		// Construct the lists that will hold the stack data to store onto the
		// REPO_CLASS object which is at the top of the thread that is suspended
		// at the breakpoint.
		final List<Value> localsNames = new ArrayList<>();
		final List<Value> localsValues = new ArrayList<>();
		final List<Value> isCall = new ArrayList<>();
		final List<Value> lineNumbers = new ArrayList<>();
		// Walk the stack looking for frames where the method's class is an
		// instance of STACK_FRAME_CLASS.
		MethodName methodNameAbove = null;
		MethodName methodNameCur = MethodName.UNKNOWN;
		ObjectReference thisRefAbove = null;
		ObjectReference thisRefCur = null;
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
			if (!method.isStatic())
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
			// Tag methods that are calls
			isCall.add(actualClient.vm.mirrorOf(methodNameCur.isCall()));
			// Fetch the locals for this frame
			handleLocals(frame, localsNames, localsValues);
			// Add the line number
			lineNumbers.add(actualClient.vm.mirrorOf(location.lineNumber()));
		}
		// Retrieve the "this" reference for the frame where the breakpoint was
		// found. This is the "this" reference of the REPO_CLASS.
		final ObjectReference thisRef = frames.get(0).thisObject();
		// Store the stack data into the fields of the "this" reference of the
		// REPO_CLASS and mark the instance as initialized.
		initializeRepo(thisRef, localsNames, localsValues, isCall, lineNumbers);
	}

	//
	// TYPES
	//

	private final class Client implements Runnable {
		//
		// DATA
		//

		public volatile boolean mustStop;
		private VirtualMachine vm;
		private BreakpointRequest breakpointRequest;
		private final Location location;
		private final EventQueue eventQueue;
		private final ClassType repoClassRef;
		private final ClassType stackFrameClassRef;
		private final ArrayType[] repoArrayFieldTypes;
		private final Field[] repoArrayFields;
		private final ArrayType stringArrayType;
		private final ArrayType objectArrayType;

		//
		// CONSTRUCTORS
		//

		private Client() {
			AttachingConnector connector = getConnector();
			try {
				vm = connect(connector, JDWPAgentClient.this.port);
			} catch (IOException e) {
				throw connectError("I/O exception", e);
			} catch (IllegalConnectorArgumentsException e) {
				throw connectError("illegal connector arguments", e);
			}
			try {
				location = getLocationOfClassMethod(vm, REPO_CLASS, "fetchInfo");
				eventQueue = vm.eventQueue();
				repoClassRef = getClassType(vm, REPO_CLASS);
				stackFrameClassRef = getClassType(vm, STACK_FRAME_CLASS);
				repoArrayFieldTypes = new ArrayType[] {
						getReferenceType(vm, LOCALS_NAME_FIELD_CLASS,
								ArrayType.class),
						getReferenceType(vm, LOCALS_VALUE_FIELD_CLASS,
								ArrayType.class),
						getReferenceType(vm, IS_CALL_FIELD_CLASS,
								ArrayType.class),
						getReferenceType(vm, LINE_NUMBERS_CLASS,
								ArrayType.class) };
				repoArrayFields = new Field[] {
						getFieldByName(repoClassRef, LOCALS_NAME_FIELD_NAME),
						getFieldByName(repoClassRef, LOCALS_VALUE_FIELD_NAME),
						getFieldByName(repoClassRef, IS_CALL_FIELD_NAME),
						getFieldByName(repoClassRef, LINE_NUMBERS_FIELD_NAME), };
				stringArrayType = getReferenceType(vm, String[].class,
						ArrayType.class);
				objectArrayType = getReferenceType(vm, Object[].class,
						ArrayType.class);
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
				synchronized (this) {
					assert null != JDWPAgentClient.this.runningThread;
					JDWPAgentClient.this.runningThread = null;
					notifyAll();
				}
				if (interrupted) {
					Thread.currentThread().interrupt();
				}
			}
		}
	}
}
