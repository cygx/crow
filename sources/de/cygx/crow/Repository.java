package de.cygx.crow;
import java.io.*;
import java.util.*;

public class Repository implements Serializable {
    static abstract class Entry {
        final String name;
        final Record record;

        Entry(String name, Record record) {
            this.name = name;
            this.record = record;
        }

        abstract boolean exists();
        abstract Entry clone(int id);
    }

    static class MemEntry extends Entry {
        final byte[] bytes;

        MemEntry(String name, Record record, byte[] bytes) {
            super(name, record);
            this.bytes = bytes;
        }

        boolean exists() {
            return true;
        }

        Entry clone(int id) {
            return new MemEntry(name, record.clone(id), bytes);
        }
    }

    static class FileEntry extends Entry {
        final File file;

        FileEntry(String name, Record record, File file) {
            super(name, record);
            this.file = file;
        }

        boolean exists() {
            return file.isFile();
        }

        Entry clone(int id) {
            return new FileEntry(name, record.clone(id), file);
        }
    }

    static byte[] slurp(String path) throws IOException {
        try(RandomAccessFile raf = new RandomAccessFile(path, "r")) {
            long len = raf.length();
            if(len > Integer.MAX_VALUE)
                throw new IOException("too large");

            byte[] bytes = new byte[(int)len];
            raf.readFully(bytes);
            return bytes;
        }
    }

    private List<Entry> entries = new ArrayList<Entry>();
    private Map<String, Entry> registry = new HashMap<String, Entry>();
    private List<Entry> stage = new ArrayList<Entry>();

    public Repository() {}

    public void add(String name, File file, boolean load) {
        Entry entry;

        if(load) {
            entry = null;
        }
        else {
            entry = null;
        }

        stage.add(entry);
    }

    private synchronized Entry get(String name) {
        return registry.get(name);
    }

    private void put(Entry entry) {
        Entry clone = entry.clone(entries.size());
        entries.add(clone);
        registry.put(clone.name, clone);
    }

    public synchronized void update() {
        for(Entry entry : stage) put(entry);
        stage.clear();
    }

    public synchronized void rebuild(boolean prune) {
        update();
        entries.clear();

        if(!prune) for(Entry entry : registry.values()) put(entry);
        else {
            Iterator<Entry> it = registry.values().iterator();
            while(it.hasNext()) {
                Entry entry = it.next();
                if(entry.exists()) put(entry);
                else it.remove();
            }
        }
    }
}
