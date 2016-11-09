package de.cygx.crow;
import java.io.*;

interface Varint {
    static void writeVarint(DataOutputStream os, long value) throws IOException {
        if(value < 1 << 15) os.writeShort((short)value);
        else if(value < 1 << 30) os.writeInt((int)value | 1 << 30);
        else if(value < 1 << 62) os.writeLong(value | 3 << 62);
        else throw new IllegalArgumentException("not in range 0..2**62-1");
    }
}
