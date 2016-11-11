package de.cygx.crow;
import java.io.*;

public interface Varint {
    static void writeVarint(DataOutputStream os, long value) throws IOException {
        if(value < 0) throw new IllegalArgumentException("negative value");
        else if(value < 1 << 15) os.writeShort((short)value);
        else if(value < 1 << 30) os.writeInt((int)value | 2 << 30);
        else if(value < 1 << 62) os.writeLong(value | 3 << 62);
        else throw new IllegalArgumentException("value greater than 2**62-1");
    }

    static long readVarint(DataInputStream is) throws IOException {
        int head = is.readUnsignedByte();
        if((head & 1 << 7) == 0) {
            return (long)head << 8
                 | (long)is.readUnsignedByte();
        }

        if((head & 1 << 6) == 0) {
            byte[] bytes = new byte[3];
            is.readFully(bytes);
            return ((long)head     & 0x3F) << 24
                 | ((long)bytes[0] & 0xFF) << 16
                 | ((long)bytes[1] & 0xFF) <<  8
                 | ((long)bytes[2] & 0xFF);
        }
        else {
            byte[] bytes = new byte[7];
            is.readFully(bytes);
            return ((long)head     & 0x3F) << 56
                 | ((long)bytes[0] & 0xFF) << 48
                 | ((long)bytes[1] & 0xFF) << 40
                 | ((long)bytes[2] & 0xFF) << 32
                 | ((long)bytes[3] & 0xFF) << 24
                 | ((long)bytes[4] & 0xFF) << 16
                 | ((long)bytes[5] & 0xFF) <<  8
                 | ((long)bytes[6] & 0xFF);
        }
    }
}
