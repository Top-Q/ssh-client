import org.aeonbits.owner.Config;

@Config.Sources({"file:config.properties"})
public interface TestConfig extends Config {

    String host();

    String username();

    String password();

    String privateKeyFile();

}
