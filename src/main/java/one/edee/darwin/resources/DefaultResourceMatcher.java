package one.edee.darwin.resources;

/**
 * Default implementation of resource matcher. File names must start with lower cased {@link PatchType}.
 *
 * @author Jan Novotný, FG Forrest a.s. (c) 2007
 */
public class DefaultResourceMatcher implements ResourceMatcher {

	@Override
	public boolean isResourceAcceptable(PatchType type, String patchName) {
		switch(type) {
			case CREATE: return patchName.toLowerCase().startsWith("create");
			case EVOLVE: return patchName.toLowerCase().startsWith("patch");
			case GUESS: return patchName.toLowerCase().startsWith("guess");
		}

		return false;
	}

}
