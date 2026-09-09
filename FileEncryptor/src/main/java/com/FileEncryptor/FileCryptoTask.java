package com.FileEncryptor;

import android.os.Handler;
import android.os.Looper;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Runs FileEncryptor.encrypt/decrypt on a background thread and
 * delivers all progress/success/error callbacks on the UI (main) thread.
 *
 * Usage:
 *
 *   FileCryptoTask.decryptAsync(
 *       inputPath, outputPath, password,
 *       new FileEncryptor.DecryptProgressCallback() {
 *           @Override public void onProgress(long done, long total, int pct) {
 *               progressBar.setProgress(pct);
 *           }
 *           @Override public void onSuccess() {
 *               Toast.makeText(ctx, "Decrypted!", Toast.LENGTH_SHORT).show();
 *           }
 *           @Override public void onError(String message) {
 *               Toast.makeText(ctx, message, Toast.LENGTH_LONG).show();
 *           }
 *       }
 *   );
 
 FileCryptoTask.encryptAsync(
 *       inputPath, outputPath, password,
 *       new FileEncryptor.DecryptProgressCallback() {
 *           @Override public void onProgress(long done, long total, int pct) {
 *               progressBar.setProgress(pct);
 *           }
 *           @Override public void onSuccess() {
 *               Toast.makeText(ctx, "Decrypted!", Toast.LENGTH_SHORT).show();
 *           }
 *           @Override public void onError(String message) {
 *               Toast.makeText(ctx, message, Toast.LENGTH_LONG).show();
 *           }
 *       }
 *   );
 
 */
public final class FileCryptoTask {

    private static final ExecutorService EXECUTOR = Executors.newSingleThreadExecutor();
    private static final Handler MAIN_HANDLER = new Handler(Looper.getMainLooper());

    private FileCryptoTask() {
    }

    public static void encryptAsync(
        final String inputPath,
        final String encryptedPath,
        final String password,
        final FileEncryptor.EncryptProgressCallback callback
    ) {
        EXECUTOR.execute(new Runnable() {
            @Override
            public void run() {
                try {
                    FileEncryptor.encrypt(inputPath, encryptedPath, password,
                        new FileEncryptor.EncryptProgressCallback() {
                            @Override
                            public void onProgress(final long bytesTransferred, final long totalBytes, final int percentage) {
                                MAIN_HANDLER.post(new Runnable() {
                                    @Override public void run() {
                                        if (callback != null) callback.onProgress(bytesTransferred, totalBytes, percentage);
                                    }
                                });
                            }

                            @Override
                            public void onSuccess() {
                                MAIN_HANDLER.post(new Runnable() {
                                    @Override public void run() {
                                        if (callback != null) callback.onSuccess();
                                    }
                                });
                            }

                            @Override
                            public void onError(final String message) {
                                MAIN_HANDLER.post(new Runnable() {
                                    @Override public void run() {
                                        if (callback != null) callback.onError(message);
                                    }
                                });
                            }
                        });
                } catch (Exception e) {
                    // onError already delivered above in normal flow;
                    // this guards against exceptions thrown before the
                    // callback wrapper was reached.
                    final String message = e.getMessage() != null ? e.getMessage() : "Encryption failed.";
                    MAIN_HANDLER.post(new Runnable() {
                        @Override public void run() {
                            if (callback != null) callback.onError(message);
                        }
                    });
                }
            }
        });
    }

    public static void decryptAsync(
        final String encryptedPath,
        final String outputPath,
        final String password,
        final FileEncryptor.DecryptProgressCallback callback
    ) {
        EXECUTOR.execute(new Runnable() {
            @Override
            public void run() {
                try {
                    FileEncryptor.decrypt(encryptedPath, outputPath, password,
                        new FileEncryptor.DecryptProgressCallback() {
                            @Override
                            public void onProgress(final long bytesTransferred, final long totalBytes, final int percentage) {
                                MAIN_HANDLER.post(new Runnable() {
                                    @Override public void run() {
                                        if (callback != null) callback.onProgress(bytesTransferred, totalBytes, percentage);
                                    }
                                });
                            }

                            @Override
                            public void onSuccess() {
                                MAIN_HANDLER.post(new Runnable() {
                                    @Override public void run() {
                                        if (callback != null) callback.onSuccess();
                                    }
                                });
                            }

                            @Override
                            public void onError(final String message) {
                                MAIN_HANDLER.post(new Runnable() {
                                    @Override public void run() {
                                        if (callback != null) callback.onError(message);
                                    }
                                });
                            }
                        });
                } catch (Exception e) {
                    final String message = e.getMessage() != null ? e.getMessage() : "Decryption failed.";
                    MAIN_HANDLER.post(new Runnable() {
                        @Override public void run() {
                            if (callback != null) callback.onError(message);
                        }
                    });
                }
            }
        });
    }

    /** Call when your app/activity is fully shutting down, if ever needed. */
    public static void shutdown() {
        EXECUTOR.shutdown();
    }
}
