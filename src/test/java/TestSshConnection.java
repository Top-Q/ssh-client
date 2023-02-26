import com.SshTerminal;
import lombok.SneakyThrows;
import lombok.var;
import org.testng.annotations.Test;
public class TestSshConnection {

    private String host = "192.168.68.111";
    private String user = "agmon";
    private String password = "1";

    @Test
    @SneakyThrows
    public void testMySshTerminal() {
        try (SshTerminal terminal = new SshTerminal(host, user, password)) {
            var response = terminal.runCommand("pwd");
            System.out.println(response);
            response = terminal.runCommand("cd Downloads");
            System.out.println(response);
            response = terminal.runCommand("pwd");
            System.out.println(response);
            response = terminal.runCommand("ls -la");
            System.out.println(response);
        }
    }


}
