import com.SshTerminal;
import lombok.SneakyThrows;
import lombok.val;
import org.testng.annotations.Test;

public class TestConnectWithPemFile extends AbstractTestCase{

    @Test
    @SneakyThrows
    public void testConnectWithPrivateKey(){
        try (SshTerminal terminal = SshTerminal.newTerminalUsingPem(cfg.host(), cfg.username(), cfg.privateKeyFile())) {
            val response = terminal.runCommand("ls -la");
            System.out.println(response.stdoutCleaned());
        }

    }

}
