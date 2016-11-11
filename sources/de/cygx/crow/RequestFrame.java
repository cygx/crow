package de.cygx.crow;
import java.io.*;
import java.nio.*;
import java.util.*;
import java.util.zip.*;
import static de.cygx.crow.Constants.*;
import static de.cygx.crow.Varint.*;
import static java.nio.charset.StandardCharsets.UTF_8;

public class RequestFrame {
    static class RecordRequestBuilder {
        byte[][] names;
        boolean deflate;
        int level;
        int size;

        RecordRequestBuilder(byte[][] names, int size) {
            this.names = names;
            this.size = size;
        }

        public RecordRequestBuilder deflate(int level) {
            this.deflate = true;
            this.level = level;
            return this;
        }

        public RecordRequestBuilder deflate() {
            return deflate(Deflater.DEFAULT_COMPRESSION);
        }

        public RequestFrame create(boolean keepAlive) {
            byte[] head = new byte[names.length * 2 + 6];
            ByteBuffer buf = ByteBuffer.wrap(head);
            buf.order(ByteOrder.BIG_ENDIAN);

            int type = RECORD_REQUEST
                     | (keepAlive ? 0x80 : 0)
                     | (deflate   ? 0x20 : 0);

            buf.put((byte)type);
            buf.put((byte)names.length);

            ByteArrayOutputStream bs = new ByteArrayOutputStream(size);
            try {
                if(deflate) {
                    Deflater def = new Deflater(level, true);
                    DeflaterOutputStream ds = new DeflaterOutputStream(bs, def);
                    for(byte[] name : names) {
                        buf.putShort((short)name.length);
                        ds.write(name);
                    }

                    ds.close();
                    buf.putInt(bs.size());
                }
                else {
                    for(byte[] name : names) {
                        buf.putShort((short)name.length);
                        bs.write(name);
                    }

                    buf.putInt(size);
                }
            }
            catch(IOException e) { throw new RuntimeException(e); }

            assert !buf.hasRemaining();
            return new RequestFrame(head, bs.toByteArray());
        }
    }

    static class BlobRequestBuilder {
        int[] ids;

        BlobRequestBuilder(int[] ids) {
            this.ids = ids;
        }

        public RequestFrame create(boolean keepAlive) {
            byte[] head = new byte[ids.length * 4 + 2];
            ByteBuffer buf = ByteBuffer.wrap(head);
            buf.order(ByteOrder.BIG_ENDIAN);

            int type = BLOB_REQUEST | (keepAlive ? 0x80 : 0);
            buf.put((byte)type);
            buf.put((byte)ids.length);
            for(int id : ids) buf.putInt(id);

            assert !buf.hasRemaining();
            return new RequestFrame(head, null);
        }
    }

    static class ChunkRequestBuilder {
        static class Chunk {
            int id;
            long offset;
            long length;

            Chunk(int id, long offset, long length) {
                this.id = id;
                this.offset = offset;
                this.length = length;
            }
        }

        List<Chunk> chunks = new ArrayList<Chunk>(255);
        boolean varint;

        ChunkRequestBuilder() {}

        public ChunkRequestBuilder add(int id, long offset, long length) {
            if(chunks.size() == 0x100) throw new IllegalStateException(
                "frame size limit 255 exceeded");

            chunks.add(new Chunk(id, offset, length));
            return this;
        }

        public ChunkRequestBuilder varint() {
            this.varint = true;
            return this;
        }

        public RequestFrame create(boolean keepAlive) {
            int size = chunks.size();
            byte[] head = new byte[size * 4 + 4];
            ByteBuffer buf = ByteBuffer.wrap(head);
            buf.order(ByteOrder.BIG_ENDIAN);

            int type = CHUNK_REQUEST
                     | (keepAlive ? 0x80 : 0)
                     | (varint    ? 0x20 : 0);

            buf.put((byte)type);
            buf.put((byte)size);

            ByteArrayOutputStream bs = new ByteArrayOutputStream(size * 16);
            DataOutputStream ds = new DataOutputStream(bs);
            try {
                if(varint) {
                    for(Chunk chunk : chunks) {
                        buf.putInt(chunk.id);
                        writeVarint(ds, chunk.offset);
                        writeVarint(ds, chunk.length);
                    }

                    buf.putInt(bs.size());
                }
                else {
                    for(Chunk chunk : chunks) {
                        buf.putInt(chunk.id);
                        ds.writeLong(chunk.offset);
                        ds.writeLong(chunk.length);
                    }

                    buf.putInt(size * 16);
                }
            }
            catch(IOException e) { throw new RuntimeException(e); }

            assert !buf.hasRemaining();
            return new RequestFrame(head, bs.toByteArray());
        }
    }

