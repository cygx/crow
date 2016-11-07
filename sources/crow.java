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
    static String[] args;
    static final int BLOCK_SIZE = 8192;
    static final FileAttribute<Set<PosixFilePermission>>
        DEFAULT_PERMISSIONS = filePermissions("rw-rw-r--");

    static FileAttribute<Set<PosixFilePermission>> filePermissions(String s) {
        return PosixFilePermissions.asFileAttribute(
            PosixFilePermissions.fromString(s));
    }

    static void println(PrintStream ps, Exception e) {
        e.printStackTrace(ps);
        //ps.println(e.getClass().getSimpleName() + ": " + e.getMessage());
    }

    static void println(PrintStream ps, Object o) {
        ps.println(o);
    }

    static void say(Exception e) { println(System.out, e); }
    static void say(Object o)    { println(System.out, o); }

    static void cry(Exception e) { println(System.err, e); }
    static void cry(Object o)    { println(System.err, o); }

    static void die()            { System.exit(1); }
    static void die(Exception e) { cry(e); die(); }
    static void die(Object o)    { cry(o); die(); }

    public static void main(String[] args) {
        crow.args = args;

        if(args.length == 0) {
            die("TODO");
        }

        switch(args[0]) {
            case "repo":
            repo();
            return;
        }

        die("TODO");
    }

    static void repo() {
        if(args.length == 1) {
            die(new Exception("TODO"));
        }

        switch(args[1]) {
            case "create":
            repo_create();
            return;

            case "store":
            repo_store();
            return;

            case "register":
            repo_register();
            return;
        }

        die("TODO");
    }

    static void repo_create() {
        if(args.length != 3) die(
            "usage: crow repo create <FILE>"
        );

        try {
            FileChannel channel = null;
            try {
                channel = FileChannel.open(
                    Paths.get(args[2]),
                    EnumSet.of(CREATE_NEW, WRITE),
                    DEFAULT_PERMISSIONS
                );
            }
            catch(UnsupportedOperationException e) {
                channel = FileChannel.open(
                    Paths.get(args[2]),
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

    static void repo_store() {
        if(args.length != 5) die(
            "usage: crow repo store <REPO> <NAME> <PATH>"
        );

        try(
            FileChannel channel = FileChannel.open(Paths.get(args[2]),
                EnumSet.of(READ, WRITE));
            FileLock lock = channel.lock();
        ) {
            ObjectInputStream is = new ObjectInputStream(
                Channels.newInputStream(channel));

            CrowRepository repo = (CrowRepository)is.readObject();
            channel.truncate(0);

            ObjectOutputStream os = new ObjectOutputStream(
                Channels.newOutputStream(channel));

            os.writeObject(repo);
            os.flush();
        }
        catch(ClassCastException e) { die(e); }
        catch(ClassNotFoundException e) { die(e); }
        catch(IOException e) { die(e); }
    }

    static void repo_register() {
        if(args.length != 5) die(
            "usage: crow repo register <REPO> <NAME> <PATH>"
        );
    }
}
