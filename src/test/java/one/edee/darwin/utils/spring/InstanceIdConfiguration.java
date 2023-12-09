package one.edee.darwin.utils.spring;

import lombok.Setter;
import one.edee.darwin.locker.InstanceIdProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * A configuration class that provides a bean for instance IDs.
 */
@Configuration
public class InstanceIdConfiguration {

    @Bean
    public InstanceIdProvider instanceIdProvider(){
        return new TestInstanceIdProvider();
    }

    @Setter
    public static class TestInstanceIdProvider implements InstanceIdProvider {

        private String nodeId = "Node_Instance";
        @Override
        public String getInstanceId() {
            return nodeId;
        }
    }
}
