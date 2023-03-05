import com.SshResponse;
import com.SshTerminal;
import lombok.SneakyThrows;
import org.testng.Assert;
import org.testng.annotations.Test;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class TestScp extends AbstractTestCase{


    @Test
    @SneakyThrows
    public void testGetFile() {
        try (SshTerminal terminal = SshTerminal.newTerminalUsingCreds(cfg.host(), cfg.username(), cfg.password())) {
            terminal.runCommand("echo 'Great Success' > myfile.txt");
            terminal.scpGetFile("myfile.txt","myfile.txt");
            terminal.runCommand("rm myfile.txt");
        }
        final Path path = Paths.get("myfile.txt");
        Assert.assertTrue(Files.exists( Paths.get("myfile.txt")));
        Files.delete(path);
    }

    @Test
    @SneakyThrows
    public void testPutFile() {
        final Path path = Paths.get("myfile.txt");
        Files.createFile(path);
        SshResponse response;
        try (SshTerminal terminal = SshTerminal.newTerminalUsingCreds(cfg.host(), cfg.username(), cfg.password())) {
            terminal.runCommand("rm myfile.txt");
            terminal.scpPutFile("myfile.txt","myfile.txt");
            response = terminal.runCommand("ls -la | grep myfile.txt");
        }
        Files.delete(path);
        System.out.println(response.stdoutCleaned());
        Assert.assertTrue(response.stdoutCleaned().contains("myfile.txt"));
    }

}
