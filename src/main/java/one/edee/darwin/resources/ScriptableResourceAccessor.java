package one.edee.darwin.resources;

import freemarker.template.Configuration;
import freemarker.template.SimpleHash;
import freemarker.template.Template;
import lombok.extern.apachecommons.CommonsLog;
import org.springframework.core.io.ResourceLoader;

import java.io.StringReader;
import java.io.StringWriter;
import java.util.Map;

/**
 * This resource accessor extend default resource accessor with ability to proces Freemarker scripts inside SQL
 * statements.
 *
 * @author Jan Novotný, FG Forrest a.s. (c) 2007
 */
@CommonsLog
public class ScriptableResourceAccessor extends DefaultResourceAccessor {
	private static final Configuration CONFIGURATION = new Configuration(Configuration.VERSION_2_3_28);
	private final SimpleHash vars = new SimpleHash(CONFIGURATION.getObjectWrapper());

	public ScriptableResourceAccessor() {
	}

	public ScriptableResourceAccessor(ResourceLoader resourceLoader, String encoding, String resourcePath) {
		super(resourceLoader, encoding, resourcePath);
	}

	public void setVars(Map<String, Object> vars) {
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
						"darwinUpdateTempTemplate",
						new StringReader(text),
						CONFIGURATION
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
