package one.edee.darwin.model.version;

import java.io.Serializable;
import java.util.Comparator;
import java.util.Iterator;

/**
 * Compares two VersionDescriptor objects.
 *
 * @author Jan Novotný, FG Forrest a.s. (c) 2007
 */
public class VersionComparator implements Comparator<VersionDescriptor>, Serializable {
	private static final long serialVersionUID = -7445439954199588183L;

	/**
	 * Compares its two arguments for order.  Returns a negative integer,
	 * zero, or a positive integer as the first argument is less than, equal
	 * to, or greater than the second.<p>
	 *
	 * The implementor must ensure that <tt>sgn(compare(x, y)) ==
	 * -sgn(compare(y, x))</tt> for all <tt>x</tt> and <tt>y</tt>.  (This
	 * implies that <tt>compare(x, y)</tt> must throw an exception if and only
	 * if <tt>compare(y, x)</tt> throws an exception.)<p>
	 *
	 * The implementor must also ensure that the relation is transitive:
	 * <tt>((compare(x, y)&gt;0) &amp;&amp; (compare(y, z)&gt;0))</tt> implies
	 * <tt>compare(x, z)&gt;0</tt>.<p>
	 *
	 * Finally, the implementer must ensure that <tt>compare(x, y)==0</tt>
	 * implies that <tt>sgn(compare(x, z))==sgn(compare(y, z))</tt> for all
	 * <tt>z</tt>.<p>
	 *
	 * It is generally the case, but <i>not</i> strictly required that
	 * <tt>(compare(x, y)==0) == (x.equals(y))</tt>.  Generally speaking,
	 * any comparator that violates this condition should clearly indicate
	 * this fact.  The recommended language is "Note: this comparator
	 * imposes orderings that are inconsistent with equals."
	 *
	 * @param o1 the first object to be compared.
	 * @param o2 the second object to be compared.
	 * @return a negative integer, zero, or a positive integer as the
	 *         first argument is less than, equal to, or greater than the
	 *         second.
	 * @throws ClassCastException if the arguments' types prevent them from
	 *                            being compared by this Comparator.
	 */
	public int compare(VersionDescriptor o1, VersionDescriptor o2) {

		if (o1 == null && o2 != null) return -1;
		if (o2 == null) throw new IllegalArgumentException("Version to be compared against (second parameter) cannot be null!");

		final Iterator<Serializable> itAlfa = o1.getIdentification().iterator();
		final Iterator<Serializable> itBeta = o2.getIdentification().iterator();
        Object lastAlfa = null;
		while(itAlfa.hasNext()) {
			final Object alfaVersion = itAlfa.next();
			Object betaVersion = itBeta.hasNext() ? itBeta.next() : null;

			if(betaVersion == null) {
				if (alfaVersion instanceof Integer) {
					betaVersion = 0;
				} else {
					return -1;
				}
			}

			if(alfaVersion instanceof Integer) {
				if(betaVersion instanceof Integer) {
					if((Integer) alfaVersion > (Integer) betaVersion) return 1;
					else if((Integer) alfaVersion < (Integer) betaVersion) return -1;
				}
				else {
					return 1;
				}
			}
			else {
				if (betaVersion instanceof Integer) {
					return -1;
				} else {
					int result = ((String)alfaVersion).compareToIgnoreCase((String)betaVersion);
					if (result > 0) return 1;
					if (result < 0) return -1;
				}
			}
            lastAlfa = alfaVersion;
		}

        if(itBeta.hasNext()) {
            Object nextVersion = itBeta.next();
            if (lastAlfa instanceof Integer) {
                return (nextVersion instanceof Integer)?-1:1;
            }
            else {
                return -1;
            }
        }

		return 0;
	}
}
