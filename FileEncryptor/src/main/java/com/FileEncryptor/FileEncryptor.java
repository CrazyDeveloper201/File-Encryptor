package com.FileEncryptor;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.security.GeneralSecurityException;
import java.security.SecureRandom;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;

/**
 * FileEncryptor - Chunked AES-GCM based file encryption/decryption.
 *
 * Unlike a single-shot GCM stream (where the whole file must be processed
 * before ANY plaintext / progress can be released), this format encrypts
 * the file in independent chunks, each with its own IV and auth tag.
 * That means decrypt() can verify + write + report progress after EVERY
 * chunk, giving smooth real progress instead of a 0 -> 100 jump at the end.
 *
 * File Format:
 * [MAGIC: 4 bytes]
 * [VERSION: 1 byte]
 * [SALT: 16 bytes]
 * [PLAINTEXT_TOTAL_SIZE: 8 bytes]
 * [CHUNK_PLAIN_SIZE: 4 bytes]
 * repeated chunks:
 *   [CHUNK_IV: 12 bytes]
 *   [CHUNK_CIPHER_LEN: 4 bytes]   (ciphertext length, includes 16 byte GCM tag)
 *   [CHUNK_CIPHERTEXT+TAG: CHUNK_CIPHER_LEN bytes]
 */
public final class FileEncryptor {

    private static final String MAGIC = "FEN2";
    private static final byte VERSION = 2;

    private static final int SALT_LENGTH = 16;
    private static final int IV_LENGTH = 12;

    private static final int KEY_LENGTH = 256;
    private static final int ITERATIONS = 150000;

    private static final int GCM_TAG_LENGTH_BITS = 128;
    private static final int GCM_TAG_LENGTH_BYTES = 16;

    // Plaintext bytes encrypted per chunk. Each chunk is independently
    // authenticated, so this is also the progress "resolution".
    private static final int CHUNK_PLAIN_SIZE = 1024 * 1024; // 1 MB

    private FileEncryptor() {
    }

    public interface EncryptProgressCallback {
        void onProgress(long bytesTransferred, long totalBytes, int percentage);
        void onSuccess();
        void onError(String message);
    }

    public interface DecryptProgressCallback {
        void onProgress(long bytesTransferred, long totalBytes, int percentage);
        void onSuccess();
        void onError(String message);
    }

    // ============================================================
    // ENCRYPT
    // ============================================================

    public static void encrypt(String inputPath, String encryptedPath, String password)
    throws Exception {
        encrypt(inputPath, encryptedPath, password, null);
    }

