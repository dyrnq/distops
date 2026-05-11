package com.dyrnq.distops.cli;

import com.dyrnq.distops.cli.Jwt;
import picocli.CommandLine;

@CommandLine.Command(
        subcommands = {
                Jwt.class,
        },
        mixinStandardHelpOptions = true
)
public class CliMain implements Runnable {


    public static void main(String[] args) {
        CliMain app = new CliMain();
        int code = new CommandLine(app).execute(args);
        System.exit(code);
    }

    @Override
    public void run() {

    }

}
