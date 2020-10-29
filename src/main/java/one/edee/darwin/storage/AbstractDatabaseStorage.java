package one.edee.darwin.storage;

import lombok.Getter;
import lombok.Setter;
import lombok.extern.apachecommons.CommonsLog;
import one.edee.darwin.model.Platform;
import one.edee.darwin.resources.DefaultResourceAccessor;
import one.edee.darwin.resources.ResourceAccessor;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.ResourceLoaderAware;
import org.springframework.core.io.ResourceLoader;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.util.Assert;

import javax.sql.DataSource;
import java.util.Objects;

/**
 * Abstract ancestor for database oriented storage.
 *
 * @author Jan Novotný, FG Forrest a.s. (c) 2007
 */
@CommonsLog
public abstract class AbstractDatabaseStorage implements InitializingBean, ResourceLoaderAware {
    protected final DefaultResourceAccessor dbResourceAccessor = new DefaultResourceAccessor();
    @Getter @Setter protected ResourceAccessor resourceAccessor;
    @Getter @Setter protected PlatformTransactionManager transactionManager;
    @Getter @Setter protected JdbcTemplate jdbcTemplate;
    private Platform platform;

    @Override
    public void afterPropertiesSet() {
        Assert.notNull(jdbcTemplate, "Darwin needs JdbcTemplate to be not null!");
        Assert.notNull(resourceAccessor, "Darwin needs dbResourceAccessor to be not null!");
    }

    public void setDataSource(DataSource dataSource) {
        this.jdbcTemplate = new JdbcTemplate(dataSource);
    }

	@Override
	public void setResourceLoader(ResourceLoader resourceLoader) {
		this.dbResourceAccessor.setResourceLoader(resourceLoader);
	}

	public Platform getPlatform() {
        if (platform == null) {
            platform = Platform.getPlatformFromJdbcUrl(
                    Objects.requireNonNull(jdbcTemplate.getDataSource())
            );
        }
        return platform;
    }



}
