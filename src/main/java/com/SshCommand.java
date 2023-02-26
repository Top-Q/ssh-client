package com;

import lombok.Getter;
import lombok.Setter;

public class SshCommand {

    @Getter
    private final String commandStr;

    @Getter
    @Setter
    private String prompt;

    public SshCommand(String commandStr) {
        this(commandStr, null);
    }

    public SshCommand(String commandStr, String prompt) {
        this.commandStr = commandStr;
        this.prompt = prompt;
    }

}
