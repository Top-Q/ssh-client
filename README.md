# ssh-client
Simple Java API for sending terminal commands via SSH and transferring files using SCP

Q: There is already JCSH and other Java libraries, why do we need another one?

A: This client is using JCSH. This is only a simpler API for the more common operations


## Usage

Sending commands with different prompts:

```Java
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
```

Command with timeout

```Java
try (SshTerminal terminal = SshTerminal.newTerminalUsingCreds(cfg.host(), cfg.username(), cfg.password())) {
    terminal.runCommand(new SshCommand("vim", 10));    
    System.out.println("Timeout exception is thrown. Not suppose to get to here");
}
```

Get file using SCP

```Java
try (SshTerminal terminal = SshTerminal.newTerminalUsingCreds(cfg.host(), cfg.username(), cfg.password())) {
    terminal.runCommand("echo 'Great Success' > myfile.txt");
    terminal.scpGetFile("remoteSource.txt","localDestination.txt");    
}
```

Put file using SFTP

```Java
try (SshTerminal terminal = SshTerminal.newTerminalUsingCreds(cfg.host(), cfg.username(), cfg.password())) {    
    terminal.sftpPutFile("localSource.txt","remoteDestination.txt");
}
```

Login with PEM file

```Java
try (SshTerminal terminal = SshTerminal.newTerminalUsingPem(cfg.host(), cfg.username(), cfg.privateKeyFile())) {
    val response = terminal.runCommand("ls -la");
    System.out.println(response.stdoutCleaned());
}
```

