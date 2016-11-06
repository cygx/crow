package de.cygx.crow;
import java.io.*;
import java.net.*;
import java.util.*;

public class CrowPipe implements Closeable, AutoCloseable {
    private Socket socket;
    private OutputStream os;
    private DataInputStream is;
    private int todo;
    private final Queue<CrowRequest> queue = new LinkedList<CrowRequest>();

    public CrowPipe(int count) {
        if(count < 0 || count > 0xFF)
            throw new IllegalArgumentException("not in range 0..255");

        todo = count;
    }

    public void connect(InetSocketAddress address) throws IOException {
        if(socket != null)
            throw new IllegalStateException("already connected");

        socket = new Socket();
        socket.connect(address);
        os = socket.getOutputStream();
        is = new DataInputStream(socket.getInputStream());

        CrowRequest req = CrowRequest.pipe(todo);
        os.write(req.message);
        os.flush();

        if(!req.receive(is, null))
            throw new IOException("connection refused");
    }

    public void send(CrowRequest req) throws IOException {
        if(socket == null)
            throw new IllegalStateException("not connected");

        if(todo == 0)
            throw new IllegalStateException("no remaining requests");

        --todo;
        os.write(req.message);
        os.flush();
        queue.add(req);
        if(todo == 0) socket.shutdownOutput();
    }

    private boolean receiveImpl(CrowRequest req, OutputStream dest)
            throws IOException{
        boolean ok = req.receive(is, dest);
        if(todo == 0 && queue.isEmpty())
            socket.shutdownInput();

        return ok;
    }

    public boolean receive(OutputStream dest) throws IOException {
        if(socket == null)
            throw new IllegalStateException("not connected");

        if(queue.isEmpty())
            throw new IllegalStateException("no pending requests");

        return receiveImpl(queue.remove(), dest);
    }

    public byte[] receive() throws IOException {
        if(socket == null)
            throw new IllegalStateException("not connected");

        if(queue.isEmpty())
            throw new IllegalStateException("no pending requests");

        CrowRequest req = queue.remove();
        ByteArrayOutputStream bs = new ByteArrayOutputStream((int)req.size());
        return receiveImpl(req, bs) ? bs.toByteArray() : null;
    }

    public void close() throws IOException {
        if(socket == null)
            throw new IllegalStateException("not connected");

        socket.close();
    }
}
