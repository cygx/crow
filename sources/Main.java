import de.cygx.crow.*;
import java.io.*;
import java.net.*;
import java.nio.*;
import java.nio.channels.*;
import java.nio.file.*;
import java.nio.file.attribute.*;
import java.util.*;
import static java.nio.file.StandardOpenOption.*;
import static java.nio.file.attribute.PosixFilePermission.*;
import static java.nio.charset.StandardCharsets.UTF_8;

class Main {
    static final InetAddress LOOPBACK = InetAddress.getLoopbackAddress();

    static final FileAttribute<Set<PosixFilePermission>>
        DEFAULT_PERMISSIONS = PosixFilePermissions.asFileAttribute(
            PosixFilePermissions.fromString("rw-rw-r--"));

    static boolean shelled;

    static interface Shellable {
        void eval(String... args);
    }

    static class Cmd {
        String[] cmds;
        String[] args;
        Runnable action;

        Cmd(String[] cmds, String[] args, Runnable action) {
            this.cmds = cmds;
            this.args = args;
            this.action = action;
        }

        public String toString() {
            return String.join(" ", cmds) +
                Arrays.stream(args).map(s -> " <" + s + ">")
                    .reduce("", (a, b) -> a + b);
        }
    }

    static Tree<String, Cmd> cmdTree = new Tree<String, Cmd>();
    static Map<String, String> argMap = new HashMap<String, String>();

    static {
        register(strings("server", "abort"), strings("PORT"),
            () -> send(intArg("PORT"), "abort"));

        register(strings("server", "quit"), strings("PORT"),
            () -> send(intArg("PORT"), "quit"));

        register(strings("server", "shutdown"), strings("PORT"),
            () -> send(intArg("PORT"), "shutdown"));

        register(strings("server", "start"), strings("PORT"),
            () -> send(intArg("PORT"), "start"));

        register(strings("server", "bind"), strings("PORT", "HOST:PORT"),
            () -> bindServer(intArg("PORT"), stringArg("HOST:PORT")));

        register(strings("server", "init"), strings("PORT"),
            () -> initServer(intArg("PORT")));

        register(strings("server", "repl"), strings("PORT"),
            () -> shell((args) -> {
                String[] realArgs;
                if(args.length == 0) realArgs = new String[] { "server" };
                else {
                    realArgs = new String[args.length + 2];
                    realArgs[0] = "server";
                    realArgs[1] = args[0];
                    realArgs[2] = argMap.get("PORT");
                    for(int i = 1; i < args.length; ++i)
                        realArgs[i + 2] = args[i];
                }
                main(realArgs);
            }));

        register(strings("repo", "prune"), strings("REPO"),
            () -> pruneRepo(stringArg("REPO")));

        register(strings("repo", "create"), strings("REPO"),
            () -> createRepo(stringArg("REPO")));

        register(strings("repo", "repl"), strings("REPO"),
            () -> shell((args) -> {
                String[] realArgs;
                if(args.length == 0) realArgs = new String[] { "repo" };
                else {
                    realArgs = new String[args.length + 2];
                    realArgs[0] = "repo";
                    realArgs[1] = args[0];
                    realArgs[2] = argMap.get("REPO");
                    for(int i = 1; i < args.length; ++i)
                        realArgs[i + 2] = args[i];
                }
                main(realArgs);
            }));

        register(strings("repl"), strings(),
            () -> shell((args) -> main(args)));
    }

    static void die() { throw new RuntimeException(); }
    static void die(RuntimeException e) { throw e; }
    static void die(Exception e) { throw new RuntimeException(null, e); }
    static void die(Object o) { throw new RuntimeException(o.toString()); }

    static String[] strings(String... s) { return s; }
    static String stringArg(String name) { return argMap.get(name); }
    static int intArg(String name) { return Integer.parseInt(argMap.get(name)); }

    static void register(String[] cmds, String[] args, Runnable action) {
        cmdTree.put(new Cmd(cmds, args, action), cmds);
    }

