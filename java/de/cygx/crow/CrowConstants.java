package de.cygx.crow;

public interface CrowConstants {
    public static final int MAGIC = 'c' << 24 |
                                    'r' << 16 |
                                    'o' <<  8 |
                                    'w';

    public static final byte NAK = 0;

    public static final byte REQ_PIPE   = 1;
    public static final byte REQ_RECORD = 2;
    public static final byte REQ_BLOB   = 3;
    public static final byte REQ_CHUNK  = 4;

    public static final byte ACK_PIPE   = -REQ_PIPE;
    public static final byte ACK_RECORD = -REQ_RECORD;
    public static final byte ACK_BLOB   = -REQ_BLOB;
    public static final byte ACK_CHUNK  = -REQ_CHUNK;
}
