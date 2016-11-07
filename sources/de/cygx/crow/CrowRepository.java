package de.cygx.crow;
import java.io.*;

public class CrowRepository implements Serializable {
    static abstract class Entry {
        final CrowRecord record;

        Entry(CrowRecord record) {
            this.record = record;
        }
    }

    static class MemEntry extends Entry {
        MemEntry() {
            super(null);
        }
    }

    static class FileEntry extends Entry {
        FileEntry(File file) {
            super(null);
        }
    }

    public CrowRepository() {}
}
