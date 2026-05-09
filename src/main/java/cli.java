import cli.Jwt;
import picocli.CommandLine;

@CommandLine.Command(
        subcommands = {
                Jwt.class,
        },
        mixinStandardHelpOptions = true
)
class cli implements Runnable {


    public static void main(String[] args) {
        cli app = new cli();
        int code = new CommandLine(app).execute(args);
        System.exit(code);
    }

    @Override
    public void run() {

    }

}
