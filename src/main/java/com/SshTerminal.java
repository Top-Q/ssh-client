package com;

import com.jcraft.jsch.*;
import lombok.Getter;
import lombok.Setter;
import lombok.SneakyThrows;

import java.io.*;
import java.util.concurrent.TimeUnit;

/**
 * This class is responsible for sending commands to remote machine using SSH connection.
 * It allows sending multiple commands in the same session
 */
public class SshTerminal implements Closeable {

    private static final String DEFAULT_PROMPT = "$";

    private static final int DEFAULT_RETRY_IN_SECONDS = 10;

    private static final int DEFAULT_SFTP_CONNECT_TIMEOUT = 5000;

    private static final int DEFAULT_SSH_PORT = 22;

    private final Session session;

    private final ByteArrayOutputStream outputStream;

    private final ChannelShell channel;

    private final PrintStream stream;

    @Setter
    @Getter
    private int retryForPromptInSeconds = DEFAULT_RETRY_IN_SECONDS;

    public static SshTerminal newTerminalUsingCreds(String host, String username, String password) {
        return new SshTerminal(host, null, username, password, DEFAULT_SSH_PORT);
    }

    public static SshTerminal newTerminalUsingCreds(String host, String username, String password, int port) {
        return new SshTerminal(host, null, username, password, port);
    }


    public static SshTerminal newTerminalUsingPem(String host, String username, String privateKey) {
        return new SshTerminal(host, privateKey, username, null, DEFAULT_SSH_PORT);
    }

    public static SshTerminal newTerminalUsingPem(String host, String username, String privateKey, int port) {
        return new SshTerminal(host, privateKey, username, null, port);
    }


    @SneakyThrows
    private SshTerminal(String host, String privateKey, String username, String password, int port) {
        java.util.Properties config = new java.util.Properties();
        config.put("StrictHostKeyChecking", "no");

        JSch jsch = new JSch();
        if (privateKey != null && !privateKey.isEmpty()) {
            jsch.addIdentity(privateKey);
        }
        session = jsch.getSession(username, host, port);
        session.setConfig(config);
        if (password != null && !password.isEmpty()) {
            session.setPassword(password);
        }
        session.connect();
        outputStream = new ByteArrayOutputStream();

        channel = (ChannelShell) session.openChannel("shell");
        channel.setOutputStream(outputStream);
        stream = new PrintStream(channel.getOutputStream());
        channel.connect();
        // Let's wait a bit and then clean the welcome text
        TimeUnit.SECONDS.sleep(1);
        outputStream.reset();
    }

    /**
     * Run single command on the same seesion
     *
     * @param commandStr Command to execute
     * @return stdout
     */
    @SneakyThrows
    public SshResponse runCommand(String commandStr) {
        return runCommand(new SshCommand(commandStr));
    }

    @SneakyThrows
    public SshResponse runCommand(final SshCommand command) {
        if (null == command.getPrompt()) {
            command.setPrompt(DEFAULT_PROMPT);
        }
        outputStream.reset();
        stream.println(command.getCommandStr());
        stream.flush();
        outputStream.reset();
        int timeout = command.getTimeoutInSeconds() > 0 ? command.getTimeoutInSeconds() : retryForPromptInSeconds;
        for (int x = 1; x < timeout; x++) {
            TimeUnit.SECONDS.sleep(1);
            if (outputStream.toString().indexOf(command.getPrompt()) > 0) {
                String responseString = outputStream.toString();
                outputStream.reset();
                return new SshResponse(command.getCommandStr(), responseString, channel.getExitStatus());
            }
        }
        throw new RuntimeException("Prompts failed to appear after " + retryForPromptInSeconds + " seconds");

    }

