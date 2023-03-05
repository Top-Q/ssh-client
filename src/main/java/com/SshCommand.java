package com;

import lombok.Getter;
import lombok.Setter;

public class SshCommand {

    @Getter
    private final String commandStr;

    @Getter
    @Setter
    private String prompt;

    @Getter
    @Setter
    private int timeoutInSeconds;

    public SshCommand(String commandStr) {
        this(commandStr, null, 0);
    }

    public SshCommand(String commandStr, String prompt) {
        this(commandStr, prompt, 0);
    }

    public SshCommand(String commandStr, int timeoutInSeconds) {
        this(commandStr, null, timeoutInSeconds);
    }

    public SshCommand(String commandStr, String prompt, int timeoutInSeconds) {
        this.commandStr = commandStr;
        this.prompt = prompt;
        this.timeoutInSeconds = timeoutInSeconds;
    }

}
