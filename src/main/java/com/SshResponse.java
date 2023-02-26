package com;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.ToString;

@AllArgsConstructor
@ToString
public class SshResponse {

    @Getter
    private final String commandStr;

    @Getter
    private final String stdout;

    @Getter
    private final int errorCode;

    public String stdoutCleaned() {
        return stdout.replace(commandStr, "");
    }


}
