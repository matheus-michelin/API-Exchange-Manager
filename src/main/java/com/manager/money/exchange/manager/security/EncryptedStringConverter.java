package com.manager.money.exchange.manager.security;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.SecureRandom;
import java.util.Base64;


@Converter
public class EncryptedStringConverter implements AttributeConverter<String, String> {

    private static final String PREFIX = "v1:";
    private static final int IV_BYTES = 12;
    private static final int TAG_BITS = 128;
    private static final SecureRandom RANDOM = new SecureRandom();

    private final SecretKeySpec key;

    public EncryptedStringConverter() {
        this(System.getenv("APP_ENCRYPTION_KEY"));
    }

    EncryptedStringConverter(String base64Key) {
        byte[] k;
        try {
            k = base64Key == null ? null : Base64.getDecoder().decode(base64Key.trim());
        } catch (IllegalArgumentException e) {
            k = null;
        }
        if (k == null || k.length != 32) {
            throw new IllegalStateException("APP_ENCRYPTION_KEY ausente ou inválida (precisa ser 32 bytes em base64)");
        }
        this.key = new SecretKeySpec(k, "AES");
    }

    @Override
    public String convertToDatabaseColumn(String plain) {
        if (plain == null) return null;
        try {
            byte[] iv = new byte[IV_BYTES];
            RANDOM.nextBytes(iv);
            Cipher c = Cipher.getInstance("AES/GCM/NoPadding");
            c.init(Cipher.ENCRYPT_MODE, key, new GCMParameterSpec(TAG_BITS, iv));
            byte[] ct = c.doFinal(plain.getBytes(StandardCharsets.UTF_8));
            byte[] out = new byte[IV_BYTES + ct.length];
            System.arraycopy(iv, 0, out, 0, IV_BYTES);
            System.arraycopy(ct, 0, out, IV_BYTES, ct.length);
            return PREFIX + Base64.getEncoder().encodeToString(out);
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("Falha ao cifrar o campo");
        }
    }

    @Override
    public String convertToEntityAttribute(String stored) {
        if (stored == null || !stored.startsWith(PREFIX)) return stored; // linha antiga, texto puro
        try {
            byte[] in = Base64.getDecoder().decode(stored.substring(PREFIX.length()));
            Cipher c = Cipher.getInstance("AES/GCM/NoPadding");
            c.init(Cipher.DECRYPT_MODE, key, new GCMParameterSpec(TAG_BITS, in, 0, IV_BYTES));
            return new String(c.doFinal(in, IV_BYTES, in.length - IV_BYTES), StandardCharsets.UTF_8);
        } catch (GeneralSecurityException | IllegalArgumentException | IndexOutOfBoundsException e) {
            // mensagem genérica de propósito: nada sobre a causa vai para o cliente
            throw new IllegalStateException("Falha ao decifrar o campo");
        }
    }
}