    static void send(int port, String msg) {
        try { try(Socket socket = new Socket(LOOPBACK, port)) {
            socket.getOutputStream().write((msg + '\n').getBytes(UTF_8));
            socket.shutdownOutput();

            String response = new BufferedReader(new InputStreamReader(
                socket.getInputStream(), UTF_8)).readLine();
            if(response != null)
                System.out.println("Response: " + response);
            socket.shutdownInput();
        } }
        catch(IOException e) { die(e); }
    }

    public static void main(String... args) {
        try {
            Tree.Path<String> path = cmdTree.path(args);
            Tree.Node<String, Cmd> node = cmdTree.consume(path);

            if(node.value == null) {
                List<Cmd> leaves = new LinkedList<>();
                node.aggregateLeafValues(leaves);

                System.out.println("Usage: " + leaves.remove(0));
                for(Cmd cmd : leaves)
                    System.out.println("       " + cmd);

                die();
            }

            Cmd cmd = node.value;
            if(cmd.args.length != path.length()) {
                System.out.println("Usage: " + cmd);
                die();
            }

            int i = 0;
            for(String name : cmd.args)
                argMap.put(name, path.key(i++));

            cmd.action.run();
        }
        catch(RuntimeException e) {
            Throwable cause = e;
            while(cause.getMessage() == null && cause.getCause() != null)
                cause = cause.getCause();

            if(cause.getMessage() != null) {
                System.out.println(cause.getClass().getSimpleName() + ": " +
                    cause.getMessage());
            }

            if(!shelled) System.exit(1);
        }
    }

    static void shell(Shellable shellable) {
        if(shelled) die(new IllegalStateException("already in shell"));
        else shelled = true;

        BufferedReader reader = new BufferedReader(
            new InputStreamReader(System.in));

        try {
            System.out.println(
                "Note: Press <ENTER> to see the list of commands\n" +
                "      Append '&' to start commands concurrently"
            );

            String line;
            System.out.print("> ");
            while((line = reader.readLine()) != null) {
                line = line.trim();
                if(!line.endsWith("&")) shellable.eval(line.split("\\s+"));
                else {
                    int pos = line.length() - 1;
                    final String[] args = line.substring(0, pos).split("\\s+");
                    new Thread(() -> {
                        long id = Thread.currentThread().getId();
                        System.out.println("[" + id + "] started...");
                        shellable.eval(args);
                        System.out.println("[" + id + "] done.");
                    }).start();
                }

                try { Thread.sleep(100); }
                catch(InterruptedException e) {}
                System.out.print("> ");
            }
        }
        catch(IOException e) { die(e); }
    }

    static String[] splitAddress(String address) {
        int pos = address.lastIndexOf(':');
        return pos < 0 ? null : new String[] {
            address.substring(0, pos),
            address.substring(pos + 1)
        };
    }

    static void createRepo(String name) {
        try {
            FileChannel channel = null;
            try {
                channel = FileChannel.open(
                    Paths.get(name),
                    EnumSet.of(CREATE_NEW, WRITE),
                    DEFAULT_PERMISSIONS
                );
            }
            catch(UnsupportedOperationException e) {
                channel = FileChannel.open(
                    Paths.get(name),
                    EnumSet.of(CREATE_NEW, WRITE)
                );
            }

            try {
                new ObjectOutputStream(Channels.newOutputStream(channel))
                    .writeObject(new Repository());
            }
            finally { channel.close(); }
        }
        catch(IOException e) { die(e); }
    }

    static void pruneRepo(String name) {
        System.out.println("prune repo " + name);
    }

    static void initServer(int port) {
        try { new Server().init(port); }
        catch(IOException e) { die(e); }
    }

    static void bindServer(int port, String hostAddress) {
        String[] toks = splitAddress(hostAddress);
        if(toks == null) {
            throw new IllegalArgumentException(
                "no colon in host address \"" + hostAddress + '"'
            );
        }

        InetSocketAddress address = null;
        try {
            address = new InetSocketAddress(
                InetAddress.getByName(toks[0]),
                toks[1].length() > 0 ? Integer.parseInt(toks[1]) : 0
            );
        }
        catch(UnknownHostException e) { die(e); }

        send(port, String.format("bind %s %d",
            address.getAddress().getHostAddress(), address.getPort()));
    }
}
