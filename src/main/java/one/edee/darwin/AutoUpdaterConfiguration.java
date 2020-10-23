package one.edee.darwin;

import one.edee.darwin.resources.DefaultResourceAccessor;
import one.edee.darwin.resources.ResourceAccessor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import static one.edee.darwin.AutoUpdater.AUTOUPDATE_COMPONENT_NAME;
import static one.edee.darwin.AutoUpdater.AUTOUPDATE_COMPONENT_VERSION;

/**
 * No documentation needed, just look at the methods.
 *
 * @author Jan Novotný (novotny@fg.cz), FG Forrest a.s. (c) 2016
 */
@Configuration
public class AutoUpdaterConfiguration {
	@Autowired private ApplicationContext applicationContext;

	@Bean
	public AutoUpdater dbAutoUpdater() {
		return new AutoUpdaterBuilder(applicationContext, AUTOUPDATE_COMPONENT_NAME, AUTOUPDATE_COMPONENT_VERSION)
				.withSkipIfDataSourceNotPresent(true)
				.withResourceAccessor(dbAutoUpdateResourceAccessor())
				.withLocker(locker())
				.build();
	}

	@Bean
	public Locker locker() {
		return new LockerBuilder(applicationContext)
				.withSkipIfDataSourceNotPresent(true)
				.withResourceAccessor(dbAutoUpdateResourceAccessor())
				.build();
	}

	@Bean
	public ResourceAccessor dbAutoUpdateResourceAccessor() {
		return new DefaultResourceAccessor(
				applicationContext, "UTF-8",
				"classpath:META-INF/lib_db_autoupdate/sql/"
		);
	}

}
