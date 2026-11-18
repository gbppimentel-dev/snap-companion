package dev.gbppimentel.snapcompanion;

import java.io.*;
import java.nio.*;
import java.security.GeneralSecurityException;
import java.util.*;
import java.util.zip.*;
import javax.crypto.Cipher;
import javax.crypto.spec.*;

/**
 * Read-only Java port of NewSnap's save envelope reader, with bounds and checksum validation.
 * ISC License: Copyright (c) 2021, SciresM, Kaphotics. See assets/licenses/newsnap.txt.
 * Reference: kwsch/NewSnap commit 2da13570960124f263e439994064d00c1a48788d.
 * Payload field meanings are deliberately not inferred from unverified byte offsets.
 */
public final class NewSnapSave {
    public static final long MAX_FILE = 2L * 1024 * 1024 * 1024;
    public static final int MAX_ENTRY = 16 * 1024 * 1024;
    public static final List<String> NAMES = List.of("5652712535912f19", "398ecf2b56c614e3", "6706e7d0f3dab2ab", "00c31e53dde3ac51", "4f342de95be7450e", "0e7280b1c3ce5a0c", "04289da492638279", "9c1c815c54797c18", "bf0b4b223f011bf9", "4b1a0e859a3f7816", "b8b0ce894762e7a8", "76401e93e6565bd5", "0a980178a54e26f3", "4207cfe275441231", "75006a27cf0c9275", "9661edcf41d19929");
    private static final byte[] KEY = {(byte)0x1f,(byte)0xc5,(byte)0xd5,0x71,(byte)0xbd,(byte)0xef,(byte)0xaf,(byte)0x83,(byte)0xfc,(byte)0x96,(byte)0xee,(byte)0xfe,0x70,(byte)0xa1,0x14,(byte)0xec};
    public record Header(int encryptedEntrySize, int payloadSize, int entryCount, int headerSize) {}
    public record Entry(int index, byte[] payload, boolean checksumVerified) {
        public byte[] jpeg() {
            return JpegEnvelope.extract(payload,16);
        }
    }
    public record Report(String filename, int archiveIndex, Header header, int verifiedEntries, int emptyEntries, int jpegEntries, long verifiedPayloadBytes) {}
    public interface EntryConsumer { void accept(Entry entry) throws IOException; }
    private NewSnapSave() {}

