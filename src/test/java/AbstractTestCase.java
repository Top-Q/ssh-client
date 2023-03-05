import org.aeonbits.owner.ConfigFactory;

public abstract class AbstractTestCase {

    protected TestConfig cfg = ConfigFactory.create(TestConfig.class);

}