    public static void encrypt(
        String inputPath,
        String encryptedPath,
        String password,
        EncryptProgressCallback callback
    ) throws Exception {

        checkPassword(password);

        File inputFile = new File(inputPath);
        File outputFile = new File(encryptedPath);

        if (!inputFile.exists()) {
            throw new IOException("Input file does not exist: " + inputPath);
        }
        if (!inputFile.isFile()) {
            throw new IOException("Input path is not a file: " + inputPath);
        }

        File parent = outputFile.getParentFile();
        if (parent != null && !parent.exists()) {
            if (!parent.mkdirs() && !parent.exists()) {
                throw new IOException("Could not create output directory: " + parent);
            }
        }

        SecureRandom random = new SecureRandom();

        byte[] salt = new byte[SALT_LENGTH];
        random.nextBytes(salt);

        SecretKey key = generateKey(password, salt);

        long totalBytes = inputFile.length();
        long transferred = 0;
        int lastPercentage = -1;

        FileInputStream fis = null;
        BufferedInputStream bis = null;
        FileOutputStream fos = null;
        DataOutputStream dos = null;

        try {
            fis = new FileInputStream(inputFile);
            bis = new BufferedInputStream(fis, CHUNK_PLAIN_SIZE);

            fos = new FileOutputStream(outputFile);
            dos = new DataOutputStream(new BufferedOutputStream(fos));

            // ===== Header =====
            dos.write(MAGIC.getBytes("UTF-8"));
            dos.writeByte(VERSION);
            dos.write(salt);
            dos.writeLong(totalBytes);
            dos.writeInt(CHUNK_PLAIN_SIZE);

            if (callback != null) {
                callback.onProgress(0, totalBytes, 0);
            }

            byte[] plainBuffer = new byte[CHUNK_PLAIN_SIZE];
            int read;

            while ((read = readChunk(bis, plainBuffer)) > 0) {

                byte[] chunkIv = new byte[IV_LENGTH];
                random.nextBytes(chunkIv);

                Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
                GCMParameterSpec spec = new GCMParameterSpec(GCM_TAG_LENGTH_BITS, chunkIv);
                cipher.init(Cipher.ENCRYPT_MODE, key, spec);

                byte[] cipherChunk = cipher.doFinal(plainBuffer, 0, read);

                dos.write(chunkIv);
                dos.writeInt(cipherChunk.length);
                dos.write(cipherChunk);

                transferred += read;

                int percentage = calculatePercentage(transferred, totalBytes);
                if (percentage != lastPercentage) {
                    lastPercentage = percentage;
                    if (callback != null) {
                        callback.onProgress(transferred, totalBytes, percentage);
                    }
                }
            }

            dos.flush();

            if (callback != null) {
                callback.onProgress(totalBytes, totalBytes, 100);
                callback.onSuccess();
            }

        } catch (Exception e) {
            safeDelete(outputFile);
            if (callback != null) {
                callback.onError(getErrorMessage(e));
            }
            throw e;

        } finally {
            safeClose(dos);
            safeClose(fos);
            safeClose(bis);
            safeClose(fis);
        }
    }

    // ============================================================
    // DECRYPT  (chunked -> real incremental progress)
    // ============================================================

    public static void decrypt(String encryptedPath, String outputPath, String password)
    throws Exception {
        decrypt(encryptedPath, outputPath, password, null);
    }

    public static void decrypt(
        String encryptedPath,
        String outputPath,
        String password,
        DecryptProgressCallback callback
    ) throws Exception {

        checkPassword(password);

        File encryptedFile = new File(encryptedPath);
        File outputFile = new File(outputPath);

        if (!encryptedFile.exists()) {
            throw new IOException("Encrypted file does not exist: " + encryptedPath);
        }
        if (!encryptedFile.isFile()) {
            throw new IOException("Encrypted path is not a file: " + encryptedPath);
        }

        File parent = outputFile.getParentFile();
        if (parent != null && !parent.exists()) {
            if (!parent.mkdirs() && !parent.exists()) {
                throw new IOException("Could not create output directory: " + parent);
            }
        }

        FileInputStream fis = null;
        DataInputStream dis = null;
        FileOutputStream fos = null;
        BufferedOutputStream bos = null;

        try {
            fis = new FileInputStream(encryptedFile);
            dis = new DataInputStream(new BufferedInputStream(fis, CHUNK_PLAIN_SIZE));

            // ===== Header =====
            byte[] magic = new byte[4];
            dis.readFully(magic);
            if (!MAGIC.equals(new String(magic, "UTF-8"))) {
                throw new IOException("Invalid encrypted file: wrong magic number");
            }

            int version = dis.readUnsignedByte();
            if (version != VERSION) {
                throw new IOException("Unsupported encrypted file version: " + version);
            }

            byte[] salt = new byte[SALT_LENGTH];
            dis.readFully(salt);

            long totalPlainBytes = dis.readLong();
            int chunkPlainSize = dis.readInt();

            SecretKey key = generateKey(password, salt);

            fos = new FileOutputStream(outputFile);
            bos = new BufferedOutputStream(fos, Math.max(chunkPlainSize, 64 * 1024));

            long plainBytesWritten = 0;
            int lastPercentage = -1;

            if (callback != null) {
                callback.onProgress(0, totalPlainBytes, 0);
            }

            byte[] chunkIv = new byte[IV_LENGTH];

            while (plainBytesWritten < totalPlainBytes) {
                dis.readFully(chunkIv);
                int cipherLen = dis.readInt();

                if (cipherLen <= 0 || cipherLen > (chunkPlainSize + GCM_TAG_LENGTH_BYTES + 1024)) {
                    throw new IOException("Corrupted encrypted file: invalid chunk length");
                }

                byte[] cipherChunk = new byte[cipherLen];
                dis.readFully(cipherChunk);

                Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
                GCMParameterSpec spec = new GCMParameterSpec(GCM_TAG_LENGTH_BITS, chunkIv);
                cipher.init(Cipher.DECRYPT_MODE, key, spec);

                // Throws AEADBadTagException here if wrong password / tampered chunk.
                byte[] plainChunk = cipher.doFinal(cipherChunk);

                bos.write(plainChunk);
                plainBytesWritten += plainChunk.length;

                int percentage = calculatePercentage(plainBytesWritten, totalPlainBytes);
                if (percentage != lastPercentage) {
                    lastPercentage = percentage;
                    if (callback != null) {
                        callback.onProgress(plainBytesWritten, totalPlainBytes, percentage);
                    }
                }
            }

            bos.flush();

            if (callback != null) {
                callback.onProgress(totalPlainBytes, totalPlainBytes, 100);
                callback.onSuccess();
            }

        } catch (Exception e) {
            safeDelete(outputFile);

            String message = getDecryptErrorMessage(e);
            if (callback != null) {
                callback.onError(message);
            }

            throw new SecurityException(message, e);

        } finally {
            safeClose(bos);
            safeClose(fos);
            safeClose(dis);
            safeClose(fis);
        }
    }

