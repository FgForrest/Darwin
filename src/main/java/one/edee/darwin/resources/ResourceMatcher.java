package one.edee.darwin.resources;

/**
 * Implementation of this interface filters patches according their {@link PatchType}.
 *
 * @author Jan Novotný, FG Forrest a.s. (c) 2007
 */
public interface ResourceMatcher {

	/**
	 * This method compare patch if he corresponds with presumed type.
	 *
	 * @param type type of patch we presumed.
	 * @param patchName file name of the patch
	 * @return return true if is patch corresponds type, or false if not
	 * @see PatchType
	 */
    boolean isResourceAcceptable(PatchType type, String patchName);

}
