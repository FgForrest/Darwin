package one.edee.darwin.storage;

import lombok.Data;
import one.edee.darwin.resources.DefaultResourceAccessor;
import one.edee.darwin.resources.ResourceAccessor;
import one.edee.darwin.utils.JdbcUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.ResourceLoaderAware;
import org.springframework.core.io.ResourceLoader;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.util.Assert;

import javax.sql.DataSource;

/**
 * Abstract ancestor for database oriented storage.
 *
 * @author Jan Novotnďż˝, FG Forrest a.s. (c) 2007
 * @version $Id$
 */
@Data
public abstract class AbstractDatabaseStorage implements InitializingBean, ResourceLoaderAware {
    public static final String MYSQL = "mysql";
    public static final String ORACLE = "oracle";
    public static final String H2 = "h2";
    public static final String MSSQL = "mssql";
    private static final Log log = LogFactory.getLog(AbstractDatabaseStorage.class);
    protected final DefaultResourceAccessor dbResourceAccessor = new DefaultResourceAccessor();
    protected ResourceAccessor resourceAccessor;
    protected PlatformTransactionManager transactionManager;
    protected JdbcTemplate jdbcTemplate;
    private String platform;

    public void setDataSource(DataSource dataSource) {
        this.jdbcTemplate = new JdbcTemplate(dataSource);
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        Assert.notNull(jdbcTemplate, "AutoUpdater needs JdbcTemplate to be not null!");
        Assert.notNull(resourceAccessor, "AutoUpdater needs dbResourceAccessor to be not null!");
    }

	@Override
	public void setResourceLoader(ResourceLoader resourceLoader) {
		this.dbResourceAccessor.setResourceLoader(resourceLoader);
	}

	public String getPlatform() {
        if (platform == null) {
            platform = JdbcUtils.getPlatformFromJdbcUrl(jdbcTemplate.getDataSource());
        }
        return platform;
    }



}
