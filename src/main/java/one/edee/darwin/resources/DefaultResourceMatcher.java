package one.edee.darwin.resources;

/**
 * Default implementation of resource matcher.
 *
 * @author Jan Novotný, FG Forrest a.s. (c) 2007
 * @version $Id$
 */
public class DefaultResourceMatcher implements ResourceMatcher {

	@Override
	public boolean isResourceAcceptable(PatchMode mode, String patchName) {
		switch(mode) {
			case Create: return patchName.toLowerCase().startsWith("create");
			case Patch: return patchName.toLowerCase().startsWith("patch");
			case Guess: return patchName.toLowerCase().startsWith("guess");
		}

		return false;
	}

}
