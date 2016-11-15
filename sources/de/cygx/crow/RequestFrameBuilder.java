package de.cygx.crow;
import java.io.*;
import java.util.zip.*;
import static java.nio.charset.StandardCharsets.UTF_8;

public class RequestFrameBuilder implements Closeable {
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

    static int putShort(short value, byte[] buffer, int pos) {
        buffer[pos + 0] = (byte)(value >>> 8);
        buffer[pos + 1] = (byte)(value);
        return 2;
    }

    static int putInt(int value, byte[] buffer, int pos) {
        buffer[pos + 0] = (byte)(value >>> 24);
        buffer[pos + 1] = (byte)(value >>> 16);
        buffer[pos + 2] = (byte)(value >>>  8);
        buffer[pos + 3] = (byte)(value);
        return 4;
    }

    static int putLong(long value, byte[] buffer, int pos) {
        buffer[pos + 0] = (byte)(value >>> 56);
        buffer[pos + 1] = (byte)(value >>> 48);
        buffer[pos + 2] = (byte)(value >>> 40);
        buffer[pos + 3] = (byte)(value >>> 32);
        buffer[pos + 4] = (byte)(value >>> 24);
        buffer[pos + 5] = (byte)(value >>> 16);
        buffer[pos + 6] = (byte)(value >>>  8);
        buffer[pos + 7] = (byte)(value);
        return 8;
    }

    static int varintSize(long value) {
        if(value < 0) throw new IllegalArgumentException("negative value");
        if(value < 1 << 15) return 2;
        if(value < 1 << 30) return 4;
        if(value < 1 << 62) return 8;
        throw new IllegalArgumentException("value greater than 2**62-1");
    }

    static int putVarint(long value, byte[] buffer, int pos) {
        if(value < 0) throw new IllegalArgumentException("negative value");
        if(value < 1 << 15) return putShort((short)value, buffer, pos);
        if(value < 1 << 30) return putInt((int)value | 2 << 30, buffer, pos);
        if(value < 1 << 62) return putLong(value | 3 << 62, buffer, pos);
        throw new IllegalArgumentException("value greater than 2**62-1");
    }

    Deflater deflater;
    RequestType type;
    int count;
    boolean keepAlive;
    boolean deflate;
    boolean varint;
    String algorithms;
    int[] ids = new int[255];
    String[] names = new String[255];
    Chunk[] chunks = new Chunk[255];

    public RequestFrameBuilder() {
        this.deflater = new Deflater(Deflater.DEFAULT_COMPRESSION, true);
    }

    public RequestFrameBuilder(int level) {
        this.deflater = new Deflater(level, true);
    }

    public RequestFrameBuilder(Deflater deflater) {
        this.deflater = deflater;
    }

    public void close() {
        if(deflater != null) {
            deflater.end();
            deflater = null;
        }
    }

    public RequestFrameBuilder requestRecords(String... names) {
        type = RequestType.RECORD;
        keepAlive = true;
        count = names.length;
        for(int i = 0; i < count; ++i)
            this.names[i] = names[i];

        return this;
    }

    public RequestFrameBuilder requestBlobs(int... ids) {
        type = RequestType.BLOB;
        keepAlive = true;
        count = ids.length;
        for(int i = 0; i < count; ++i)
            this.ids[i] = ids[i];

        return this;
    }

    public RequestFrameBuilder requestChunks() {
        type = RequestType.CHUNK;
        keepAlive = true;
        count = 0;
        return this;
    }

    public RequestFrameBuilder requestDigests(int... ids) {
        type = RequestType.DIGEST;
        keepAlive = true;
        count = ids.length;
        for(int i = 0; i < count; ++i)
            this.ids[i] = ids[i];

        return this;
    }

    public RequestFrameBuilder add(String name) {
        names[count++] = name;
        return this;
    }

    public RequestFrameBuilder add(int id) {
        ids[count++] = id;
        return this;
    }

    public RequestFrameBuilder add(int id, long offset, long length) {
        chunks[count++] = new Chunk(id, offset, length);
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
        if(size < count)
            count = size;
    }

