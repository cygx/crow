import java.io.*;
import java.util.zip.*;

public class RequestFrameBuilder {
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

    static void writeBigEndian(int value, byte[] buffer, int pos) {
        buffer[pos + 0] = (byte)(value >>> 24);
        buffer[pos + 1] = (byte)(value >>> 16);
        buffer[pos + 2] = (byte)(value >>>  8);
        buffer[pos + 3] = (byte)(value);
    }

    public static Deflater deflater(int level) {
        return new Deflater(level, true);
    }

    public static Deflater deflater() {
        return new Deflater(Deflater.DEFAULT_COMPRESSION, true);
    }

    Deflater deflater;
    RequestType type;
    int size;
    boolean keepAlive;
    boolean deflate;
    boolean varint;
    String algorithms;
    int[] ids = new int[255];
    String[] names = new String[255];
    Chunk[] chunks = new Chunk[255];

    public RequestFrameBuilder(Deflater deflater) {
        this.deflater = deflater;
    }

    public RequestFrameBuilder requestRecords(String... names) {
        type = RequestType.RECORD;
        keepAlive = true;
        size = names.length;
        for(int i = 0; i < size; ++i)
            this.names[i] = names[i];

        return this;
    }

    public RequestFrameBuilder requestBlobs(int... ids) {
        type = RequestType.BLOB;
        keepAlive = true;
        size = ids.length;
        for(int i = 0; i < size; ++i)
            this.ids[i] = ids[i];

        return this;
    }

    public RequestFrameBuilder requestChunks() {
        type = RequestType.CHUNK;
        keepAlive = true;
        size = 0;
        return this;
    }

    public RequestFrameBuilder requestDigests(int... ids) {
        type = RequestType.DIGEST;
        keepAlive = true;
        size = ids.length;
        for(int i = 0; i < size; ++i)
            this.ids[i] = ids[i];

        return this;
    }

    public RequestFrameBuilder add(String name) {
        names[size++] = name;
        return this;
    }

    public RequestFrameBuilder add(int id) {
        ids[size++] = id;
        return this;
    }

    public RequestFrameBuilder add(int id, long offset, long length) {
        chunks[size++] = new Chunk(id, offset, length);
        return this;
    }

    public RequestFrameBuilder done() {
        keepAlive = false;
        return this;
    }

    public RequestFrameBuilder deflate() {
        deflate = true;
        return this;
    }

    public RequestFrameBuilder inflate() {
        deflate = false;
        return this;
    }

    public RequestFrameBuilder varint() {
        varint = true;
        return this;
    }

    public RequestFrameBuilder fixint() {
        varint = false;
        return this;
    }

    public RequestFrameBuilder algorithms(String... names) {
        algorithms = String.join(",", names);
        return this;
    }

    public RequestFrameBuilder algorithms() {
        algorithms = null;
        return this;
    }

    public void downsize(int size) {
        if(size < this.size)
            this.size = size;
    }

    public int headSize() {
        switch(type) {
            case RECORD:
            return size * 2 + 4 + 2;

            case BLOB:
            return size * 4 + 2;

            case CHUNK:
            return size * 4 + 2 + 2;

            case DIGEST:
            return size * 4 + 2 + 2;

            default:
            throw new IllegalStateException();
        }
    }

    public int encodeTo(byte[] buffer) {
        return encodeTo(buffer, 0, buffer.length);
    }

    public int encodeTo(byte[] buffer, int offset, int length) {
        switch(type) {
            case RECORD:
            return encodeRecordReqTo(buffer, offset, length);

            case BLOB:
            return encodeBlobReqTo(buffer, offset, length);

            case CHUNK:
            return encodeChunkReqTo(buffer, offset, length);

            case DIGEST:
            return encodeDigestReqTo(buffer, offset, length);

            default:
            throw new IllegalStateException();
        }
    }

    byte firstByte() {
        return (byte)(type.value
            | (keepAlive ? 0x80 : 0)
            | (deflate   ? 0x40 : 0)
            | (varint    ? 0x20 : 0));
    }

    int encodeRecordReqTo(byte[] buffer, int offset, int length) {}

    int encodeBlobReqTo(byte[] buffer, int offset, int length) {
        int size = headSize();
        if(length < size)
            return length - size;

        buffer[offset++] = firstByte();
        buffer[offset++] = (byte)size;
        for(int id : ids) {
            writeBigEndian(id, buffer, offset);
            offset += 4;
        }

        return size;
    }

    int encodeChunkReqTo(byte[] buffer, int offset, int length) {}

    int encodeDigestReqTo(byte[] buffer, int offset, int length) {}
}
