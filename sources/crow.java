import de.cygx.crow.*;
import java.io.*;
import java.net.*;
import java.util.*;
import static java.nio.charset.StandardCharsets.UTF_8;

class crow {
    static final InetAddress LOOPBACK = InetAddress.getLoopbackAddress();
    static boolean shelled;

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
        register(strings("shell"), strings(),
            () -> shell());

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

        register(strings("repo", "prune"), strings("REPO"),
            () -> pruneRepo(stringArg("REPO")));

        register(strings("repo", "create"), strings("REPO"),
            () -> createRepo(stringArg("REPO")));
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

    public static void main(String[] args) {
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

    static void shell() {
        if(shelled) die(new IllegalStateException("already in shell"));
        else shelled = true;

        BufferedReader reader = new BufferedReader(
            new InputStreamReader(System.in));

        String line;
        try {
            System.out.println(
                "Note: Press <ENTER> to see the list of commands\n" +
                "      Append '&' to start commands concurrently"
            );

            System.out.print("> ");
            while((line = reader.readLine()) != null) {
                line = line.trim();
                if(!line.endsWith("&")) main(line.split("\\s+"));
                else {
                    int pos = line.length() - 1;
                    final String[] args = line.substring(0, pos).split("\\s+");
                    new Thread(() -> {
                        long id = Thread.currentThread().getId();
                        System.out.println("Thread " + id + ": started");
                        main(args);
                        System.out.println("Thread " + id + ": done");
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
        System.out.println("create repo " + name);
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
