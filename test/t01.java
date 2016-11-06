import de.cygx.crow.*;
import java.io.*;
import java.net.*;
import static de.cygx.crow.CrowConstants.*;

public class t01 {
    public static void main(String[] args) throws Exception {
        InetAddress loopback = InetAddress.getLoopbackAddress();
        final ServerSocket server = new ServerSocket();
        server.bind(new InetSocketAddress(loopback, 0));
        SocketAddress address = server.getLocalSocketAddress();

        new Thread(new Runnable() {
            public void run() {
                try {
                    Socket socket = server.accept();
                    socket.getOutputStream().write(new byte[] {
                        'c', 'r', 'o', 'w', ACK_PIPE
                    });
                    socket.close();
                }
                catch(IOException err) {
                    err.printStackTrace();
                    System.exit(1);
                }
            }
        }).start();

        assert CrowRequest.pipe(0).sendTo(address, null);
    }
}
