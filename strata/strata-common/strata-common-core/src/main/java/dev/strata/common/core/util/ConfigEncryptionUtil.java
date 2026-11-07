// SPDX-License-Identifier: Apache-2.0
package dev.strata.common.core.util;

import org.jasypt.encryption.pbe.StandardPBEStringEncryptor;
import org.jasypt.encryption.pbe.config.EnvironmentStringPBEConfig;

/**
 * Configuration encryption utility for Jasypt.
 *
 * <p>Usage:
 * <pre>{@code
 * // Encrypt a value
 * String encrypted = ConfigEncryptionUtil.encrypt("my-secret-password", "my-encryption-key");
 *
 * // Decrypt a value
 * String decrypted = ConfigEncryptionUtil.decrypt(encrypted, "my-encryption-key");
 *
 * // In application.yml, use ENC(...) format:
 * // spring.datasource.password: ENC(encrypted-value-here)
 * }</pre>
 *
 * <p>The encryption key should be provided via environment variable
 * {@code JASYPT_ENCRYPTOR_PASSWORD}, never committed to source control.
 */
public final class ConfigEncryptionUtil {

    private static final String ALGORITHM = "PBEWITHHMACSHA512ANDAES_256";
    private static final int IV_GENERATOR_IV_SIZE = 16;
    private static final int SALT_GENERATOR_SALT_SIZE = 16;
    private static final int KEY_OBTENTION_ITERATIONS = 1000;
    private static final String POOL_SIZE = "1";

    private ConfigEncryptionUtil() {
        // Utility class
    }

    /**
     * Encrypt a plaintext value using the provided encryption key.
     *
     * @param plaintext the value to encrypt
     * @param password  the encryption key (should be at least 16 characters)
     * @return the encrypted value (without ENC() wrapper)
     */
    public static String encrypt(String plaintext, String password) {
        StandardPBEStringEncryptor encryptor = createEncryptor(password);
        return encryptor.encrypt(plaintext);
    }

    /**
     * Decrypt an encrypted value using the provided encryption key.
     *
     * @param encrypted the encrypted value (without ENC() wrapper)
     * @param password  the encryption key
     * @return the decrypted plaintext value
     */
    public static String decrypt(String encrypted, String password) {
        StandardPBEStringEncryptor encryptor = createEncryptor(password);
        return encryptor.decrypt(encrypted);
    }

    /**
     * Wrap an encrypted value in ENC(...) format for use in configuration files.
     *
     * @param encryptedValue the encrypted value
     * @return the value wrapped in ENC(...)
     */
    public static String wrap(String encryptedValue) {
        return "ENC(" + encryptedValue + ")";
    }

    /**
     * Encrypt a value and wrap it in ENC(...) format.
     *
     * @param plaintext the value to encrypt
     * @param password  the encryption key
     * @return the encrypted value wrapped in ENC(...)
     */
    public static String encryptAndWrap(String plaintext, String password) {
        return wrap(encrypt(plaintext, password));
    }

    /**
     * Check if a value is encrypted (wrapped in ENC(...)).
     *
     * @param value the value to check
     * @return true if the value is wrapped in ENC(...)
     */
    public static boolean isEncrypted(String value) {
        return value != null && value.startsWith("ENC(") && value.endsWith(")");
    }

    /**
     * Extract the encrypted value from ENC(...) wrapper.
     *
     * @param wrappedValue the value wrapped in ENC(...)
     * @return the raw encrypted value
     */
    public static String unwrap(String wrappedValue) {
        if (isEncrypted(wrappedValue)) {
            return wrappedValue.substring(4, wrappedValue.length() - 1);
        }
        return wrappedValue;
    }

    /**
     * Create a configured StandardPBEStringEncryptor.
     *
     * @param password the encryption key
     * @return configured encryptor
     */
    private static StandardPBEStringEncryptor createEncryptor(String password) {
        StandardPBEStringEncryptor encryptor = new StandardPBEStringEncryptor();
        EnvironmentStringPBEConfig config = new EnvironmentStringPBEConfig();
        config.setAlgorithm(ALGORITHM);
        config.setPassword(password);
        config.setIvGeneratorClassName("org.jasypt.iv.RandomIvGenerator");
        config.setSaltGeneratorClassName("org.jasypt.salt.RandomSaltGenerator");
        config.setKeyObtentionIterations(KEY_OBTENTION_ITERATIONS);
        config.setPoolSize(POOL_SIZE);
        config.setStringOutputType("base64");
        encryptor.setConfig(config);
        return encryptor;
    }

    /**
     * Main method for command-line encryption.
     *
     * <p>Usage: {@code java -cp ... dev.strata.common.core.util.ConfigEncryptionUtil <plaintext> <password>}
     *
     * @param args command line arguments: [0] plaintext, [1] password
     */
    public static void main(String[] args) {
        if (args.length < 2) {
            System.err.println("Usage: ConfigEncryptionUtil <plaintext> <password>");
            System.err.println("Example: ConfigEncryptionUtil my-secret-password my-encryption-key");
            System.exit(1);
        }

        String plaintext = args[0];
        String password = args[1];

        String encrypted = encrypt(plaintext, password);
        String wrapped = wrap(encrypted);

        System.out.println("Plaintext: " + plaintext);
        System.out.println("Encrypted: " + encrypted);
        System.out.println("Wrapped (for config): " + wrapped);
        System.out.println();
        System.out.println("Add to application.yml:");
        System.out.println("  sensitive-property: " + wrapped);
        System.out.println();
        System.out.println("Set environment variable:");
        System.out.println("  export JASYPT_ENCRYPTOR_PASSWORD=" + password);
    }
}
