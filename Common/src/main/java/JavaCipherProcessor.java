import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Base64;

/**
 * Java版本
 *
 * @author AfeiBaili
 * @version 2025/12/25 17:26
 */

public class JavaCipherProcessor {
    private Cipher cipher;

    private SecretKeySpec keySpec;

    public JavaCipherProcessor(String token) {
        try {
            byte[] hashToken = MessageDigest
                    .getInstance("sha-256")
                    .digest(token.getBytes(StandardCharsets.UTF_8));
            keySpec = new SecretKeySpec(hashToken, "AES");

            cipher = Cipher.getInstance("AES/ECB/PKCS5Padding");
        } catch (Exception exception) {
            exception.printStackTrace();
        }
    }

    public String encrypt(String plainText) {
        try {
            cipher.init(Cipher.ENCRYPT_MODE, keySpec);
            byte[] doFinal = cipher.doFinal(plainText.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(doFinal);
        } catch (Exception ignored) {
        }
        throw new RuntimeException("cipher init error");
    }

    public String decrypt(String cipherText) {
        byte[] decode = Base64.getDecoder().decode(cipherText);
        try {
            cipher.init(Cipher.DECRYPT_MODE, keySpec);
            byte[] bytes;
            try {
                bytes = cipher.doFinal(decode);
                return new String(bytes, StandardCharsets.UTF_8);
            } catch (Exception e) {
                System.out.println("解密错误");
            }
        } catch (Exception ignored) {
        }

        throw new RuntimeException("cipher init error");
    }
}
