package com.huangxueqin.surge.Surge.cache;

import java.io.*;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Created by huangxueqin on 16/11/12.
 */
public class SurgeDiskCache {
    private static final String DIRECTORY = "/Users/huangxueqin/IdeaProjects/Polari/cache";
    private static final String JOURNAL_FILE = "journal";
    private static final String JOURNAL_FILE_TMP = ".journal.swp";

    private static final String CLEAN = "clean";
    private static final String DIRTY = "dirty";
    private static final String REMOVE = "remove";
    private static final String READ = "read";

    private File directory;
    private File journalFile;
    private File journalFileTmp;
    private Writer journalWriter;

    private long maxSize;
    private long size;
    private long redundantCount;

    private LinkedHashMap<String, Entry> lruEntry = new LinkedHashMap<>(0, 0.75f, true);

    private ExecutorService executorService = Executors.newSingleThreadExecutor();

    private Callable<Void> journalCompactor = new Callable<Void>() {
        @Override
        public Void call() throws Exception {
            synchronized (SurgeDiskCache.this) {
                if (journalWriter == null) {
                    return null;
                }
                trimToSizeLocked();
                if (journalRebuildRequiredLocked()) {
                    rebuildJournalLocked();
                }
            }
            return null;
        }
    };

    private SurgeDiskCache(long maxSize) {
        this(new File(DIRECTORY), maxSize);
    }

    private SurgeDiskCache(File directory, long maxSize) {
        this.directory = directory;
        this.journalFile = new File(directory, JOURNAL_FILE);
        this.journalFileTmp = new File(directory, JOURNAL_FILE_TMP);
        this.maxSize = maxSize;
    }

    public static SurgeDiskCache open(File directory, long maxSize) throws IOException {
        SurgeDiskCache cache = new SurgeDiskCache(directory, maxSize);
        if(cache.journalFile.exists()) {
            try {
                cache.readJournal();
                cache.processJournal();
                cache.journalWriter = new BufferedWriter(new FileWriter(cache.journalFile, true));
                return cache;
            } catch (IOException e) {
                e.printStackTrace();
                cache.close();
            }
        }
        directory.mkdirs();
        cache = new SurgeDiskCache(directory, maxSize);
        synchronized (cache) {
            cache.rebuildJournalLocked();
        }
        return cache;
    }

    private void readJournal() throws IOException {
        BufferedReader br = null;
        try {
            br = new BufferedReader(new FileReader(journalFile));
            int lineCount = 0;
            String line = null;
            while ((line = br.readLine()) != null) {
                lineCount++;
                int firstSpace = line.indexOf(' ');
                if (firstSpace == -1) {
                    throw new IOException("unexpected journal line: " + line);
                }
                int secondSpace = line.indexOf(' ', firstSpace+1);
                String key = line.substring(firstSpace+1, secondSpace != -1 ?
                        secondSpace : line.length());
                Entry entry = lruEntry.get(key);
                if (entry == null) {
                    entry = new Entry(key);
                    lruEntry.put(key, entry);
                }
                if (secondSpace != -1 && line.startsWith(CLEAN+" ")) {
                    entry.readable = true;
                    entry.length = Long.valueOf(line.substring(secondSpace+1, line.length()));
                } else if (secondSpace == -1 && line.startsWith(DIRTY + " ")) {
                    entry.readable = false;
                } else if (secondSpace == -1 && line.startsWith(REMOVE + " ")) {
                    lruEntry.remove(key);
                } else if (secondSpace == -1 && line.startsWith(READ + " ")) {
                    // already done by lruCache.get(key) above
                } else {
                    throw new IOException("unexpected journal line: " + line);
                }
            }
            redundantCount = lineCount - lruEntry.size();
        } finally {
            if (br != null) {
                br.close();
            }
        }
    }

    private void processJournal() {
        for(Iterator<Entry> ie = lruEntry.values().iterator(); ie.hasNext(); ) {
            Entry entry = ie.next();
            if(entry.readable) {
                size += entry.length;
            }
            else {
                entry.getCleanFile().delete();
                entry.getDirtyFile().delete();
                ie.remove();
            }
        }
    }

    private void rebuildJournalLocked() throws IOException {
        if (journalWriter != null) {
            journalWriter.close();
        }
        journalFileTmp.delete();
        Writer writer = new BufferedWriter(new FileWriter(journalFileTmp, true));
        for (Entry entry : lruEntry.values()) {
            if (entry.editor == null) {
                writer.write(CLEAN + " " + entry.key + " " + entry.length + "\n");
            }
            else {
                writer.write(DIRTY + " " + entry.key + "\n");
            }
        }
        writer.close();
        journalFileTmp.renameTo(journalFile);
        journalWriter = new BufferedWriter(new FileWriter(journalFile, true));
        redundantCount = 0;
    }

    private boolean journalRebuildRequiredLocked() {
        return redundantCount >= 2000 &&
                redundantCount >= lruEntry.size();
    }

    public synchronized void close() throws IOException {
        if (journalWriter == null) {
            return;
        }
        journalWriter.close();
        for (Entry entry : lruEntry.values()) {
            if (entry.editor != null) {
                entry.editor.abort();
            }
        }
        trimToSizeLocked();
        journalWriter = null;
    }

