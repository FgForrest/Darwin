package one.edee.darwin.resources;

/**
 * PatchType for {@link DefaultResourceMatcher}
 *
 * @author Radek Salay, FG Forest a.s. 6/27/16.
 */
public enum PatchType {

	/**
	 * Create patch represents initial model setup script.
	 */
	CREATE,
	/**
	 * Evolve patch represents model evolution script. Ie. patch that alters existing model in database.
	 */
	EVOLVE,
	/**
	 * Guess patch represents a read only script that detects proper model version from existing database.
	 */
	GUESS

}
