package one.edee.darwin.resources;

import freemarker.template.Configuration;
import freemarker.template.SimpleHash;
import freemarker.template.Template;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.io.StringReader;
import java.io.StringWriter;
import java.util.Map;

/**
 * This resouce accessor extend default resource accessor with ability to proces Freemarker scripts inside SQL
 * statements.
 *
 * @author Jan Novotný, FG Forrest a.s. (c) 2007
 * @version $Id$
 */
public class ScriptableResourceAccessor extends DefaultResourceAccessor {
	private static final Log log = LogFactory.getLog(ScriptableResourceAccessor.class);
	private static final Configuration cfg = new Configuration();
	private final SimpleHash vars = new SimpleHash();

	public void setVars(Map vars) {
		this.vars.putAll(vars);
	}

	@Override
    public String getTextContentFromResource(String resourcePath) {
		String text = super.getTextContentFromResource(resourcePath);
		if (text == null) {
			if (log.isWarnEnabled()) {
				log.warn("Can not find resource: " + resourcePath);
			}
			return null;
		} else {
			try {
				Template template = new Template(
						"dbAutoUpdateTempTemplate",
						new StringReader(text),
						cfg
				);
				StringWriter writer = new StringWriter(text.length() * 2);
				template.process(vars, writer);
				return writer.toString();
			} catch(Exception e) {
				String msg = "Error in SQL script preprocessing: " + e.getMessage();
				log.fatal(msg, e);
				throw new RuntimeException(msg, e);
			}
		}
	}

}
