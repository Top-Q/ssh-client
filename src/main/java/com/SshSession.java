package com;

import com.jcraft.jsch.ChannelExec;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.Session;
import lombok.Getter;
import lombok.SneakyThrows;

import java.io.ByteArrayOutputStream;
import java.io.Closeable;
import java.io.IOException;

public class SshSession implements Closeable {

    private final Session session;

    private final ChannelExec channel;

    @Getter
    private final String host;

    @Getter
    private final String username;

    @Getter
    private final String password;

    @Getter
    private final int port;

    @SneakyThrows
    public SshSession(String host, String username, String password, int port) {
        this.host = host;
        this.username = username;
        this.password = password;
        this.port = port;
        session = new JSch().getSession(username, host, port);
        session.setPassword(password);
        session.setConfig("StrictHostKeyChecking", "no");
        session.connect();
        channel = (ChannelExec) session.openChannel("exec");
    }

    public SshSession(String host, String username, String password) {
        this(host, username, password, 22);
    }

    @SneakyThrows
    public String runCommand(String command) {
        channel.setCommand(command);
        ByteArrayOutputStream responseStream = new ByteArrayOutputStream();
        channel.setOutputStream(responseStream);
        channel.connect();
        while (channel.isConnected()) {
            Thread.sleep(100);
        }
        return new String(responseStream.toByteArray());
    }


    @Override
    public void close() throws IOException {
        if (session != null) {
            session.disconnect();
        }
        if (channel != null) {
            channel.disconnect();
        }
    }
}
