package one.edee.darwin;

import lombok.Getter;
import lombok.Setter;
import lombok.extern.apachecommons.CommonsLog;
import one.edee.darwin.exception.ProcessIsLockedException;
import one.edee.darwin.locker.Locker;
import one.edee.darwin.model.Patch;
import one.edee.darwin.model.Platform;
import one.edee.darwin.model.SchemaVersion;
import one.edee.darwin.model.SchemaVersionProvider;
import one.edee.darwin.model.version.VersionComparator;
import one.edee.darwin.model.version.VersionDescriptor;
import one.edee.darwin.resources.DefaultResourceAccessor;
import one.edee.darwin.resources.DefaultResourceMatcher;
import one.edee.darwin.resources.DefaultResourceNameAnalyzer;
import one.edee.darwin.resources.PatchType;
import one.edee.darwin.resources.ResourceAccessor;
import one.edee.darwin.resources.ResourceMatcher;
import one.edee.darwin.resources.ResourceNameAnalyzer;
import one.edee.darwin.resources.ResourcePatchMediator;
import one.edee.darwin.storage.DarwinStorage;
import one.edee.darwin.storage.DefaultDatabaseDarwinStorage;
import one.edee.darwin.storage.DefaultDatabaseStorageChecker;
import one.edee.darwin.storage.DefaultDatabaseStorageUpdater;
import one.edee.darwin.storage.StorageChecker;
import one.edee.darwin.storage.StorageUpdater;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.support.AbstractApplicationContext;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallbackWithoutResult;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.util.Assert;

import javax.sql.DataSource;
import java.time.LocalDateTime;

/**
 * Darwin allows you to automatically update your database layer structure. It tracks version of the data model
 * that is currently available in your database and automatically applies patches as the component version grows
 * with newer versions of your software.
 * <p>
 * In short it has following features:
 * <p>
 * - when there are no tables for your component, it uses create.sql script to setup them
 * - when there exists data layer for your component, but newer version of the component starts up, it looks up for
 * patches that fill the gap between current structure of data layer and structure that is expected for new version
 * - when suddenly old patch appears (ie. data layer is in version 2.0 but patch_1.6.sql appears) it looks up whether
 * it was applied and when not it tries to apply it ... this allows parallel development in several branches sharing
 * the same database (assuming patches are not conflicting)
 * - it recovers from errors in patch in a way that it stores information about SQL commands successfully executed so that
 * when applying the same patch again (after it has been corrected by developer) it skips commands already executed and
 * jumps straight to the one that failed last time
 *
 * @author Jan Novotný, FG Forrest a.s. (c) 2007
 */
@CommonsLog
public class Darwin implements InitializingBean, ApplicationContextAware {
	public static final String DARWIN_COMPONENT_NAME = "darwin";
	public static final String DARWIN_COMPONENT_VERSION = "1.0";

