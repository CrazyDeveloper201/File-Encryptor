🔐 FileEncryptor

A secure and lightweight Android Java library for file encryption and decryption using AES-GCM.

✨ Features

- 🔐 AES-256-GCM encryption
- 🔑 Password-based key derivation
- 🧂 Random salt
- 🔄 Random IV for each chunk
- 📦 Chunk-based file processing
- 📊 Encryption progress
- 📊 Decryption progress
- ⚡ Async encryption/decryption
- ❌ Wrong password detection
- 🛡️ File integrity/authentication protection
- 📱 Android-friendly API
- 🚫 No external runtime dependencies

---

📦 Installation

JitPack

Add the JitPack repository:

repositories {
    maven { url 'https://jitpack.io' }
}

Add the dependency:

dependencies {
    implementation 'com.github.CrazyDeveloper201:File-Encryptor:1.0.0'
}

---

📥 Import

import com.FileEncryptor.FileEncryptor;
import com.FileEncryptor.FileCryptoTask;

---

🔐 Encrypt a File

FileEncryptor.encrypt(
    inputPath,
    encryptedPath,
    password
);

Example:

String inputPath =
        "/storage/emulated/0/Documents/photo.jpg";

String encryptedPath =
        "/storage/emulated/0/Documents/photo.enc";

String password =
        "MyStrongPassword123";

try {
    FileEncryptor.encrypt(
        inputPath,
        encryptedPath,
        password
    );

    System.out.println("Encryption successful");

} catch (Exception e) {
    System.out.println(
        "Encryption failed: " + e.getMessage()
    );
}

---

🔓 Decrypt a File

FileEncryptor.decrypt(
    encryptedPath,
    outputPath,
    password
);

Example:

String encryptedPath =
        "/storage/emulated/0/Documents/photo.enc";

String outputPath =
        "/storage/emulated/0/Documents/photo.jpg";

String password =
        "MyStrongPassword123";

try {
    FileEncryptor.decrypt(
        encryptedPath,
        outputPath,
        password
    );

    System.out.println("Decryption successful");

} catch (Exception e) {
    System.out.println(
        "Decryption failed: " + e.getMessage()
    );
}

---

⚡ Async Encryption

For Android applications, use "FileCryptoTask" for background processing.

FileCryptoTask.encryptAsync(
    inputPath,
    encryptedPath,
    password,
    callback
);

---

⚡ Async Decryption

FileCryptoTask.decryptAsync(
    encryptedPath,
    outputPath,
    password,
    callback
);

---

📊 Progress Callback

Encryption

new FileEncryptor.EncryptProgressCallback() {

    @Override
    public void onProgress(
            long bytesTransferred,
            long totalBytes,
            int percentage) {

        // Update ProgressBar
    }

    @Override
    public void onSuccess() {

        // Encryption completed
    }

    @Override
    public void onError(String message) {

        // Encryption failed
    }
};

Decryption

new FileEncryptor.DecryptProgressCallback() {

    @Override
    public void onProgress(
            long bytesTransferred,
            long totalBytes,
            int percentage) {

        // Update ProgressBar
    }

    @Override
    public void onSuccess() {

        // Decryption completed
    }

    @Override
    public void onError(String message) {

        // Decryption failed
    }
};

---

📚 API

"FileEncryptor"

Function| Description
"encrypt()"| Encrypt a file
"encrypt(..., callback)"| Encrypt with progress
"decrypt()"| Decrypt a file
"decrypt(..., callback)"| Decrypt with progress

"FileCryptoTask"

Function| Description
"encryptAsync()"| Encrypt in background
"decryptAsync()"| Decrypt in background
"shutdown()"| Shutdown internal executor

---

🔒 Security

FileEncryptor uses:

- AES-GCM
- 256-bit encryption key
- PBKDF2-HMAC-SHA256
- 150,000 PBKDF2 iterations
- Random salt
- Random IV per chunk
- Authentication tag

The password is not stored by the library.

«⚠️ Keep your password safe. If the password is lost, the encrypted file may not be recoverable.»

---

📦 Chunk-Based Encryption

Files are processed in chunks instead of loading the entire file into memory.

Input File
    │
    ▼
┌─────────────┐
│   Chunk 1   │
├─────────────┤
│   Chunk 2   │
├─────────────┤
│   Chunk 3   │
├─────────────┤
│     ...     │
└─────────────┘
    │
    ▼
Encrypted File

This allows progress to be reported while large files are being processed.

---

🛡️ File Integrity

AES-GCM authentication helps detect:

- Incorrect password
- Corrupted encrypted data
- Modified/tampered encrypted data

If authentication fails, decryption reports an error.

---

📱 Requirements

- Android
- Java
- Minimum SDK: 21

---

📄 License

See the ""LICENSE"" (LICENSE) file for license information.

---

👨‍💻 Author

CrazyDeveloper201

GitHub:
https://github.com/CrazyDeveloper201

---

⭐ Support

If this library is useful for your project, consider giving the repository a ⭐ on GitHub.