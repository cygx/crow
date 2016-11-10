package de.cygx.crow;
import java.io.*;
import java.net.*;
import java.text.*;
import java.util.*;
import static java.nio.charset.StandardCharsets.UTF_8;

public class Server implements Runnable {
    static final InetAddress LOOPBACK = InetAddress.getLoopbackAddress();
    static final DateFormat DATE_FORMAT =
        new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

    static IOException writeTo(Socket socket, String s) {
        try {
            OutputStream os = socket.getOutputStream();
            os.write(s.getBytes(UTF_8));
            os.flush();
            return null;
        }
        catch(IOException e) { return e; }
    }

    static String gist(Throwable t) {
        return t.getClass().getSimpleName() + ": " + t.getMessage();
    }

    ServerSocket server;
    Thread listener;

    synchronized void println(String s) {
        System.out.println(s);
    }

    void echo(String s) {
        println(String.format("%04d %s ", Thread.currentThread().getId(),
            DATE_FORMAT.format(new Date())) + s);
    }

    void echo(Throwable t) {
        echo(gist(t));
    }

    void echo(String fmt, Object... args) {
        echo(String.format(fmt, args));
    }

    void bind(InetSocketAddress address) throws IOException,
            IllegalStateException {
        if(server != null)
            throw new IllegalStateException("already bound");

        server = new ServerSocket();
        server.bind(address);
        echo("bound to " + server.getLocalSocketAddress());
    }

    void bind(InetAddress address, int port) throws IOException {
        bind(new InetSocketAddress(address, port));
    }

    public void run() {}

    void start() {
        if(server == null)
            throw new IllegalStateException("not bound");

        if(listener != null)
            throw new IllegalStateException("already started");

        (listener = new Thread(this)).start();
    }

    void shutdown() throws IOException, IllegalStateException {
        if(server == null)
            throw new IllegalStateException("not bound");

        server.close();
        echo("server closed");
    }

    void quietShutdown() {
        if(server != null && !server.isClosed()) try {
            server.close();
            echo("server closed");
        }
        catch(IOException e) { echo(e); }
    }

    public void init(int port) throws IOException {
        try(ServerSocket ss = new ServerSocket()) {
            ss.bind(new InetSocketAddress(
                InetAddress.getLoopbackAddress(), port));

            echo("listening on port %d for commands...", ss.getLocalPort());
            while(process(ss));
        }
    }

    void report(Socket socket, String s) {
        echo(s);
        writeTo(socket, s);
    }

    void report(Socket socket, Exception e) {
        echo(e);
        writeTo(socket, gist(e));
    }

    boolean process(ServerSocket ss) {
        Socket socket;
        try { socket = ss.accept(); }
        catch(IOException e) { throw new RuntimeException(e); }

        String cmd = null, args = null;
        try {
            BufferedReader reader = new BufferedReader(
                new InputStreamReader(socket.getInputStream(), UTF_8));

            String[] toks = reader.readLine().split(" ", 2);
            cmd = toks[0];
            if(toks.length == 2)
                args = toks[1];

            socket.shutdownInput();
        }
        catch(IOException e) {
            echo(e);
            return true;
        }

        try {
            switch(cmd) {
                case "bind": try {
                    echo("binding...");
                    String[] toks = args.split(" ", 2);
                    bind(InetAddress.getByName(toks[0]),
                        Integer.parseInt(toks[1]));
                    writeTo(socket, "bound to " +
                        server.getLocalSocketAddress());
                }
                catch(IOException e) { report(socket, e); }
                catch(NullPointerException e) { report(socket, e); }
                catch(ArrayIndexOutOfBoundsException e) { report(socket, e); }
                catch(IllegalStateException e) { report(socket, e); }
                catch(NumberFormatException e) { report(socket, e); }
                finally { return true; }

                case "shutdown": try {
                    echo("shutting down...");
                    shutdown();
                }
                catch(IOException e) { report(socket, e); }
                catch(IllegalStateException e) { report(socket, e); }
                finally { return true; }

                case "quit": try {
                    echo("quitting...");
                    quietShutdown();
                    ss.close();
                }
                catch(IOException e) { report(socket, e); }
                finally { return false; }

                case "abort":
                echo("aborting...");
                throw new RuntimeException("Aborted");

                default:
                report(socket, "unsupported command \"" + cmd + '"');
                return true;
            }
        }
        finally {
            try { socket.close(); }
            catch(IOException e) { echo(e); }
        }
    }
}
