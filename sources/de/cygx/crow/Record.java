package de.cygx.crow;
import java.io.*;
import java.net.*;

public class Record {
    public static final int SIZE = 24;

    public static Record from(byte[] bytes) {
        if(bytes != null && bytes.length == SIZE) try {
            DataInputStream is = new DataInputStream(
                new ByteArrayInputStream(bytes));

            return new Record(
                is.readInt(), is.readInt(), is.readLong(), is.readLong()
            );
        }
        catch(IOException err) {}
        return null;
    }

    public final int id;
    public final int checksum;
    public final long size;
    public final long timestamp;

    public Record(int id, int checksum, long size, long timestamp) {
        this.id = id;
        this.checksum = checksum;
        this.size = size;
        this.timestamp = timestamp;
    }

    public Record clone(int id) {
        return new Record(id, checksum, size, timestamp);
    }

    public void writeTo(DataOutputStream os) throws IOException {
        os.writeInt(id);
        os.writeInt(checksum);
        os.writeLong(size);
        os.writeLong(timestamp);
    }
}
