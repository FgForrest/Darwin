package one.edee.darwin.utils.spring;

import one.edee.darwin.Darwin;
import one.edee.darwin.DarwinBuilder;
import one.edee.darwin.locker.Locker;
import one.edee.darwin.resources.ResourceAccessor;
import one.edee.darwin.resources.ResourceAccessorForTest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * No extra information provided - see (selfexplanatory) method signatures.
 * I have the best intention to write more detailed documentation but if you see this, there was not enough time or will to do so.
 *
 * @author Jan Novotný (novotny@fg.cz), FG Forrest a.s. (c) 2020
 */
@Configuration
public class DarwinTestConfiguration {

	@Bean
	public Darwin darwin(ApplicationContext applicationContext, @Value("${componentId}") String componentId, @Value("${componentVersion}") String version) {
		final DarwinBuilder darwin = new DarwinBuilder(applicationContext, componentId, version);
		darwin.withSkipIfDataSourceNotPresent(true);
		darwin.withResourceAccessor(realDarwinResourceAccessor());
		return darwin.build();
	}

	@Bean
	public Locker locker() {
		final Locker locker = new Locker();
		locker.setSkipIfDataSourceNotPresent(true);
		locker.setResourceAccessor(realDarwinResourceAccessor());
		return locker;
	}

	@Bean
	public ResourceAccessor realDarwinResourceAccessor() {
		final ResourceAccessorForTest resourceAccessorForTest = new ResourceAccessorForTest();
		resourceAccessorForTest.setResourcePath("classpath:/META-INF/darwin/sql/");
		return resourceAccessorForTest;
	}

}