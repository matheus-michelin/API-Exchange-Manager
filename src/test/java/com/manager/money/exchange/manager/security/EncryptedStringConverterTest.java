package com.manager.money.exchange.manager.security;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.Base64;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class EncryptedStringConverterTest {

    private static final String KEY = "Y2hhdmUtc29tZW50ZS1wYXJhLXRlc3Rlcy0xMjM0NTY=";
    private static final String OTHER_KEY = Base64.getEncoder().encodeToString(new byte[32]);

    private final EncryptedStringConverter converter = new EncryptedStringConverter(KEY);

    @Test
    void roundTripPreservesAccentsAndEmoji() {
        var plain = "Almoço no restaurante 🍽️";

        var stored = converter.convertToDatabaseColumn(plain);

        assertThat(stored).startsWith("v1:").doesNotContain("Almoço");
        assertThat(converter.convertToEntityAttribute(stored)).isEqualTo(plain);
    }

    @Test
    void sameInputProducesDifferentCiphertext() {
        assertThat(converter.convertToDatabaseColumn("deposit"))
                .isNotEqualTo(converter.convertToDatabaseColumn("deposit"));
    }

    @Test
    void nullStaysNull() {
        assertThat(converter.convertToDatabaseColumn(null)).isNull();
        assertThat(converter.convertToEntityAttribute(null)).isNull();
    }

    @Test
    void legacyPlainTextIsReturnedAsIs() {
        assertThat(converter.convertToEntityAttribute("texto antigo")).isEqualTo("texto antigo");
    }

    @Test
    void worstCaseDescriptionFitsTheVarchar600Column() {
        var stored = converter.convertToDatabaseColumn("€".repeat(85)); // 3 bytes por caractere

        assertThat(stored.length()).isLessThanOrEqualTo(600);
    }

    @Test
    void tamperedCiphertextIsRejected() {
        var stored = converter.convertToDatabaseColumn("deposit");
        var bytes = Base64.getDecoder().decode(stored.substring(3));
        bytes[bytes.length - 1] ^= 1;
        var tampered = "v1:" + Base64.getEncoder().encodeToString(bytes);

        assertThatThrownBy(() -> converter.convertToEntityAttribute(tampered))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Falha ao decifrar o campo");
    }

    @Test
    void wrongKeyIsRejected() {
        var stored = converter.convertToDatabaseColumn("deposit");

        assertThatThrownBy(() -> new EncryptedStringConverter(OTHER_KEY).convertToEntityAttribute(stored))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Falha ao decifrar o campo");
    }

    @ParameterizedTest
    @ValueSource(strings = {"v1:AAAA", "v1:%%%"})
    void truncatedOrInvalidBase64IsRejected(String stored) {
        assertThatThrownBy(() -> converter.convertToEntityAttribute(stored))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Falha ao decifrar o campo");
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"***", "c2hvcnQ="})
    void invalidKeyIsRejected(String key) {
        assertThatThrownBy(() -> new EncryptedStringConverter(key))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("APP_ENCRYPTION_KEY");
    }

    @Test
    void keyWithSurroundingWhitespaceIsAccepted() {
        var stored = converter.convertToDatabaseColumn("deposit");

        assertThat(new EncryptedStringConverter(KEY + "\n").convertToEntityAttribute(stored)).isEqualTo("deposit");
    }
}