    public void scpPutFile(String localSource, String remoteDestination) throws JSchException, IOException {
        boolean ptimestamp = true;

        // exec 'scp -t rfile' remotely
        remoteDestination = remoteDestination.replace("'", "'\"'\"'");
        remoteDestination = "'" + remoteDestination + "'";
        String command = "scp " + (ptimestamp ? "-p" : "") + " -t " + remoteDestination;
        Channel channel = session.openChannel("exec");
        ((ChannelExec) channel).setCommand(command);

        // get I/O streams for remote scp
        OutputStream out = channel.getOutputStream();
        InputStream in = channel.getInputStream();

        channel.connect();

        if (checkAck(in) != 0) {
            return;
        }

        File _lfile = new File(localSource);

        if (ptimestamp) {
            command = "T " + (_lfile.lastModified() / 1000) + " 0";
            // The access time should be sent here,
            // but it is not accessible with JavaAPI ;-<
            command += (" " + (_lfile.lastModified() / 1000) + " 0\n");
            out.write(command.getBytes());
            out.flush();
            if (checkAck(in) != 0) {
                return;
            }
        }

        // send "C0644 filesize filename", where filename should not include '/'
        long filesize = _lfile.length();
        command = "C0644 " + filesize + " ";
        if (localSource.lastIndexOf('/') > 0) {
            command += localSource.substring(localSource.lastIndexOf('/') + 1);
        } else {
            command += localSource;
        }
        command += "\n";
        out.write(command.getBytes());
        out.flush();
        if (checkAck(in) != 0) {
            return;
        }

        // send a content of lfile
        byte[] buf = new byte[1024];
        try (FileInputStream fis = new FileInputStream(localSource)) {

            while (true) {
                int len = fis.read(buf, 0, buf.length);
                if (len <= 0) break;
                out.write(buf, 0, len); //out.flush();
            }

        }
        // send '\0'
        buf[0] = 0;
        out.write(buf, 0, 1);
        out.flush();
        if (checkAck(in) != 0) {
            return;
        }
        out.close();
        channel.disconnect();
    }

    public void scpGetFile(String remoteSource, String localDestination) throws IOException, JSchException {
        String prefix = null;
        if (new File(localDestination).isDirectory()) {
            prefix = localDestination + File.separator;
        }

        // exec 'scp -f rfile' remotely
        remoteSource = remoteSource.replace("'", "'\"'\"'");
        remoteSource = "'" + remoteSource + "'";
        String command = "scp -f " + remoteSource;
        Channel channel = session.openChannel("exec");
        ((ChannelExec) channel).setCommand(command);

        // get I/O streams for remote scp
        OutputStream out = channel.getOutputStream();
        InputStream in = channel.getInputStream();

        channel.connect();

        byte[] buf = new byte[1024];

        // send '\0'
        buf[0] = 0;
        out.write(buf, 0, 1);
        out.flush();
        FileOutputStream fos = null;
        while (true) {
            int c = checkAck(in);
            if (c != 'C') {
                break;
            }

            // read '0644 '
            in.read(buf, 0, 5);

            long filesize = 0L;
            while (true) {
                if (in.read(buf, 0, 1) < 0) {
                    // error
                    break;
                }
                if (buf[0] == ' ') break;
                filesize = filesize * 10L + (long) (buf[0] - '0');
            }

            String file = null;
            for (int i = 0; ; i++) {
                in.read(buf, i, 1);
                if (buf[i] == (byte) 0x0a) {
                    file = new String(buf, 0, i);
                    break;
                }
            }

            // send '\0'
            buf[0] = 0;
            out.write(buf, 0, 1);
            out.flush();

            // read a content of lfile
            fos = new FileOutputStream(prefix == null ? localDestination : prefix + file);
            int foo;
            while (true) {
                if (buf.length < filesize) foo = buf.length;
                else foo = (int) filesize;
                foo = in.read(buf, 0, foo);
                if (foo < 0) {
                    // error
                    break;
                }
                fos.write(buf, 0, foo);
                filesize -= foo;
                if (filesize == 0L) break;
            }
            fos.close();
            if (checkAck(in) != 0) {
                return;
            }

            // send '\0'
            buf[0] = 0;
            out.write(buf, 0, 1);
            out.flush();
        }

    }


    @SneakyThrows
    static int checkAck(InputStream in) {
        int b = in.read();
        // b may be 0 for success,
        //          1 for error,
        //          2 for fatal error,
        //          -1
        if (b == 0) return b;
        if (b == -1) return b;

        if (b == 1 || b == 2) {
            StringBuffer sb = new StringBuffer();
            int c;
            do {
                c = in.read();
                sb.append((char) c);
            }
            while (c != '\n');
            if (b == 1) { // error
                System.out.print(sb.toString());
            }
            if (b == 2) { // fatal error
                System.out.print(sb.toString());
            }
        }
        return b;
    }

    public void sftpGetFile(String remoteSource, String localDestination) throws JSchException, SftpException {
        ChannelSftp channelSftp = (ChannelSftp)session.openChannel("sftp");
        try {
            channelSftp.connect(DEFAULT_SFTP_CONNECT_TIMEOUT);
            channelSftp.get(remoteSource, localDestination);
        } finally{
            channelSftp.exit();
        }
    }

    public void sftpPutFile(String localSource, String remoteDestination) throws JSchException, SftpException {
        ChannelSftp channelSftp = (ChannelSftp)session.openChannel("sftp");
        try {
            channelSftp.connect(DEFAULT_SFTP_CONNECT_TIMEOUT);
            channelSftp.put(localSource, remoteDestination);
        } finally{
            channelSftp.exit();
        }
    }


    /**
     * Closing the session
     *
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
