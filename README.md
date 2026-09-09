# 🔐 FileEncryptor

**FileEncryptor** is a secure and lightweight Android Java library for encrypting and decrypting files using **AES-256-GCM**.

It is designed for Android applications where you need secure file encryption, password-based key derivation, chunk-based processing, and progress callbacks without adding external runtime dependencies.

---

## ✨ Features

- 🔐 **AES-256-GCM encryption**
- 🔑 **Password-based key derivation**
- 🧂 **Random salt**
- 🔄 **Random IV for each chunk**
- 📦 **Chunk-based file processing**
- 📊 **Encryption progress callbacks**
- 📊 **Decryption progress callbacks**
- ⚡ **Asynchronous encryption/decryption**
- ❌ **Wrong password detection**
- 🛡️ **File integrity and authentication protection**
- 📱 **Android-friendly API**
- 🚫 **No external runtime dependencies**

---

## 📦 Installation

### JitPack

Add the JitPack repository to your project:

```gradle
repositories {
    maven { url 'https://jitpack.io' }
}
```

Then add the dependency:

```gradle
dependencies {
    implementation 'com.github.CrazyDeveloper201:File-Encryptor:1.0.0'
}
```

> **Note:** Replace `1.0.1` with the latest released version when available.

---

## 📥 Import

```java
import com.FileEncryptor.FileEncryptor;
import com.FileEncryptor.FileCryptoTask;
```

---

# 🔐 Encrypt a File

Use `FileEncryptor.encrypt()` for synchronous file encryption.

```java
FileEncryptor.encrypt(
    inputPath,
    encryptedPath,
    password
);
```

### Example

```java
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
```

### ⚠️ Android UI Note

Synchronous encryption performs file processing on the calling thread. For Android UI applications, prefer [`FileCryptoTask`](#-async-encryption) so that large files do not block the main thread.

---

# 🔓 Decrypt a File

Use `FileEncryptor.decrypt()` to decrypt an encrypted file.

```java
FileEncryptor.decrypt(
    encryptedPath,
    outputPath,
    password
);
```

### Example

```java
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
```

If the password is incorrect or the encrypted data has been modified/corrupted, authentication will fail and decryption will report an error.

---

# ⚡ Async Encryption

For Android applications, use `FileCryptoTask` for background processing.

```java
FileCryptoTask.encryptAsync(
    inputPath,
    encryptedPath,
    password,
    callback
);
```

This allows file encryption to run outside the main UI thread.

---

# ⚡ Async Decryption

```java
FileCryptoTask.decryptAsync(
    encryptedPath,
    outputPath,
    password,
    callback
);
```

Use this approach when decrypting large files to keep the Android UI responsive.

---

# 📊 Progress Callback

## Encryption Progress

```java
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
```

### Progress Values

| Parameter | Description |
|---|---|
| `bytesTransferred` | Number of bytes processed |
| `totalBytes` | Total input file size |
| `percentage` | Progress percentage from 0–100 |

---

## Decryption Progress

```java
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
```

---

# 📚 API Reference

## `FileEncryptor`

| Function | Description |
|---|---|
| `encrypt()` | Encrypt a file |
| `encrypt(..., callback)` | Encrypt a file with progress callbacks |
| `decrypt()` | Decrypt a file |
| `decrypt(..., callback)` | Decrypt a file with progress callbacks |

---

## `FileCryptoTask`

| Function | Description |
|---|---|
| `encryptAsync()` | Encrypt a file in the background |
| `decryptAsync()` | Decrypt a file in the background |
| `shutdown()` | Shut down the internal executor |

---

# 🔒 Security

FileEncryptor uses the following cryptographic components:

| Security Component | Configuration |
|---|---|
| Encryption | **AES-GCM** |
| Key size | **256-bit** |
| Key derivation | **PBKDF2-HMAC-SHA256** |
| PBKDF2 iterations | **150,000** |
| Salt | **Random** |
| IV | **Random IV per chunk** |
| Authentication | **GCM authentication tag** |

### Password Security

The library does **not** store your password.

The password is used to derive the encryption key and is not persisted by FileEncryptor.

> ⚠️ **Important:** Keep your password safe. If the password is lost, the encrypted file may not be recoverable.

---

# 📦 Chunk-Based Encryption

FileEncryptor processes files in chunks instead of loading the entire file into memory.

```text
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
```

### Why chunks?

Chunk-based processing helps:

- Reduce memory usage
- Process large files
- Report encryption progress
- Report decryption progress
- Avoid loading the complete file into RAM

---

# 🛡️ File Integrity & Authentication

AES-GCM provides authenticated encryption.

This allows FileEncryptor to detect:

- ❌ Incorrect password
- ❌ Corrupted encrypted data
- ❌ Modified encrypted data
- ❌ Tampered ciphertext

If authentication fails during decryption, the operation reports an error instead of silently returning unauthenticated plaintext.

---

# 📱 Requirements

| Requirement | Version |
|---|---|
| Platform | Android |
| Language | Java |
| Minimum SDK | **21** |
| Runtime dependencies | **None** |

---

# 🧪 Basic Workflow

```text
        Password
           │
           ▼
    ┌──────────────┐
    │ Key Derivation│
    └──────┬───────┘
           │
           ▼
       AES-256-GCM
           │
           ▼
    ┌──────────────┐
    │ Encrypted File│
    └──────────────┘
```

For decryption:

```text
   Encrypted File
         │
         ▼
   Password + Salt
         │
         ▼
    Key Derivation
         │
         ▼
     AES-256-GCM
         │
    ┌────┴────┐
    │         │
    ▼         ▼
 Success    Failure
    │         │
    ▼         ▼
Original   Error
  File
```

---

# ⚠️ Security Recommendations

For better application security:

1. Use a strong, high-entropy password.
2. Never hard-code sensitive passwords in your application.
3. Do not log passwords.
4. Do not send passwords to a server unnecessarily.
5. Store passwords securely if your application must remember them.
6. Keep the encrypted file and password protected separately.
7. Use the latest stable FileEncryptor release.

> **Important:** Encryption protects the file, but the security of the password and the surrounding application also matters.

---

# 📄 License

See the [`LICENSE`](LICENSE) file for license information.

---

# 👨‍💻 Author

**CrazyDeveloper201**

GitHub:  
https://github.com/CrazyDeveloper201

---

# ⭐ Support

If **FileEncryptor** is useful for your Android project:

- ⭐ Give the repository a star
- 🐛 Report bugs
- 💡 Suggest improvements
- 🔧 Contribute improvements

---

## 🚀 FileEncryptor

**Secure. Lightweight. Android-friendly.**

Made for developers who need simple and secure file encryption in Android Java applications.