	@Getter @Setter private String dataSourceName = "dataSource";
	@Getter @Setter private String transactionManagerName = "transactionManager";
	@Getter @Setter private ResourceAccessor resourceAccessor;
	@Getter @Setter private SchemaVersionProvider componentDescriptor;
	@Getter @Setter private boolean skipIfDataSourceNotPresent;
	@Getter @Setter private boolean switchOff;
	@Getter @Setter private Locker locker;
	@Getter @Setter private ResourceMatcher resourceMatcher = new DefaultResourceMatcher();
	@Getter @Setter private ResourceNameAnalyzer resourceNameAnalyzer = new DefaultResourceNameAnalyzer();
	@Getter @Setter private ResourcePatchMediator resourcePatchMediator;
	@Getter @Setter private DarwinStorage darwinStorage;
	@Getter @Setter private StorageUpdater storageUpdater;
	@Getter @Setter private StorageChecker storageChecker;
	@Getter @Setter private PlatformTransactionManager transactionManager;
	@Getter private ApplicationContext applicationContext;

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.applicationContext = applicationContext;
    }

	/**
	 * Executes darwin instance logic.
	 */
	public void apply() {
    	afterPropertiesSet();
	}

	/**
	 * Initializes Darwin and updates infrastructure tables for it.
	 * Must be called prior calling {@link #darwinComponent(String, String)}
	 */
	@Override
	public void afterPropertiesSet() {
		initDefaults();
		if (!isSwitchOff()) {
			//darwin itself first
			updateMyself();

			//darwin target component
			darwinComponent(componentDescriptor.getComponentName(), componentDescriptor.getComponentVersion());
		}
	}

	/**
     * This method will initializes default objects needed for autoupdating.
     * Only dbResourceAccessor, resourceMather and resourceNameAnalyzer needs to be set in order to use Darwin.
     * Other objects are created and wired by this method.
     */
    public void initDefaults() {
        Assert.notNull(resourceAccessor, "Darwin needs dbResourceAccessor property to be not null!");
        Assert.notNull(resourceMatcher, "Darwin needs resourceMatcher property to be not null!");
        Assert.notNull(resourceNameAnalyzer, "Darwin needs resourceNameAnalyzer property to be not null!");

        //defaults
		final ConfigurableListableBeanFactory beanFactory = ((AbstractApplicationContext) applicationContext).getBeanFactory();
		boolean dataSourcePresent = beanFactory.containsBean(dataSourceName);
        boolean transactionManagerPresent = beanFactory.containsBean(transactionManagerName);
        boolean lockerPresent = beanFactory.containsBean("locker") && beanFactory.getBean("locker") instanceof Locker;
        if (dataSourcePresent) {
            // comprehensive check if there isn't only ExternalDependency based proxy
            try {
                DataSource bean = applicationContext.getBean(dataSourceName, DataSource.class);
                bean.toString();
            } catch (NoSuchBeanDefinitionException e) {
                dataSourcePresent = false;
                transactionManagerPresent = false;
            }
        }
        if (dataSourcePresent) {
            DataSource ds = (DataSource) applicationContext.getBean(dataSourceName);
            transactionManager = transactionManagerPresent ?
					(PlatformTransactionManager) applicationContext.getBean(transactionManagerName) : null;

			resourcePatchMediator = new ResourcePatchMediator(resourceMatcher, resourceNameAnalyzer);

			if (storageChecker == null) {
				final DefaultDatabaseStorageChecker defaultChecker = new DefaultDatabaseStorageChecker(resourcePatchMediator);
				defaultChecker.setDataSource(ds);
				defaultChecker.setTransactionManager(transactionManager);
				defaultChecker.setResourceAccessor(resourceAccessor);
				defaultChecker.setResourceMatcher(resourceMatcher);
				defaultChecker.setResourceNameAnalyzer(resourceNameAnalyzer);
				defaultChecker.setResourceLoader(applicationContext);
				storageChecker = defaultChecker;
			}
            if (darwinStorage == null) {
				final DefaultDatabaseDarwinStorage defaultPersister = new DefaultDatabaseDarwinStorage(resourceNameAnalyzer, storageChecker);
				defaultPersister.setDataSource(ds);
                defaultPersister.setTransactionManager(transactionManager);
				defaultPersister.setResourceLoader(applicationContext);
				darwinStorage = defaultPersister;
			}
            if (storageUpdater == null) {
				final DefaultDatabaseStorageUpdater defaultUpdater = new DefaultDatabaseStorageUpdater(storageChecker);
				defaultUpdater.setResourceAccessor(resourceAccessor);
                defaultUpdater.setDataSource(ds);
                defaultUpdater.setTransactionManager(transactionManager);
				defaultUpdater.setResourceLoader(applicationContext);
				storageUpdater = defaultUpdater;
			}
            if (locker == null) {
				if (lockerPresent) {
					locker = applicationContext.getBean("locker", Locker.class);
				} else {
					locker = new Locker();
					locker.setResourceAccessor(resourceAccessor);
					locker.setApplicationContext(applicationContext);
					locker.setSkipIfDataSourceNotPresent(skipIfDataSourceNotPresent);
					locker.setDataSourceName(dataSourceName);
					locker.setTransactionManagerName(transactionManagerName);
					locker.afterPropertiesSet();
				}
            }
        } else {
            if (!skipIfDataSourceNotPresent) {
                throw new IllegalStateException("DataSource not accessible and skipIfDataSourceNotPresent" +
                        " flag is not set. Cannot perform database check (and possible update).");
            } else {
                switchOff = true;
            }
        }
    }

    /**
     * Performs darwin of specified component to specified version.
     * Uses default resourceMatcher and resourceNameAnalyzer.
     */
    public void darwinComponent(String componentName, String componentVersion) {
        darwinComponent(componentName, componentVersion, this.resourceMatcher, this.resourceNameAnalyzer);
    }

    /**
     * Darwin serge version, in DB or try guest it. If find nothing then will crate new storage.
     *
     * @param componentName          Name of component which we want update
     * @param componentVersionString version o which we update component, MAX version
     * @param resourceMatcher        {@link ResourceMatcher}
     * @param resourceNameAnalyzer   {@link ResourceNameAnalyzer}
     */
    public void darwinComponent(final String componentName, String componentVersionString,
                                    final ResourceMatcher resourceMatcher,
                                    final ResourceNameAnalyzer resourceNameAnalyzer) {
		if(switchOff) {
			if(log.isDebugEnabled()) {
				log.debug("Darwin is switched off - no data source accessible.");
			}
		} else {
			final VersionComparator versionComparator = new VersionComparator();
			final VersionDescriptor lastStoredVersion = darwinStorage.getVersionDescriptorForComponent(componentName);
			final VersionDescriptor currentVersion = new VersionDescriptor(componentVersionString);

			ensureRunsUniquely(
					componentName, lastStoredVersion, versionComparator,
					new Runnable() {
						@Override
						public void run() {
						doUpdateComponent(
								lastStoredVersion, componentName, resourceMatcher,
								versionComparator, currentVersion, resourceNameAnalyzer
						);
						}
					}
			);
		}


    }

	/**
	 * This method is used to setup extra Darwin instance that looks up to the Classpath and initializes data
	 * structures for Darwin itself.
	 */
	private void updateMyself() {
		final DefaultResourceAccessor resourceAccessor = new DefaultResourceAccessor(
				applicationContext, "UTF-8", "classpath:/META-INF/lib_db_darwin/sql/"
		);
		final SchemaVersion myVersion = new SchemaVersion(DARWIN_COMPONENT_NAME, DARWIN_COMPONENT_VERSION);
		final Darwin meUpdater = new Darwin();
		meUpdater.setApplicationContext(applicationContext);
		meUpdater.setComponentDescriptor(myVersion);
		meUpdater.setDataSourceName(dataSourceName);
		meUpdater.setTransactionManagerName(transactionManagerName);
		meUpdater.setSkipIfDataSourceNotPresent(skipIfDataSourceNotPresent);
		meUpdater.setResourceAccessor(resourceAccessor);
		meUpdater.initDefaults();
		meUpdater.updateMySelf(myVersion);
	}

    /**
     * Updates specially Darwin itself. It must apply different logic, because internal logic was changed during
     * life of the library and now uses PATCH table to determine which patches has been already applied. So there is
     * special jump from version 1.1 to 3.0 in its internal data layer.
     */
    private void updateMySelf(SchemaVersion desiredDarwin) {
		final String componentName = desiredDarwin.getComponentName();
		final VersionDescriptor existingVersion = darwinStorage.getVersionDescriptorForComponent(componentName);
		final VersionComparator comparator = new VersionComparator();
		final VersionDescriptor version_3_0 = new VersionDescriptor("3.0");
		final VersionDescriptor version_1_1 = new VersionDescriptor("1.1");
		if (existingVersion != null &&
				comparator.compare(existingVersion, version_1_1) >= 0 &&
				comparator.compare(existingVersion, version_3_0) < 0) {

			ensureRunsUniquely(
					componentName, existingVersion, comparator,
					new Runnable() {
						@Override
						public void run() {
							storageUpdater.executeScript(
									storageChecker.getPlatform() + "/patch_3.0.sql",
									componentName, darwinStorage, storageChecker
							);
							darwinStorage.updateVersionDescriptorForComponent(componentName, "3.0");
						}
					}
			);

        }
        darwinComponent(componentName, desiredDarwin.getComponentVersion());
    }

    /**
     * Method which find version of component, or guess it, or create new record about component
     * <p>
     * most needed parameter is on {@link Darwin} himself
     */
    private void doUpdateComponent(final VersionDescriptor lastStoredVersion, final String componentName,
                                   final ResourceMatcher resourceMatcher, final VersionComparator versionComparator,
                                   final VersionDescriptor currentVersion, final ResourceNameAnalyzer resourceNameAnalyzer) {
        //if stored version is null create new storage
        final Platform platform = storageChecker.getPlatform();
        //we should try to envelope operation with transaction boundary - some database engines allows to rollback
        final TransactionTemplate transaction = new TransactionTemplate(transactionManager);
        final VersionDescriptor storedVersion = transaction.execute(status -> {
            VersionDescriptor resultVersion = lastStoredVersion;
            if (resultVersion == null) {
                //try to guess version of storage if no record found
                if (storageChecker != null) {
                    resultVersion = storageChecker.guessVersion(componentName, darwinStorage);
						log.info("No version record found for this component." +
								" Guess script detected version: " + resultVersion);
                }
                //if we still have no record about version, we will try to set up new storage
                if (resultVersion == null) {
                    createNewStorage(componentName, platform, resourceMatcher);
                } else {
                    //just update record of stored version
						log.info("There already exists version " + resultVersion + " of the component in database." +
								" Guess script matched it. Storing info to database.");
                    darwinStorage.updateVersionDescriptorForComponent(componentName, resultVersion.toString());
                }
            }
            return resultVersion;
        });

        //update component's storage, if version in DB is smaller then version of component
        updateStorage(
			storedVersion, componentName, currentVersion, platform,
			resourceMatcher, resourceNameAnalyzer, versionComparator
		);

    }

    /**
     * Method which update the component, method use all patches which find on classpath
     * most needed parameter is on {@link Darwin} himself
     */
    private void updateStorage(final VersionDescriptor lastStoredVersion, final String componentName,
                               final VersionDescriptor currentVersion, final Platform platform,
                               final ResourceMatcher resourceMatcher, final ResourceNameAnalyzer resourceNameAnalyzer,
                               final VersionComparator versionComparator) {
        //get appropriate patches
        final Patch[] patches = resourcePatchMediator.getPatches(
        		resourceAccessor.getSortedResourceList(platform),
                componentName, platform, darwinStorage, storageChecker,
				PatchType.EVOLVE
		);

        if (patches != null) {
            fillMissingPatchesForComponentsCreatedBeforePatchTableWasAvailable(patches, componentName, versionComparator, lastStoredVersion);
            for (final Patch patch : patches) {
                //we should try to envelope operation with transaction boundary - some database engines allows to rollback
                TransactionTemplate transaction = new TransactionTemplate(transactionManager);
                transaction.execute(new TransactionCallbackWithoutResult() {
                    @Override
                    protected void doInTransactionWithoutResult(TransactionStatus status) {
                        if (
                        		resourceMatcher.isResourceAcceptable(PatchType.EVOLVE, patch.getPatchName()) ||
								resourceMatcher.isResourceAcceptable(PatchType.CREATE, patch.getPatchName())
						) {
                            if (versionComparator.compare(resourceNameAnalyzer.getVersionFromPatch(patch), currentVersion) <= 0) {
	                            if (!darwinStorage.isPatchFinishedInDb(patch)) {
                                    VersionDescriptor resourceVersion = resourceNameAnalyzer.getVersionFromPatch(patch);
		                            try {
			                            if (storageChecker.guessPatchAlreadyApplied(componentName, darwinStorage, resourceVersion)) {
				                            log.info("Component " + componentName + " marked as updated to version " + resourceVersion + " because guessing logic matched database contents.");
				                            darwinStorage.markPatchAsFinished(patch);
			                            } else {
				                            storageUpdater.executeScript(patch.getResourcesPath(), componentName,
						                            darwinStorage, storageChecker);
				                            log.info("Component " + componentName + " storage updated to version " + resourceVersion + ".");
			                            }
			                            //update stored version only when resource version is lesser than lastStoredVersion
			                            //and when everything went ok
			                            if (lastStoredVersion == null ||
					                            versionComparator.compare(resourceVersion, lastStoredVersion) > 0) {
			                            	if (resourceVersion!=null) {
												darwinStorage.updateVersionDescriptorForComponent(componentName,
														resourceVersion.toString());
											}
			                            }
		                            } catch (Exception ex) {
			                            log.error("Failed to update " + componentName + " storage to version " + resourceVersion + ": " + ex.getMessage());
			                            throw ex;
		                            }
                                }
                            }
                        }
                    }
                });
            }
        }
    }

    /**
     * If DARWIN has record about component but there is no record in DARWIN_PATCH for this component
     * we will believe that all patches has been successfully applied and mark them as done in DARWIN_PATCH
     * table.
     *
     * @param patches           list of patches available
     * @param componentName     name of component witch is darwind
     * @param versionComparator version of component which is present in DARWIN
     */
    private void fillMissingPatchesForComponentsCreatedBeforePatchTableWasAvailable(final Patch[] patches,
                                                                                    final String componentName,
                                                                                    final VersionComparator versionComparator,
                                                                                    final VersionDescriptor storedVersion) {
        if (storedVersion != null && storageChecker.existPatchAndSqlTable()) {
            if (darwinStorage.isAnyPatchRecordedFor(componentName)) {
                // because there is some record in DARWIN_PATCH table, initial setup check only create script
				// that was skipped in previous versions
				for (final Patch patch : patches) {
					if (resourceMatcher.isResourceAcceptable(PatchType.CREATE, patch.getPatchName())) {
						if (!darwinStorage.isPatchRecordedByResourcePath(patch.getResourcesPath(),patch.getComponentName())) {
							// update only if not created before as failed
							TransactionTemplate transaction = new TransactionTemplate(transactionManager);
							transaction.execute(new TransactionCallbackWithoutResult() {
								@Override
								protected void doInTransactionWithoutResult(TransactionStatus status) {
									darwinStorage.markPatchAsFinished(
											darwinStorage.insertPatchToDatabase(
													patch.getPatchName(), patch.getComponentName(), LocalDateTime.now(), storageChecker.getPlatform()
											)
									);
								}
							});
						}
					}
				}
            } else {
                /*
                  because there is no record about component, but we have record in DARWIN then we add
				  to DARWIN_PATCH all patches which have lesser version than record in DARWIN
                */
				TransactionTemplate transaction = new TransactionTemplate(transactionManager);
				transaction.execute(new TransactionCallbackWithoutResult() {
					@Override
					protected void doInTransactionWithoutResult(TransactionStatus status) {
						for(Patch patch : patches) {
							if(
									resourceMatcher.isResourceAcceptable(PatchType.EVOLVE, patch.getPatchName()) ||
									resourceMatcher.isResourceAcceptable(PatchType.CREATE, patch.getPatchName())
							) {
								if(versionComparator.compare(resourceNameAnalyzer.getVersionFromPatch(patch), storedVersion) <= 0) {
									darwinStorage.markPatchAsFinished(
											darwinStorage.insertPatchToDatabase(
													patch.getPatchName(), patch.getComponentName(), LocalDateTime.now(), storageChecker.getPlatform()
											)
									);
								}
							}
						}
					}
				});
            }
        }
    }

    /**
     * Creates new storage of a component.
     */
    private void createNewStorage(final String componentName, Platform platform, final ResourceMatcher resourceMatcher) {
        if (log.isDebugEnabled()) {
            log.debug("No component " + componentName + " storage found. Creating new one.");
        }
        //get appropriate patches
        Patch[] patches = resourcePatchMediator.getPatches(
        		resourceAccessor.getSortedResourceList(platform),
                componentName, platform, darwinStorage, storageChecker,
				PatchType.CREATE
		);
        if (patches != null) {
            for (final Patch patch : patches) {
				//we should try to envelope operation with transaction boundary - some database engines allows to rollback
				TransactionTemplate transaction = new TransactionTemplate(transactionManager);
				transaction.execute(new TransactionCallbackWithoutResult() {
					@Override
					protected void doInTransactionWithoutResult(TransactionStatus status) {
						if(resourceMatcher.isResourceAcceptable(PatchType.CREATE, patch.getPatchName())) {
							try {
								storageUpdater.executeScript(patch.getResourcesPath(), componentName, darwinStorage,
										storageChecker);
								log.info("Component " + componentName + " initial version of storage created.");
							} catch(Exception ex) {
								log.error("Failed to create initial version of " + componentName + " storage: " + ex.getMessage());
								throw ex;
							}
						}
					}
				});
            }

            if (log.isDebugEnabled()) {
                log.debug("Creating new storage for component " + componentName + " finished.");
            }
        }
    }

	/**
	 * Ensures that passed function is executed uniquely in the entire cluster.
	 * @param componentName
	 * @param existingVersion
	 * @param comparator
	 * @param logic
	 */
    private void ensureRunsUniquely(String componentName, VersionDescriptor existingVersion, VersionComparator comparator, Runnable logic) {
		final String processName = getLockProcessName(componentName);
		String unlockKey = null;
		try {

			unlockKey = acquireProcessLockKey(componentName, existingVersion, comparator, null, processName);
			logic.run();

		} catch(ProcessIsLockedException ignored) {
			String msg = "Skipping darwin - another thread is performing darwin for component " + componentName;
			log.error(msg);
		} finally {
			//release lock
			if(unlockKey != null) {
				try {
					locker.releaseProcess(processName, unlockKey);
				} catch (ProcessIsLockedException e) {
					throw new IllegalStateException(
						"Process " + processName + " cannot be unlocked with " + unlockKey + " key!", e
					);
				}
			}
		}
	}

    /**
     * Method will try to acquire lock for auto update process. If it fails, it tries several times with some
     * timeout to let original thread to unlock the process.
     *
     * @throws ProcessIsLockedException
     */
    private String acquireProcessLockKey(String componentName, VersionDescriptor storedVersion,
                                         VersionComparator versionComparator, String unlockKey, String processName)
            throws ProcessIsLockedException {

        if (lockFunctionalityAvailable(componentName, storedVersion, versionComparator)) {
            int i = 0;
            ProcessIsLockedException lastException = null;
            while (unlockKey == null && i < 20) {
                try {
                    i++;
                    unlockKey = locker.leaseProcess(processName, LocalDateTime.now().plusMinutes(2), 5000);
                } catch (ProcessIsLockedException ex) {
                    lastException = ex;
                    if (log.isInfoEnabled()) {
                        log.info("Process " + processName + " is currently locked ... try " + i + " out of " + 20);
                    }
                }
            }

            if (unlockKey == null) {
                throw lastException;
            }
        }

        return unlockKey;
    }

    /**
     * Check whether lock functionality is available (available since 1.1 of darwin component).
     */
    private boolean lockFunctionalityAvailable(String componentName, VersionDescriptor storedVersion,
                                               VersionComparator versionComparator) {
        return !(DARWIN_COMPONENT_NAME.equals(componentName) && (storedVersion == null ||
                versionComparator.compare(storedVersion, new VersionDescriptor("1.1")) < 1));
    }

    /**
     * Returns auto update process name.
     */
    private String getLockProcessName(String componentName) {
        return componentName + ":darwinProcess";
    }
}