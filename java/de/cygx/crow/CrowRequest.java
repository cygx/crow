package de.cygx.crow;
import java.io.*;
import java.net.*;
import java.util.zip.CRC32;
import static de.cygx.crow.CrowConstants.*;
import static java.nio.charset.StandardCharsets.UTF_8;

public abstract class CrowRequest {
    private static final int BUFFER_SIZE = 8192;

    static void transfer(InputStream is, OutputStream os, long size)
            throws IOException {
        byte[] buffer = new byte[BUFFER_SIZE];
        long todo = size;

        for(int count; todo >= BUFFER_SIZE; todo -= count) {
            if((count = is.read(buffer)) < 0)
                throw new IOException("premature end of stream");

            os.write(buffer, 0, count);
        }

        for(int count; todo > 0; todo -= count) {
            if((count = is.read(buffer, 0, (int)todo)) < 0)
                throw new IOException("premature end of stream");

            os.write(buffer, 0, count);
        }
    }

    static int checkedTransfer(InputStream is, OutputStream os, long size)
            throws IOException {
        byte[] buffer = new byte[BUFFER_SIZE];
        CRC32 crc = new CRC32();
        long todo = size;

        for(int count; todo >= BUFFER_SIZE; todo -= count) {
            if((count = is.read(buffer)) < 0)
                throw new IOException("premature end of stream");

            os.write(buffer, 0, count);
            crc.update(buffer, 0, count);
        }

        for(int count; todo > 0; todo -= count) {
            if((count = is.read(buffer, 0, (int)todo)) < 0)
                throw new IOException("premature end of stream");

            os.write(buffer, 0, count);
            crc.update(buffer, 0, count);
        }

        return (int)crc.getValue();
    }

    private static class PipeReq extends CrowRequest {
        PipeReq(byte[] msg) { super(msg); }

        boolean invalidAck(byte ack) {
            return ack != ACK_PIPE;
        }

        void transfer(DataInputStream is, OutputStream os) {}
    }

    private static class RecordReq extends CrowRequest {
        RecordReq(byte[] msg) { super(msg); }

        boolean invalidAck(byte ack) {
            return ack != ACK_RECORD;
        }

        void transfer(DataInputStream is, OutputStream os)
                throws IOException {
            transfer(is, os, CrowRecord.SIZE);
        }
    }

    private static class BlobReq extends CrowRequest {
        private final int checksum;
        private final long size;

        BlobReq(byte[] msg, int checksum, long size) {
            super(msg);
            this.checksum = checksum;
            this.size = size;
        }

        boolean invalidAck(byte ack) {
            return ack != ACK_BLOB;
        }

        void transfer(DataInputStream is, OutputStream os) throws IOException {
            if(checkedTransfer(is, os, size) != checksum)
                throw new IOException("invalid checksum");
        }
    }

    private static class ChunkReq extends CrowRequest {
        private final long length;

        ChunkReq(byte[] msg, long length) {
            super(msg);
            this.length = length;
        }

        boolean invalidAck(byte ack) {
            return ack != ACK_CHUNK;
        }

        void transfer(DataInputStream is, OutputStream os) throws IOException {
            int checksum = checkedTransfer(is, os, length);
            if(checksum != is.readInt())
                throw new IOException("invalid checksum");
        }
    }

    public static CrowRequest pipe(int count) {
        if(count < 0 || count > 0xFF)
            throw new IllegalArgumentException("not in range 0..255");

        return new PipeReq(new byte[] { REQ_PIPE, (byte)count });
    }

    public static CrowRequest record(String name) {
        byte[] bytes = name.getBytes(UTF_8);
        if(bytes.length > 0xFFFF)
            throw new IllegalArgumentException("too long");

        byte[] msg = new byte[bytes.length + 3];
        msg[0] = REQ_RECORD;
        msg[1] = (byte)(bytes.length >> 8);
        msg[2] = (byte)(bytes.length);
        System.arraycopy(bytes, 0, msg, 3, bytes.length);

        return new RecordReq(msg);
    }

    public static CrowRequest blob(int id, int checksum, long size) {
        byte[] msg = {
            REQ_BLOB,
            (byte)(id >> 24), (byte)(id >> 16), (byte)(id >> 8), (byte)id,
        };

        return new BlobReq(msg, checksum, size);
    }

    public static CrowRequest chunk(int id, long offset, long length) {
        byte[] msg = {
            REQ_CHUNK,
            (byte)(id >> 24), (byte)(id >> 16), (byte)(id >> 8), (byte)id,
            (byte)(offset >> 56), (byte)(offset >> 48),
            (byte)(offset >> 40), (byte)(offset >> 32),
            (byte)(offset >> 24), (byte)(offset >> 16),
            (byte)(offset >>  8), (byte)(offset),
            (byte)(length >> 56), (byte)(length >> 48),
            (byte)(length >> 40), (byte)(length >> 32),
            (byte)(length >> 24), (byte)(length >> 16),
            (byte)(length >>  8), (byte)(length),
        };

        return new ChunkReq(msg, length);
    }

    private final byte[] msg;

    abstract boolean invalidAck(byte ack);
    abstract void transfer(DataInputStream is, OutputStream dest)
        throws IOException;

    CrowRequest(byte[] msg) {
        this.msg = msg;
    }

    public boolean receive(DataInputStream is, OutputStream os)
            throws IOException {
        byte ack = is.readByte();
        if(ack == NAK) return false;
        if(invalidAck(ack))
            throw new IOException("invalid ack");

        transfer(is, os);
        return true;
    }

    public void sendTo(CrowPipe pipe) throws IOException {
        pipe.out().write(msg);
    }

    public boolean sendTo(InetSocketAddress address, OutputStream dest)
            throws IOException {
        try(Socket socket = new Socket()) {
            socket.connect(address);

            DataOutputStream os = new DataOutputStream(socket.getOutputStream());
            os.writeInt(MAGIC);
            os.write(msg);
            os.close();

            DataInputStream is = new DataInputStream(socket.getInputStream());
            if(is.readInt() != MAGIC)
                throw new IOException("not a crow stream");

            return receive(is, dest);
        }
    }

    public byte[] sendTo(InetSocketAddress address) throws IOException {
        ByteArrayOutputStream bs = new ByteArrayOutputStream();
        return sendTo(address, bs) ? bs.toByteArray() : null;
    }
}
