import de.cygx.crow.*;
import java.util.*;

class crow {
    static class Cmd {
        String[] args;
        Runnable action;

        Cmd(String[] args, Runnable action) {
            this.args = args;
            this.action = action;
        }
    }

    static Tree<String, Cmd> cmdTree = new Tree<String, Cmd>();
    static Map<String, String> argMap = new HashMap<String, String>();

    static {
        register(strings("repo", "create"), strings("REPO"),
            () -> createRepo(stringArg("REPO")));
        register(strings("repo", "prune"), strings("REPO"),
            () -> pruneRepo(stringArg("REPO")));
    }

    static String[] strings(String... s) { return s; }
    static String stringArg(String name) { return argMap.get(name); }
    static int intArg(String name) { return Integer.parseInt(argMap.get(name)); }

    static void register(String[] path, String[] args, Runnable action) {
        cmdTree.put(new Cmd(args, action), path);
    }

    public static void main(String[] args) {
        Tree.Path<String> path = cmdTree.path(args);
        Tree.Node<String, Cmd> node = cmdTree.consume(path);
        if(node == null) throw new RuntimeException("not found");
        Cmd cmd = node.value;
        if(cmd.args.length != path.length())
            throw new RuntimeException("wrong arg count");

        int i = 0;
        for(String name : cmd.args)
            argMap.put(name, path.key(i++));

        cmd.action.run();
    }

    static void createRepo(String name) {
        System.out.println("create repo " + name);
    }

    static void pruneRepo(String name) {
        System.out.println("prune repo " + name);
    }
}
