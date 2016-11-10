package de.cygx.crow;
import java.io.*;
import java.net.*;
import java.text.*;
import java.util.*;
import static java.nio.charset.StandardCharsets.UTF_8;

public class Server {
    static final DateFormat DATE_FORMAT =
        new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

    ServerSocket server;

    synchronized void println(String s) {
        System.out.println(s);
    }

    void echo(String s) {
        println(String.format("%04d %s ", Thread.currentThread().getId(),
            DATE_FORMAT.format(new Date())) + s);
    }

    void echo(Throwable t) {
        echo("%s: %s", t.getClass().getSimpleName(), t.getMessage());
    }

    void echo(String fmt, Object... args) {
        echo(String.format(fmt, args));
    }

    void bind(InetSocketAddress address) throws IOException {
        if(server != null)
            throw new IllegalStateException("already bound");

        server = new ServerSocket();
        server.bind(address);
        echo("bound to " + server.getLocalSocketAddress());
    }

    void bind(InetAddress address, int port) throws IOException {
        bind(new InetSocketAddress(address, port));
    }

    void shutdown() throws IOException {
        if(server == null)
            throw new IllegalStateException("not bound");

        server.close();
        echo("server closed");
    }

    public void init(int port) throws IOException {
        ServerSocket ss = new ServerSocket();
        ss.bind(new InetSocketAddress(
            InetAddress.getLoopbackAddress(), port));

        try {
            echo("listening on port %d for commands", ss.getLocalPort());

            for(;;) {
                Socket socket = ss.accept();
                try {
                    BufferedReader reader = new BufferedReader(
                        new InputStreamReader(socket.getInputStream(), UTF_8));

                    String[] toks = reader.readLine().split(" ", 2);
                    switch(toks[0]) {
                        case "bind": try {
                            echo("binding...");
                            toks = toks[1].split(" ", 2);
                            bind(InetAddress.getByName(toks[0]),
                                Integer.parseInt(toks[1]));
                        }
                        catch(Exception e) { echo(e); }
                        finally { break; }

                        case "shutdown": try {
                            echo("shutting down...");
                            shutdown();
                        }
                        catch(Exception e) { echo(e); }
                        finally { break; }

                        default:
                        echo("unsupported command \"%s\" received", toks[0]);
                        break;
                    }
                }
                finally { socket.close(); }
            }
        }
        finally { ss.close(); }
    }
}
