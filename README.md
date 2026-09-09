# FileEncryptor

A secure Android Java library for encrypting and decrypting files using AES-GCM.

## Features

- AES-256-GCM encryption
- Password-based key derivation
- Chunk-based encryption
- Encryption/decryption progress
- Async encryption/decryption
- Wrong password detection
- File integrity protection

## Installation

### JitPack

```gradle
repositories {
    maven { url 'https://jitpack.io' }
}

dependencies {
    implementation 'com.github.CrazyDeveloper201:FileEncryptor:1.0.0'
}