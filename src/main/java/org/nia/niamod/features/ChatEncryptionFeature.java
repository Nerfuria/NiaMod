package org.nia.niamod.features;

import net.fabricmc.fabric.api.client.message.v1.ClientSendMessageEvents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import org.nia.niamod.config.NyahConfig;
import org.nia.niamod.eventbus.NiaEventBus;
import org.nia.niamod.eventbus.Subscribe;
import org.nia.niamod.models.events.ChatModifyEvent;
import org.nia.niamod.models.events.CommandSentEvent;

import javax.crypto.AEADBadTagException;
import javax.crypto.Cipher;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.security.spec.KeySpec;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@SuppressWarnings("unused")
public class ChatEncryptionFeature extends Feature {
    private static final int AAD_VALUE = 67;
    private static final int GCM_TAG_LENGTH = 128;
    private static final int GCM_NONCE_LENGTH = 12;
    private static final int GCM_TAG_LENGTH_BYTES = GCM_TAG_LENGTH / 8;
    private static final String CIPHER_ALGO = "AES/GCM/NoPadding";
    private static final String KEY_ALGO = "AES";
    private static final String KEY_DERIVATION_ALGO = "PBKDF2WithHmacSHA256";
    private static final String MSG_START = "£\uDB8D\uDE37-";
    private static final String MSG_END = "-\uDB8D\uDE37$";
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private static String encode(byte[] bytes) {
        StringBuilder builder = new StringBuilder(bytes.length / 2 + bytes.length % 2);
        for (int i = 0; i < bytes.length - 1; i += 2) {
            int b1 = bytes[i] & 0xFF;
            int b2 = bytes[i + 1] & 0xFF;
            int codePoint = (b1 == 255 && b2 >= 254) ? 1048576 + (b2 - 254) : 983040 + (b1 << 8 | b2);
            builder.appendCodePoint(codePoint);
        }
        if (bytes.length % 2 == 1) {
            builder.appendCodePoint(1048576 + ((bytes[bytes.length - 1] & 0xFF) << 8) + 238);
        }
        return builder.toString();
    }

    private static byte[] decode(String string) {
        List<Byte> byteList = new ArrayList<>();
        for (int cp : string.codePoints().toArray()) {
            if (cp >= 1048576) {
                if ((cp & 255) == 238) {
                    byteList.add((byte) ((cp - 1048576) >> 8));
                } else {
                    byteList.add((byte) 255);
                    byteList.add((byte) ((cp - 1048576) + 2));
                }
            } else {
                int val = cp - 983040;
                byteList.add((byte) (val >> 8));
                byteList.add((byte) (val & 0xFF));
            }
        }
        byte[] result = new byte[byteList.size()];
        for (int i = 0; i < byteList.size(); i++) {
            result[i] = byteList.get(i);
        }
        return result;
    }

    @Override
    public void init() {
        ClientSendMessageEvents.MODIFY_CHAT.register(
                message -> callSafe("processMessage", () -> processMessage(message), message));
        NiaEventBus.subscribe(this);
    }

    private byte[] encryptionKey() {
        try {
            String password = NyahConfig.getData().getEncryptionKey();
            byte[] salt = new byte[16];
            KeySpec spec = new PBEKeySpec(password.toCharArray(), salt, 100000, 256);
            return SecretKeyFactory.getInstance(KEY_DERIVATION_ALGO).generateSecret(spec).getEncoded();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private GCMParameterSpec gcmSpec(byte[] iv) {
        if (iv.length != GCM_NONCE_LENGTH) {
            throw new IllegalArgumentException("Invalid GCM nonce length");
        }
        return new GCMParameterSpec(GCM_TAG_LENGTH, iv);
    }

    private Cipher initCipher(int mode, byte[] iv) {
        try {
            Cipher cipher = Cipher.getInstance(CIPHER_ALGO);
            cipher.init(mode, new SecretKeySpec(encryptionKey(), KEY_ALGO), gcmSpec(iv));
            cipher.updateAAD(ByteBuffer.allocate(4).putInt(AAD_VALUE).array());
            return cipher;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private String encryptMessage(String message) {
        try {
            byte[] iv = new byte[GCM_NONCE_LENGTH];
            SECURE_RANDOM.nextBytes(iv);
            byte[] encrypted =
                    initCipher(Cipher.ENCRYPT_MODE, iv)
                            .doFinal(message.trim().getBytes(StandardCharsets.UTF_8));
            return MSG_START + encode(iv) + encode(encrypted) + MSG_END;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private String decrypt(String message) throws AEADBadTagException {
        try {
            byte[] bytes = decode(message);
            if (bytes.length < GCM_NONCE_LENGTH + GCM_TAG_LENGTH_BYTES) {
                return message;
            }
            byte[] iv = java.util.Arrays.copyOfRange(bytes, 0, GCM_NONCE_LENGTH);
            byte[] decrypted =
                    initCipher(Cipher.DECRYPT_MODE, iv)
                            .doFinal(bytes, GCM_NONCE_LENGTH, bytes.length - GCM_NONCE_LENGTH);
            return new String(decrypted, StandardCharsets.UTF_8);
        } catch (AEADBadTagException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public String processMessage(String message) {
        String prefix = NyahConfig.getData().getEncryptionPrefix();
        int idx = message.indexOf(prefix);
        if (idx == -1) {
            return message;
        }
        return message.substring(0, idx) + encryptMessage(message.substring(idx + prefix.length()));
    }

    @Subscribe
    public void commandSentEvent(CommandSentEvent event) {
        event.setCommand(processMessage(event.command()));
    }

    public String decodeMessage(String message) {
        int start = message.indexOf(MSG_START);
        int end = message.indexOf(MSG_END);
        if (start == -1 || end == -1) {
            return message;
        }
        try {
            return decrypt(message.substring(start + MSG_START.length(), end));
        } catch (AEADBadTagException e) {
            return message;
        }
    }

    @Subscribe
    public void modifyChat(ChatModifyEvent event) {
        Component text = event.getMessage();
        MutableComponent copy = Component.empty();
        text.visit(
                (style, string) -> {
                    String decoded = decodeMessage(string);
                    if (!decoded.equals(string)) {
                        copy.append(
                                Component.literal(string.substring(0, string.indexOf(MSG_START))).withStyle(style));
                        style =
                                style
                                        .withHoverEvent(
                                                new HoverEvent.ShowText(
                                                        Component.nullToEmpty("This message was encoded with NiaMod")))
                                        .withUnderlined(true);
                    }
                    copy.append(Component.literal(decoded).withStyle(style));
                    return Optional.empty();
                },
                Style.EMPTY);
        event.setMessage(copy);
    }
}
