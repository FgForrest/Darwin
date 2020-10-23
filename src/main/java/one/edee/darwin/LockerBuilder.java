package one.edee.darwin;

import one.edee.darwin.resources.ResourceAccessor;
import org.springframework.context.ApplicationContext;

/**
 * No documentation needed, just look at the methods.
 *
 * @author Jan Novotný (novotny@fg.cz), FG Forrest a.s. (c) 2016
 */
public class LockerBuilder {
	private final ApplicationContext applicationContext;
	private boolean skipIfDataSourceNotPresent = true;
	private boolean inSeparateTransaction;
	private String dataSourceName = "dataSource";
	private String transactionManagerName = "transactionManager";
	private ResourceAccessor resourceAccessor;

	public LockerBuilder(ApplicationContext applicationContext) {
		this.applicationContext = applicationContext;
	}

	public LockerBuilder withSkipIfDataSourceNotPresent(boolean skipIfDataSourceNotPresent) {
		this.skipIfDataSourceNotPresent = skipIfDataSourceNotPresent;
		return this;
	}

	public LockerBuilder withInSeparateTransaction(boolean inSeparateTransaction) {
		this.inSeparateTransaction = inSeparateTransaction;
		return this;
	}

	public LockerBuilder withDataSourceName(String dataSourceName) {
		this.dataSourceName = dataSourceName;
		return this;
	}

	public LockerBuilder withTransactionManagerName(String transactionManagerName) {
		this.transactionManagerName = transactionManagerName;
		return this;
	}

	public LockerBuilder withResourceAccessor(ResourceAccessor resourceAccessor) {
		this.resourceAccessor = resourceAccessor;
		return this;
	}

	public Locker build() {
		final Locker locker = new Locker();
		locker.setApplicationContext(applicationContext);
		locker.setSkipIfDataSourceNotPresent(skipIfDataSourceNotPresent);
		locker.setInSeparateTransaction(inSeparateTransaction);
		locker.setDataSourceName(dataSourceName);
		locker.setTransactionManagerName(transactionManagerName);
		locker.setResourceAccessor(resourceAccessor);
		return locker;
	}

}