    public static Header header(byte[] raw, int archiveIndex) throws IOException {
        checkIndex(archiveIndex);
        if (raw.length < 0x30) throw new IOException("Incomplete save header");
        byte[] decoded = decrypt(Arrays.copyOfRange(raw, 0, 0x20), iv(raw, 0x20, 0xB519FC35 + archiveIndex));
        ByteBuffer b = ByteBuffer.wrap(decoded).order(ByteOrder.LITTLE_ENDIAN);
        if (b.getInt() != 0xC63E569C) throw new IOException("Unsupported save header, filename or archive version");
        int encrypted = b.getInt(), payload = b.getInt(), count = b.getInt(), size = b.getInt();
        long expected = ((long)payload + 15) / 16 * 16 + 0x30;
        if (payload < 1 || payload > MAX_ENTRY || encrypted != expected || count < 0 || count > 8192 || size != 0x30) throw new IOException("Invalid save envelope dimensions");
        return new Header(encrypted, payload, count, size);
    }
    public static Entry entry(byte[] raw, int archiveIndex, int entryIndex, int expectedPayload) throws IOException {
        checkIndex(archiveIndex);
        if (entryIndex < 0 || entryIndex > 8191 || raw.length < 0x30 || raw.length > MAX_ENTRY + 0x30 || raw.length % 16 != 0) throw new IOException("Invalid save entry dimensions");
        boolean empty = true; for (int i = 0; i < 8; i++) if (raw[i] != 0) { empty = false; break; }
        if (empty) return new Entry(entryIndex, new byte[0], false);
        byte[] decoded = decrypt(Arrays.copyOfRange(raw, 16, raw.length), iv(raw, 0, 0xB519FC35 + archiveIndex + entryIndex));
        ByteBuffer b = ByteBuffer.wrap(decoded).order(ByteOrder.LITTLE_ENDIAN);
        long adler = Integer.toUnsignedLong(b.getInt()), crc = Integer.toUnsignedLong(b.getInt());
        if (b.getInt() != 0x9B7A0B7C) throw new IOException("Unsupported save entry or mismatched archive");
        int size = b.getInt(), duplicate = b.getInt();
        int payloadLength = expectedPayload < 0 ? size : expectedPayload;
        if (size != duplicate || size < 0 || size > decoded.length - 0x20 || payloadLength < 0 || payloadLength > decoded.length - 0x20) throw new IOException("Incomplete save payload");
        byte[] payload = Arrays.copyOfRange(decoded, 0x20, 0x20 + payloadLength);
        Adler32 a = new Adler32(); a.update(payload); CRC32 c = new CRC32(); c.update(payload);
        if (a.getValue() != adler || c.getValue() != crc) throw new IOException("Save checksum mismatch; the snapshot may be changing or corrupt");
        return new Entry(entryIndex, payload, true);
    }
    public static Report inspect(File snapshot, String name, EntryConsumer consumer) throws IOException {
        int index = NAMES.indexOf(name.toLowerCase(Locale.ROOT));
        if (index < 0) throw new IOException("Select a recognized New Pokémon Snap save file, or an exported ZIP containing its hashed filenames");
        long size = snapshot.length();
        if (size < 0x30 || size > MAX_FILE) throw new IOException("Save snapshot must be between 48 bytes and 2 GiB");
        try (RandomAccessFile f = new RandomAccessFile(snapshot, "r")) {
            byte[] h = new byte[0x30]; f.readFully(h); Header header = header(h, index);
            long expected = 0x30L + (long)header.encryptedEntrySize() * header.entryCount();
            if (expected != size) throw new IOException("Partial or unsupported save length");
            int verified = 0, empty = 0, jpegs = 0; long payloadBytes = 0;
            byte[] block = new byte[header.encryptedEntrySize()];
            for (int i = 0; i < header.entryCount(); i++) {
                long position=f.getFilePointer();
                if(f.readLong()==0){f.seek(position+header.encryptedEntrySize());empty++;continue;}
                f.seek(position);f.readFully(block); Entry e = entry(block, index, i, header.payloadSize());
                if (e.payload().length == 0) { empty++; continue; }
                verified++; payloadBytes += e.payload().length;
                if (e.jpeg().length > 0) jpegs++;
                if (consumer != null) consumer.accept(e);
            }
            return new Report(name, index, header, verified, empty, jpegs, payloadBytes);
        }
    }
    private static void checkIndex(int index) throws IOException { if (index < 0 || index >= NAMES.size()) throw new IOException("Unknown save archive identity"); }
    private static byte[] decrypt(byte[] encrypted, byte[] iv) throws IOException {
        try { Cipher cipher = Cipher.getInstance("AES/CBC/NoPadding"); cipher.init(Cipher.DECRYPT_MODE, new SecretKeySpec(KEY, "AES"), new IvParameterSpec(iv)); return cipher.doFinal(encrypted); }
        catch (GeneralSecurityException e) { throw new IOException("Save decryption failed", e); }
    }
    private static byte[] iv(byte[] bytes, int offset, int seed) {
        Rng rng = new Rng(seed); byte[] iv = new byte[16];
        for (int i = 0; i < 16; i++) iv[i] = (byte)(bytes[offset+i] ^ (rng.next(0xfe) + 1));
        return iv;
    }
    private static final class Rng {
        int x,y,z,w;
        Rng(int seed) { w = 0x41C64E6D * seed + 12345; z = 0x41C64E6D * w + 12345; y = 0x41C64E6D * z + 12345; x = 0x41C64E6D * y + 12345; }
        int next() { int t = x ^ (x << 11); x=y; y=z; z=w; w=w ^ (w >> 19) ^ t ^ (t >> 8); return w & 0x7fffffff; }
        int next(int limit) { int divisor = (int)(0x80000000L / (limit + 1L)); while (true) { int result = next() / divisor; if (result <= limit) return result; } }
    }
}
