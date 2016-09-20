package alien4cloud.plugin.local;

import alien4cloud.plugin.model.ManagedPlugin;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

/**
 * This component is used in order to launch the plugin from the ide (basically we run an alien4cloud server with the content of the plugin context being part
 * of the main context rather than imported as a plugin).
 * 
 * This allows to work easily in debug and with grunt serve (for ui part).
 */
@Component
public class LocalManagedPlugin extends ManagedPlugin {
    public LocalManagedPlugin() {
        this("../../alien4cloud-cloudify3-provider/src/main/resources");
    }

    public LocalManagedPlugin(String pluginPath) {
        super(pluginPath);
    }
}
