import com.SshCommand;
import com.SshTerminal;
import lombok.SneakyThrows;
import lombok.val;
import lombok.var;
import org.testng.annotations.Test;

public class TestSshConnection extends AbstractTestCase{


    @Test
    @SneakyThrows
    public void testSshTerminalWithPrompts() {
        try (SshTerminal terminal = SshTerminal.newTerminalUsingCreds(cfg.host(), cfg.username(), cfg.password())) {
            var response = terminal.runCommand(new SshCommand("rm delme.txt"));
            System.out.println(response);
            response = terminal.runCommand(new SshCommand("touch delme.txt"));
            System.out.println(response);
            response = terminal.runCommand(new SshCommand("sudo chown root delme.txt", ":"));
            System.out.println(response);
            response = terminal.runCommand(new SshCommand("1"));
            System.out.println(response);
            response = terminal.runCommand("ls -la | grep delme");
            System.out.println(response);
        }
    }

    @Test
    @SneakyThrows
    public void testSshTerminalSimpleCommands() {
        try (SshTerminal terminal = SshTerminal.newTerminalUsingCreds(cfg.host(), cfg.username(), cfg.password())) {
            var response = terminal.runCommand(new SshCommand("pwd"));
            System.out.println(response);
            response = terminal.runCommand("cd Downloads");
            System.out.println(response);
            response = terminal.runCommand("pwd");
            System.out.println(response);
            response = terminal.runCommand("ls -la");
            System.out.println(response);
        }
    }

    @Test(expectedExceptions = {java.lang.RuntimeException.class})
    @SneakyThrows
    public void testCommandWithoutPrompt() {
        try (SshTerminal terminal = SshTerminal.newTerminalUsingCreds(cfg.host(), cfg.username(), cfg.password())) {
            val response = terminal.runCommand(new SshCommand("vim",10 ));
            System.out.println(response.getStdout());
            System.out.println("Not suppose to get to here");
        }
    }


}