    // ============================================================
    // HELPERS
    // ============================================================

    private static int readChunk(java.io.InputStream in, byte[] buffer) throws IOException {
        int totalRead = 0;
        while (totalRead < buffer.length) {
            int read = in.read(buffer, totalRead, buffer.length - totalRead);
            if (read == -1) {
                break;
            }
            totalRead += read;
        }
        return totalRead;
    }

    private static SecretKey generateKey(String password, byte[] salt)
    throws GeneralSecurityException {

        PBEKeySpec spec = new PBEKeySpec(
            password.toCharArray(),
            salt,
            ITERATIONS,
            KEY_LENGTH
        );

        try {
            SecretKeyFactory factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");
            byte[] keyBytes = factory.generateSecret(spec).getEncoded();
            return new SecretKeySpec(keyBytes, "AES");
        } finally {
            spec.clearPassword();
        }
    }

    private static int calculatePercentage(long current, long total) {
        if (total <= 0) {
            return 100;
        }
        if (current >= total) {
            return 100;
        }
        long percentage = (current * 100L) / total;
        return (int) Math.max(0, Math.min(100, percentage));
    }

    private static void checkPassword(String password) {
        if (password == null || password.isEmpty()) {
            throw new IllegalArgumentException("Password cannot be empty");
        }
        if (password.length() < 4) {
            throw new IllegalArgumentException("Password must be at least 4 characters");
        }
    }

    private static String getDecryptErrorMessage(Exception e) {
        Throwable cause = e;
        while (cause != null) {
            String className = cause.getClass().getName();
            if (className.contains("AEADBadTagException") ||
                className.contains("BadPaddingException")) {
                return "Wrong password or corrupted file. Please check your password.";
            }
            cause = cause.getCause();
        }

        String message = e.getMessage();
        if (message != null && !message.isEmpty()) {
            return message;
        }
        return "Wrong password or corrupted file.";
    }

    private static String getErrorMessage(Exception e) {
        String message = e.getMessage();
        if (message != null && !message.isEmpty()) {
            return message;
        }
        return "Operation failed. Please try again.";
    }

    private static void safeClose(java.io.Closeable closeable) {
        if (closeable == null) {
            return;
        }
        try {
            closeable.close();
        } catch (Exception ignored) {
        }
    }

    private static void safeDelete(File file) {
        if (file == null) {
            return;
        }
        try {
            if (file.exists()) {
                file.delete();
            }
        } catch (Exception ignored) {
        }
    }
}
