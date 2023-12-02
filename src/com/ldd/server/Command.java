package com.ldd.server;

import java.util.stream.Stream;

/**
 * @author wang.qingsong
 * Create on 2023/12/2
 */
public enum Command {

    LS("ls"), BYE("bye"), HELP("help"), CD("cd"), GET("get");

    private String value;

    Command(String value) {
        this.value = value;
    }

    public static Command of(String command){
        return Stream.of(Command.values())
                .filter(element -> command.startsWith(element.value))
                .findFirst()
                .orElse(null);
    }

    public static boolean isLegal(String msg) {
        return Command.of(msg) != null;
    }
}
