package one.edee.darwin.locker;

/**
 * An interface for providing instance IDs. Implementations must provide the getInstanceId() method,
 * which returns the unique identifier for the current instance and node.
 *
 * @author Štěpán Kameník, FG Forrest a.s. (c) 2023
 */
public interface InstanceIdProvider {
    String DEFAULT_INSTANCE_ID = "DEFAULT";
    String INSTANCE_DELIMITER = "_";

    String getInstanceId();
}
