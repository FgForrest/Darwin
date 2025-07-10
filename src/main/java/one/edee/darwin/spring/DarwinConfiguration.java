package one.edee.darwin.spring;

import one.edee.darwin.Darwin;
import one.edee.darwin.DarwinBuilder;
import one.edee.darwin.locker.Locker;
import one.edee.darwin.locker.LockerBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * No documentation needed, just look at the methods.
 *
 * @author Jan Novotný (novotny@fg.cz), FG Forrest a.s. (c) 2016
 */
@Configuration
public class DarwinConfiguration {
	@Autowired private ApplicationContext applicationContext;

	@Bean
	public Darwin darwin() {
		return new DarwinBuilder(applicationContext, Darwin.DARWIN_COMPONENT_NAME, Darwin.DARWIN_COMPONENT_VERSION)
				.withSkipIfDataSourceNotPresent(true)
				.withLocker(darwinLocker())
				.build();
	}

	@Bean
	public Locker darwinLocker() {
		return new LockerBuilder(applicationContext)
				.withSkipIfDataSourceNotPresent(true)
				.build();
	}

}