    public int headSize() {
        switch(type) {
            case RECORD : return 2 + count * 2 + 4;
            case BLOB   : return 2 + count * 4 + 0;
            case CHUNK  : return 2 + count * 4 + 2;
            case DIGEST : return 2 + count * 4 + 2;
            default     : throw new IllegalStateException();
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

    int encodeRecordReqTo(byte[] buffer, int offset, int length) {
        throw new RuntimeException("TODO");
    }

    int encodeBlobReqTo(byte[] buffer, int offset, int length) {
        int size = headSize();
        if(length < size)
            return length - size;

        buffer[offset++] = firstByte();
        buffer[offset++] = (byte)size;
        for(int id : ids)
            offset += putInt(id, buffer, offset);

        return size;
    }

    int varintChunkBodySize() {
        int size = 0;
        for(int i = 0; i < count; ++i) {
            size += varintSize(chunks[i].offset);
            size += varintSize(chunks[i].length);
        }

        return size;
    }

    int encodeChunkReqTo(byte[] buffer, int offset, int length) {
        int size = headSize();
        byte[] body;

        if(!varint) {
            body = new byte[count * 16];
            int pos = 0;
            for(int i = 0; i < count; ++i) {
                pos += putLong(chunks[i].offset, body, pos);
                pos += putLong(chunks[i].length, body, pos);
            }
        }
        else {
            body = new byte[varintChunkBodySize()];
            int pos = 0;
            for(int i = 0; i < count; ++i) {
                pos += putVarint(chunks[i].offset, body, pos);
                pos += putVarint(chunks[i].length, body, pos);
            }
        }

        if(!deflate) {
            size += body.length;
            if(length < size)
                return length - size;

            buffer[offset++] = firstByte();
            buffer[offset++] = (byte)size;
            for(int id : ids)
                offset += putInt(id, buffer, offset);

            offset += putShort((short)body.length, buffer, offset);
            System.arraycopy(body, 0, buffer, offset, body.length);

            return size;
        }
        else {
            if(deflater == null)
                throw new IllegalStateException("deflater missing or closed");

            if(length < size)
                return 0;

            int bufferSize = offset + length;

            buffer[offset++] = firstByte();
            buffer[offset++] = (byte)size;
            for(int id : ids)
                offset += putInt(id, buffer, offset);

            int sizeOffset = offset;
            offset += 2;

            deflater.reset();
            deflater.setInput(body);
            deflater.deflate(buffer, offset, bufferSize - offset,
                Deflater.SYNC_FLUSH);
            if(!deflater.needsInput())
                return 0;

            long written = deflater.getBytesWritten();
            if(written >= 0x10000)
                throw new IllegalArgumentException(
                    "compressed chunks exceed size limit 65536");

            putShort((short)written, buffer, sizeOffset);
            size += written;

            return size;
        }
    }

    int encodeDigestReqTo(byte[] buffer, int offset, int length) {
        if(algorithms == null) {
            int size = headSize();
            if(length < size)
                return length - size;

            buffer[offset++] = firstByte();
            buffer[offset++] = (byte)size;
            for(int id : ids)
                offset += putInt(id, buffer, offset);

            putShort((short)0, buffer, offset);

            return size;
        }
        else if(!deflate) {
            byte[] body = algorithms.getBytes(UTF_8);
            if(body.length >= 0x10000) {
                throw new IllegalArgumentException(
                    "algorithm names exceed size limit 65536");
            }

            int size = headSize() + body.length;
            if(length < size)
                return length - size;

            buffer[offset++] = firstByte();
            buffer[offset++] = (byte)size;
            for(int id : ids)
                offset += putInt(id, buffer, offset);

            offset += putShort((short)body.length, buffer, offset);
            System.arraycopy(body, 0, buffer, offset, body.length);

            return size;
        }
        else {
            if(deflater == null)
                throw new IllegalStateException("deflater missing or closed");

            int bufferSize = offset + length;
            int size = headSize();
            if(length < size)
                return 0;

            buffer[offset++] = firstByte();
            buffer[offset++] = (byte)size;
            for(int id : ids)
                offset += putInt(id, buffer, offset);

            int sizeOffset = offset;
            offset += 2;

            deflater.reset();
            deflater.setInput(algorithms.getBytes(UTF_8));
            deflater.deflate(buffer, offset, bufferSize - offset,
                Deflater.SYNC_FLUSH);
            if(!deflater.needsInput())
                return 0;

            long written = deflater.getBytesWritten();
            if(written >= 0x10000)
                throw new IllegalArgumentException(
                    "compressed algorithm names exceed size limit 65536");

            putShort((short)written, buffer, sizeOffset);
            size += written;

            return size;
        }
    }
}
