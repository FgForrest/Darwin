package one.edee.darwin;

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
public class AutoUpdaterBuilder {
	private final ApplicationContext ctx;
	private final VersionProvider componentDescriptor;
	private ResourceAccessor resourceAccessor;
	private boolean skipIfDataSourceNotPresent;
	private ResourceMatcher resourceMatcher = new DefaultResourceMatcher();
	private ResourceNameAnalyzer resourceNameAnalyzer = new DefaultResourceNameAnalyzer();
	private Locker locker;
	private String dataSourceName = "dataSource";
	private String transactionManagerName = "transactionManager";

	public AutoUpdaterBuilder(ApplicationContext ctx, String componentName, String componentVersion) {
		this.ctx = ctx;
		this.componentDescriptor = new AutoUpdaterInfo(componentName, componentVersion);
	}

	public AutoUpdaterBuilder withResourcePath(String resourcePath) {
		this.resourceAccessor = new DefaultResourceAccessor(ctx, "UTF-8", resourcePath);
		return this;
	}

	public AutoUpdaterBuilder withResourceAccessor(ResourceAccessor resourceAccessor) {
		this.resourceAccessor = resourceAccessor;
		return this;
	}

	public AutoUpdaterBuilder withSkipIfDataSourceNotPresent(boolean skipIfDataSourceNotPresent) {
		this.skipIfDataSourceNotPresent = skipIfDataSourceNotPresent;
		return this;
	}

	public AutoUpdaterBuilder withResourceMatcher(ResourceMatcher resourceMatcher) {
		this.resourceMatcher = resourceMatcher;
		return this;
	}

	public AutoUpdaterBuilder withResourceNameAnalyzer(ResourceNameAnalyzer resourceNameAnalyzer) {
		this.resourceNameAnalyzer = resourceNameAnalyzer;
		return this;
	}

	public AutoUpdaterBuilder withDataSourceName(String dataSourceName) {
		this.dataSourceName = dataSourceName;
		return this;
	}

	public AutoUpdaterBuilder withTransactionManagerName(String transactionManagerName) {
		this.transactionManagerName = transactionManagerName;
		return this;
	}

	public AutoUpdaterBuilder withLocker(Locker locker) {
		this.locker = locker;
		return this;
	}

	public AutoUpdater build() {
		final AutoUpdater autoUpdater = new AutoUpdater();
		autoUpdater.setApplicationContext(ctx);
		autoUpdater.setSkipIfDataSourceNotPresent(skipIfDataSourceNotPresent);
		autoUpdater.setResourceMatcher(resourceMatcher);
		autoUpdater.setResourceNameAnalyzer(resourceNameAnalyzer);
		autoUpdater.setResourceAccessor(resourceAccessor);
		autoUpdater.setComponentDescriptor(componentDescriptor);
		autoUpdater.setDataSourceName(dataSourceName);
		autoUpdater.setTransactionManagerName(transactionManagerName);
		autoUpdater.setLocker(locker);

		return autoUpdater;
	}

}
