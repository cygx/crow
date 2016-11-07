import de.cygx.crow.*;
import java.io.*;
import java.net.*;
import java.nio.*;
import java.nio.channels.*;
import java.nio.file.*;
import java.nio.file.attribute.*;
import java.util.*;
import static java.nio.file.StandardOpenOption.*;
import static java.nio.file.attribute.PosixFilePermission .*;

public class crow {
    static class Death extends RuntimeException {
        public Death() { super("Died."); }
        public Death(Throwable cause) { super(cause); }
        public Death(String msg) { super(msg); }

        public String toString() {
            Throwable cause = getCause();
            return cause != null
                ? cause.getClass().getSimpleName() + ": " + cause.getMessage()
                : getMessage();
        }
    }

    static final FileAttribute<Set<PosixFilePermission>>
        DEFAULT_PERMISSIONS = filePermissions("rw-rw-r--");

    static String[] args;
    static boolean shelled;

    static FileAttribute<Set<PosixFilePermission>> filePermissions(String s) {
        return PosixFilePermissions.asFileAttribute(
            PosixFilePermissions.fromString(s));
    }

    static void say(Object o) { System.out.println(o); }
    static void cry(Object o) { System.err.println(o); }

    static void die()            { throw new Death(); }
    static void die(Throwable t) { throw new Death(t); }
    static void die(Object o)    { throw new Death(o.toString()); }

    static void usage(String... msg) {
        String[] lines = new String[msg.length];
        lines[0] = "Usage: " + (shelled ? "" : "crow ") + msg[0];
        for(int i = 1; i < msg.length; ++i)
            lines[i] = "       " + (shelled ? "" : "crow ") + msg[i];

        die(String.join("\n", lines));
    }

    static void usage(boolean check, String... msg) {
        if(!check) usage(msg);
    }

    public static void main(String[] args) {
        crow.args = args;
        try { process(); }
        catch(Death death) {
            cry(death);
            System.exit(1);
        }
    }

    static void process() {
        if(args.length > 0) switch(args[0]) {
            case "repo":
            repo();
            return;

            case "-s":
            case "shell":
            shell();
            return;
        }

        usage("[shell|repo] <...>", "-s");
    }

    static void shell() {
        usage(args.length == 1, "[shell|-s]");

        if(shelled) die(new IllegalStateException("already in shell"));
        shelled = true;

        BufferedReader reader = new BufferedReader(
            new InputStreamReader(System.in));

        String line;
        try {
            System.out.print("> ");
            while((line = reader.readLine()) != null) {
                args = line.trim().split("\\s+");
                try { process(); }
                catch(Death death) { cry(death); }
                System.out.print("\n> ");
            }
        }
        catch(IOException e) { die(e); }
    }

    static void repo() {
        final int CMD = 1;

        String cmd = args[CMD];
        if(args.length > 1) switch(cmd) {
            case "create":
            repo_create();
            return;

            case "list":
            repo_list();
            return;

            case "store":
            repo_store();
            return;

            case "register":
            repo_register();
            return;

            case "prune":
            repo_prune();
            return;

            default:
            if(cmd.startsWith("-")) {
                for(int i = 1; i < cmd.length(); ++i) {
                    char c = cmd.charAt(i);
                    if("clsrp".indexOf(c) < 0)
                        die(new IllegalArgumentException("-" + c));

                    die("TODO");
                }
            }
        }

        usage(
            "repo [create|list|store|register|prune] <...>",
            "repo -{clsrp} <...>"
        );
    }

    static void repo_list() {
        final int REPO = 2;
        usage(args.length == 3, "repo [list|-l] <REPO>");

        CrowRepository repo;
        try(FileChannel channel = FileChannel.open(Paths.get(args[REPO]))) {
             repo = (CrowRepository)new ObjectInputStream(
                Channels.newInputStream(channel)).readObject();
        }
        catch(ClassCastException e) { die(e); }
        catch(ClassNotFoundException e) { die(e); }
        catch(IOException e) { die(e); }
    }

    static void repo_create() {
        final int REPO = 2;
        usage(args.length == 3, "repo create <REPO>");

        try {
            FileChannel channel = null;
            try {
                channel = FileChannel.open(
                    Paths.get(args[REPO]),
                    EnumSet.of(CREATE_NEW, WRITE),
                    DEFAULT_PERMISSIONS
                );
            }
            catch(UnsupportedOperationException e) {
                channel = FileChannel.open(
                    Paths.get(args[REPO]),
                    EnumSet.of(CREATE_NEW, WRITE)
                );
            }

            try(FileLock lock = channel.lock()) {
                ObjectOutputStream os = new ObjectOutputStream(
                    Channels.newOutputStream(channel));

                os.writeObject(new CrowRepository());
                os.flush();
            }
            finally { channel.close(); }
        }
        catch(IOException e) { die(e); }
    }

    static void repo_add(boolean load) {
        final int REPO = 2, NAME = 3, PATH = 4;

        try(
            FileChannel channel = FileChannel.open(Paths.get(args[REPO]),
                EnumSet.of(READ, WRITE));
            FileLock lock = channel.lock();
        ) {
            ObjectInputStream is = new ObjectInputStream(
                Channels.newInputStream(channel));

            CrowRepository repo = (CrowRepository)is.readObject();
            channel.truncate(0);

            repo.add(args[NAME], new File(args[PATH]), load);
            repo.update();

            ObjectOutputStream os = new ObjectOutputStream(
                Channels.newOutputStream(channel));

            os.writeObject(repo);
            os.flush();
        }
        catch(ClassCastException e) { die(e); }
        catch(ClassNotFoundException e) { die(e); }
        catch(IOException e) { die(e); }
    }

    static void repo_store() {
        usage(args.length == 5, "repo store <REPO> <NAME> <PATH>");
        repo_add(true);
    }

    static void repo_register() {
        usage(args.length == 5, "repo register <REPO> <NAME> <PATH>");
        repo_add(false);
    }

    static void repo_prune() {
        final int REPO = 2;
        usage(args.length == 3, "repo prune <REPO>");
        die("TODO");
    }
}
