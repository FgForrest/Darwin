package one.edee.darwin.locker;

/**
 * The HostProvider interface represents a provider that can provide information about hosts.
 * Implementations of this interface can provide different ways to retrieve host information.
 */
public interface InstanceIdProvider {
    String DEFAULT_INSTANCE_ID = "DEFAULT";
    String INSTANCE_DELIMITER = "_";

    String getInstanceId();
}
