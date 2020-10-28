package one.edee.darwin;

import one.edee.darwin.locker.Locker;
import one.edee.darwin.model.SchemaVersion;
import one.edee.darwin.model.SchemaVersionProvider;
import one.edee.darwin.resources.DefaultResourceAccessor;
import one.edee.darwin.resources.DefaultResourceMatcher;
import one.edee.darwin.resources.DefaultResourceNameAnalyzer;
import one.edee.darwin.resources.ResourceAccessor;
import one.edee.darwin.resources.ResourceMatcher;
import one.edee.darwin.resources.ResourceNameAnalyzer;
import org.springframework.context.ApplicationContext;

/**
 * Builder factory for AutoUpdate
 *
 * @author Jan Novotný (novotny@fg.cz), FG Forrest a.s. (c) 2016
 */
public class DarwinBuilder {
	private final ApplicationContext ctx;
	private final SchemaVersionProvider componentDescriptor;
	private ResourceAccessor resourceAccessor;
	private boolean skipIfDataSourceNotPresent;
	private ResourceMatcher resourceMatcher = new DefaultResourceMatcher();
	private ResourceNameAnalyzer resourceNameAnalyzer = new DefaultResourceNameAnalyzer();
	private Locker locker;
	private String dataSourceName = "dataSource";
	private String transactionManagerName = "transactionManager";

	public DarwinBuilder(ApplicationContext ctx, String componentName, String componentVersion) {
		this.ctx = ctx;
		this.componentDescriptor = new SchemaVersion(componentName, componentVersion);
	}

	public DarwinBuilder withResourcePath(String resourcePath) {
		this.resourceAccessor = new DefaultResourceAccessor(ctx, "UTF-8", resourcePath);
		return this;
	}

	public DarwinBuilder withResourceAccessor(ResourceAccessor resourceAccessor) {
		this.resourceAccessor = resourceAccessor;
		return this;
	}

	public DarwinBuilder withSkipIfDataSourceNotPresent(boolean skipIfDataSourceNotPresent) {
		this.skipIfDataSourceNotPresent = skipIfDataSourceNotPresent;
		return this;
	}

	public DarwinBuilder withResourceMatcher(ResourceMatcher resourceMatcher) {
		this.resourceMatcher = resourceMatcher;
		return this;
	}

	public DarwinBuilder withResourceNameAnalyzer(ResourceNameAnalyzer resourceNameAnalyzer) {
		this.resourceNameAnalyzer = resourceNameAnalyzer;
		return this;
	}

	public DarwinBuilder withDataSourceName(String dataSourceName) {
		this.dataSourceName = dataSourceName;
		return this;
	}

	public DarwinBuilder withTransactionManagerName(String transactionManagerName) {
		this.transactionManagerName = transactionManagerName;
		return this;
	}

	public DarwinBuilder withLocker(Locker locker) {
		this.locker = locker;
		return this;
	}

	public Darwin build() {
		final Darwin darwin = new Darwin();
		darwin.setApplicationContext(ctx);
		darwin.setSkipIfDataSourceNotPresent(skipIfDataSourceNotPresent);
		darwin.setResourceMatcher(resourceMatcher);
		darwin.setResourceNameAnalyzer(resourceNameAnalyzer);
		darwin.setResourceAccessor(resourceAccessor);
		darwin.setComponentDescriptor(componentDescriptor);
		darwin.setDataSourceName(dataSourceName);
		darwin.setTransactionManagerName(transactionManagerName);
		darwin.setLocker(locker);

		return darwin;
	}

}
