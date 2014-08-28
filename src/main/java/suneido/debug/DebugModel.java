package suneido.debug;

import suneido.boot.Bootstrap;

/**
 * Enumerates debugging models available in jSuneido.
 *
 * @author Victor Schappert
 * @since 20140814
 */
public enum DebugModel {

	/**
	 * Full debugging, as described in {@link Bootstrap#DEBUG_OPTION_ALL}.
	 */
	ALL(Bootstrap.DEBUG_OPTION_ALL),
	/**
	 * Stack trace translation, as described in
	 * {@link Bootstrap#DEBUG_OPTION_STACK}.
	 */
	STACK(Bootstrap.DEBUG_OPTION_STACK),
	/**
	 * No extra debugging support, as described in
	 * {@link Bootstrap#DEBUG_OPTION_NONE}.
	 */
	NONE(Bootstrap.DEBUG_OPTION_NONE);

	//
	// DATA
	//

	private final String commandLineOption;

	//
	// CONSTRUCTORS
	//

	private DebugModel(String commandLineOption) {
		this.commandLineOption = commandLineOption;
	}

	//
	// STATICS
	//

	/**
	 * Converts a string given on the command line into the appropriate
	 * debug model enumerator.
	 *
	 * @param commandLineOption An option given on the command line
	 * @return Debug model
	 * @throws IllegalArgumentException If {@code commandLineOption} does not
	 *         correspond to any known debug model
	 */
	public static DebugModel fromCommandLineOption(String commandLineOption) {
		DebugModel[] values = values();
		for (DebugModel model : values) {
			if (model.commandLineOption.equals(commandLineOption)) {
				return model;
			}
		}
		StringBuilder error = new StringBuilder();
		error.append('\'').append(commandLineOption)
				.append("' is not a valid debug model: use ")
				.append(values[0].commandLineOption);
		for (int k = 1; k < values.length; ++k) {
			error.append('|').append(values[k].commandLineOption);
		}
		throw new IllegalArgumentException(error.toString());
	}

	private static DebugModel currentModel = null;

	/**
	 * <p>
	 * Returns the global debug model in effect.
	 * </p>
	 *
	 * @return
	 */
	public static DebugModel getCurrentModel() {
		return currentModel;
	}

	public static void setCurrentModel(DebugModel model) {
		if (null == model) {
			throw new NullPointerException();
		}
		currentModel = model;
	}
}