    static class DigestRequestBuilder {
        int[] ids;
        byte[] algorithms = new byte[0];
        boolean deflate;
        int level;

        DigestRequestBuilder(int[] ids) {
            this.ids = ids;
        }

        public DigestRequestBuilder deflate(int level) {
            this.deflate = true;
            this.level = level;
            return this;
        }

        public DigestRequestBuilder deflate() {
            return deflate(Deflater.DEFAULT_COMPRESSION);
        }

        public DigestRequestBuilder algorithms(String... names) {
            byte[] bytes = String.join(",", names).getBytes(UTF_8);
            if(bytes.length >= 0x10000) {
                throw new IllegalArgumentException(
                    "name size limit 65535 exceeded");
            }

            algorithms = bytes;
            return this;
        }

        public RequestFrame create(boolean keepAlive) {
            byte[] head = new byte[ids.length * 4 + 4];
            ByteBuffer buf = ByteBuffer.wrap(head);
            buf.order(ByteOrder.BIG_ENDIAN);

            int type = DIGEST_REQUEST
                     | (keepAlive ? 0x80 : 0)
                     | (deflate   ? 0x20 : 0);

            buf.put((byte)type);
            buf.put((byte)ids.length);
            for(int id : ids) buf.putInt(id);

            byte[] body;
            if(deflate) try {
                ByteArrayOutputStream bs =
                    new ByteArrayOutputStream(algorithms.length);

                Deflater def = new Deflater(level, true);
                DeflaterOutputStream ds = new DeflaterOutputStream(bs, def);
                ds.write(algorithms);
                ds.close();

                int bodySize = bs.size();
                if(bodySize >= 0x10000)
                    throw new RuntimeException("deflatd size exceeds limit");

                body = bs.toByteArray();
                buf.putShort((short)bodySize);
            }
            catch(IOException e) { throw new RuntimeException(e); }
            else {
                body = algorithms;
                buf.putShort((short)algorithms.length);
            }

            assert !buf.hasRemaining();
            return new RequestFrame(head, body);
        }
    }

    private static void checkFrameSize(int len) {
        if(len == 0)
            throw new IllegalArgumentException("empty request not supported");

        if(len >= 0x100)
            throw new IllegalArgumentException("frame size limit 255 exceeded");
    }

    public static RecordRequestBuilder requestRecords(String... names) {
        checkFrameSize(names.length);

        byte[][] encodedNames = new byte[names.length][];
        int i = 0, size = 0;
        for(String name : names) {
            int len = (encodedNames[i++] = name.getBytes(UTF_8)).length;
            if(len >= 0x10000) throw new IllegalArgumentException(
                "name size limit 65535 exceeded");

            size += len;
        }

        return new RecordRequestBuilder(encodedNames, size);
    }

    public static BlobRequestBuilder requestBlobs(int... ids) {
        checkFrameSize(ids.length);
        return new BlobRequestBuilder(ids);
    }

    public static ChunkRequestBuilder requestChunks() {
        return new ChunkRequestBuilder();
    }

    public static DigestRequestBuilder requestDigests(int... ids) {
        checkFrameSize(ids.length);
        return new DigestRequestBuilder(ids);
    }

    public final byte[] head;
    public final byte[] body;

    RequestFrame(byte[] head, byte[] body) {
        this.head = head;
        this.body = body;
    }

    public int type() { return head[0] & 0x1F; }
    public int size() { return head[1]; }
    public boolean keepAlive() { return (head[0] & 0x80) != 0; }
    public int representation() { return (head[0] & 0x60) >> 5; }
}
