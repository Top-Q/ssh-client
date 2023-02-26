package com;

import com.jcraft.jsch.ChannelShell;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.Session;
import lombok.Getter;
import lombok.Setter;
import lombok.SneakyThrows;

import java.io.ByteArrayOutputStream;
import java.io.Closeable;
import java.io.IOException;
import java.io.PrintStream;
import java.util.concurrent.TimeUnit;

/**
 * This class is responsible for sending commands to remote machine using SSH connection.
 * It allows sending multiple commands in the same session
 */
public class SshTerminal implements Closeable {

    private final String host;

    private final String username;

    private final String password;

    private final int port;

    private final Session session;

    private final ByteArrayOutputStream outputStream;

    private final ChannelShell channel;

    private final PrintStream stream;

    @Getter
    @Setter
    private String prompt = "$";

    @Setter
    @Getter
    private int retryForPromptInSeconds = 5;

    public SshTerminal(String host, String username, String password) {
        this(host, username, password, 22);
    }

    @SneakyThrows
    public SshTerminal(String host, String username, String password, int port) {
        this.host = host;
        this.username = username;
        this.password = password;
        this.port = port;
        java.util.Properties config = new java.util.Properties();
        config.put("StrictHostKeyChecking", "no");

        JSch jsch = new JSch();
        session = jsch.getSession(username, host, port);
        session.setConfig(config);
        session.setPassword(password);
        session.connect();
        outputStream = new ByteArrayOutputStream();

        channel = (ChannelShell) session.openChannel("shell");
        channel.setOutputStream(outputStream);
        stream = new PrintStream(channel.getOutputStream());
        channel.connect();
    }

    /**
     * Run single command on the same seesion
     * @param commandStr Command to execute
     * @return stdout
     */
    @SneakyThrows
    public String runCommand(String commandStr) {
        stream.println(commandStr);
        stream.flush();
        return waitForPrompt();
    }

    /**
     * Wait for the specified prompt to appear
     * @return command stdout
     */
    @SneakyThrows
    private String waitForPrompt() {
        for (int x = 1; x < retryForPromptInSeconds; x++) {
            TimeUnit.SECONDS.sleep(1);
            if (outputStream.toString().indexOf(prompt) > 0) {
                String responseString = outputStream.toString();
                outputStream.reset();
                return responseString;
            }
        }
        throw new RuntimeException("Prompts failed to appear after " + retryForPromptInSeconds +" seconds");
    }

    /**
     * Closing the session
     * @throws IOException Never happens.
     */
    @Override
    public void close() throws IOException {
        if (channel != null) {
            channel.disconnect();
        }
        if (session != null) {
            session.disconnect();
        }
    }
}
