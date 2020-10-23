package one.edee.darwin.resources;

/**
 * Distinguishes between resources in different modes.
 *
 * @author Jan Novotný, FG Forrest a.s. (c) 2007
 * @version $Id$
 */
public interface ResourceMatcher {

	/**
	 * This method compare patch if he corresponds with presumed mode.
	 *
	 * @param mode mode of patch we presumed.
	 * @param patchName
	 * @return return true if is patch corresponds mode, or false if not.
	 * @see {@link PatchMode}
	 */
    boolean isResourceAcceptable(PatchMode mode, String patchName);

}