    public synchronized void flush() throws IOException {
        if (journalWriter == null) {
            return;
        }
        journalWriter.flush();
    }

    private void trimToSizeLocked() throws IOException {
        while (size > maxSize) {
            // remove oldest entry
            String key = lruEntry.keySet().iterator().next();
            removeCache(key);
        }
    }

    private synchronized void removeCache(String key) throws IOException {
        if (journalWriter == null) {
            throw new IllegalStateException();
        }
        Entry entry = lruEntry.get(key);
        if (entry != null && entry.editor == null) {
            File clean = entry.getCleanFile();
            if (!clean.delete()) {
                throw new IOException("can not delete file: " + entry.key);
            }
            size -= entry.length;
            lruEntry.remove(entry.key);
            journalWriter.append(REMOVE + " " + entry.key + "\n");
            redundantCount++;
            if (journalRebuildRequiredLocked()) {
                executorService.submit(journalCompactor);
            }
        }
    }

    // used for reading cached file
    public synchronized InputStream getInputStream(final String key) throws IOException {
        Entry entry = lruEntry.get(key);
        if (entry == null || !entry.readable) {
            return null;
        }
        InputStream is = null;
        try {
            is = new FileInputStream(entry.getCleanFile());
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }

        if (is != null) {
            redundantCount++;
            journalWriter.write(READ + " " + key + "\n");
            if (journalRebuildRequiredLocked()) {
                executorService.submit(journalCompactor);
            }
        }

        return is;
    }

    // used for caching file into disk
    public synchronized Editor edit(String key) throws IOException {
        Entry entry = lruEntry.get(key);
        if (entry == null) {
            entry = new Entry(key);
            lruEntry.put(key, entry);
        }
        if (entry.editor != null) {
            // another thread is using editor
            return null;
        }
        Editor editor = new Editor(entry);
        entry.editor = editor;

        // write into journal
        journalWriter.write(DIRTY + " " + key + "\n");
        journalWriter.flush();
        return editor;
    }

    public synchronized void commitEdit(Editor editor, boolean success) throws IOException {
        Entry entry = editor.entry;
        if (entry.editor != editor) {
            throw new IllegalStateException();
        }

        File dirty = entry.getDirtyFile();
        if (success) {
            if (dirty.exists()) {
                File clean = entry.getCleanFile();
                dirty.renameTo(clean);
                size += clean.length() - entry.length;
                entry.length = clean.length();
            }
        } else {
            dirty.delete();
        }

        redundantCount++;
        entry.editor = null;
        if (success) {
            entry.readable = true;
            journalWriter.write(CLEAN + " " + entry.key + " " + entry.length + "\n");
        } else {
            journalWriter.write(REMOVE + " " + entry.key + "\n");
            lruEntry.remove(entry.key);
        }
        if (size > maxSize || journalRebuildRequiredLocked()) {
            executorService.submit(journalCompactor);
        }
    }

    public final class Editor {
        private Entry entry;
        private boolean hasError;

        private Editor(Entry entry) {
            this.entry = entry;
        }

        private InputStream newInputStream() throws IOException {
            synchronized (SurgeDiskCache.this) {
                if (entry.editor != this) {
                    throw new IllegalStateException();
                }
                if (!entry.readable) {
                    return null;
                }
                return new FileInputStream(entry.getCleanFile());
            }
        }

        public OutputStream newOutputStream() throws IOException {
            synchronized (SurgeDiskCache.this) {
                if (entry.editor != this) {
                    throw new IllegalStateException();
                }
                return new FaultHiddenOutputStream(entry.getDirtyFile());
            }
        }

        public void abort() throws IOException {
            commitEdit(this, false);
        }

        public void commit() throws IOException {
            if (hasError) {
                commitEdit(this, false);

            } else {
                commitEdit(this, true);
            }
        }

        private class FaultHiddenOutputStream extends FileOutputStream {
            public FaultHiddenOutputStream(File file) throws FileNotFoundException {
                super(file);
            }

            @Override
            public void write(int b) {
                try {
                    super.write(b);
                } catch (IOException e) {
                    hasError = true;
                }
            }

            @Override
            public void write(byte[] b) {
                try {
                    super.write(b);
                } catch (IOException e) {
                    hasError = true;
                }
            }

            @Override
            public void write(byte[] b, int off, int len) {
                try {
                    super.write(b, off, len);
                } catch (IOException e) {
                    hasError = true;
                }
            }

            @Override
            public void close() {
                try {
                    super.close();
                } catch (IOException e) {
                    hasError = true;
                }
            }

            @Override
            public void flush() {
                try {
                    super.flush();
                } catch (IOException e) {
                    hasError = true;
                }
            }
        }
    }

    private final class Entry {
        private final String key;
        private long length;
        private boolean readable;
        private Editor editor;

        private Entry(String key) {
            this.key = key;
        }

        public File getCleanFile() {
            return new File(directory, key);
        }

        public File getDirtyFile() {
            return new File(directory, key + ".");
        }
    }
}

