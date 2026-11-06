// SPDX-License-Identifier: Apache-2.0
package dev.strata.common.security.crypto;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Unit tests for {@link EncryptedFieldListener}: encrypt-before-persist and decrypt-after-load driven by
 * {@link Encrypted} fields, including null/empty guards, the already-encrypted skip, and the superclass
 * walk. The static encryptor is restored after every test so global state does not leak.
 */
@DisplayName("EncryptedFieldListener 字段加解密监听")
@ExtendWith(MockitoExtension.class)
class EncryptedFieldListenerTest {

    @Mock
    private FieldEncryptor encryptor;

    static class Entity {
        @Encrypted
        String secret;

        String plain;
    }

    static class Child extends Entity {
        @Encrypted
        String childSecret;
    }

    @AfterEach
    void tearDown() {
        EncryptedFieldListener.setFieldEncryptor(null);
    }

    private void stubEncryptor() {
        lenient().when(encryptor.isEncrypted(anyString())).thenAnswer(inv -> ((String) inv.getArgument(0))
                .startsWith("ENC:"));
        lenient().when(encryptor.encrypt(anyString())).thenAnswer(inv -> "ENC:" + inv.getArgument(0));
        lenient().when(encryptor.decrypt(anyString())).thenAnswer(inv -> ((String) inv.getArgument(0)).substring(4));
    }

    @Test
    @DisplayName("未配置 encryptor 时 encrypt/decrypt 为空操作")
    void noOpWithoutEncryptor() {
        Entity entity = new Entity();
        entity.secret = "plain";

        EncryptedFieldListener listener = new EncryptedFieldListener();
        listener.encrypt(entity);
        listener.decrypt(entity);

        assertThat(entity.secret).isEqualTo("plain");
    }

    @Test
    @DisplayName("持久化前加密未加密字段，已加密/空值跳过")
    void encryptsPlaintextAndSkipsEncrypted() {
        stubEncryptor();
        EncryptedFieldListener.setFieldEncryptor(encryptor);
        Entity entity = new Entity();
        entity.secret = "plain";
        Entity already = new Entity();
        already.secret = "ENC:already";
        Entity empty = new Entity();
        empty.secret = "";

        new EncryptedFieldListener().encrypt(entity);
        new EncryptedFieldListener().encrypt(already);
        new EncryptedFieldListener().encrypt(empty);

        assertThat(entity.secret).isEqualTo("ENC:plain");
        assertThat(already.secret).isEqualTo("ENC:already");
        verify(encryptor, never()).decrypt(anyString());
    }

    @Test
    @DisplayName("加载后解密已加密字段，未加密/空值跳过")
    void decryptsEncryptedAndSkipsPlaintext() {
        stubEncryptor();
        EncryptedFieldListener.setFieldEncryptor(encryptor);
        Entity encrypted = new Entity();
        encrypted.secret = "ENC:secret";
        Entity plain = new Entity();
        plain.secret = "still-plain";

        new EncryptedFieldListener().decrypt(encrypted);
        new EncryptedFieldListener().decrypt(plain);

        assertThat(encrypted.secret).isEqualTo("secret");
        assertThat(plain.secret).isEqualTo("still-plain");
    }

    @Test
    @DisplayName("沿继承链处理父类与子类的 @Encrypted 字段")
    void walksSuperclassChain() {
        stubEncryptor();
        EncryptedFieldListener.setFieldEncryptor(encryptor);
        Child child = new Child();
        child.secret = "parent";
        child.childSecret = "child";

        new EncryptedFieldListener().encrypt(child);

        assertThat(child.secret).isEqualTo("ENC:parent");
        assertThat(child.childSecret).isEqualTo("ENC:child");
    }

    @Test
    @DisplayName("null 字段值不触发加解密")
    void nullValueIsIgnored() {
        stubEncryptor();
        EncryptedFieldListener.setFieldEncryptor(encryptor);
        Entity entity = new Entity();
        entity.secret = null;

        new EncryptedFieldListener().encrypt(entity);
        new EncryptedFieldListener().decrypt(entity);

        assertThat(entity.secret).isNull();
        verify(encryptor, never()).encrypt(anyString());
    }
}
