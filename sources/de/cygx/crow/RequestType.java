package de.cygx.crow;

public enum RequestType {
    RECORD(0x11),
    BLOB(0x12),
    CHUNK(0x13),
    DIGEST(0x14);

    public final int value;

    RequestType(int value) {
        this.value = value;
    }
}
