import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import javax.crypto.Cipher
import javax.crypto.spec.SecretKeySpec
import kotlin.io.encoding.Base64

/**
 * 加密类
 *
 *@author AfeiBaili
 *@version 2025/12/25 17:19
 */

class CipherProcessor(token: String) {
    private val hashToken =
        MessageDigest.getInstance("sha-256").digest(token.toByteArray())

    private val keySpec = SecretKeySpec(hashToken, "AES")

    private val cipher = Cipher.getInstance("AES/ECB/PKCS5Padding")

    fun encrypt(string: String): String {
        cipher.init(Cipher.ENCRYPT_MODE, keySpec)
        val bytes: ByteArray = cipher.doFinal(string.toByteArray())
        return Base64.encode(bytes)
    }

    fun decrypt(string: String): String {
        val bytes: ByteArray = Base64.decode(string)
        cipher.init(Cipher.DECRYPT_MODE, keySpec)
        val final: ByteArray = cipher.doFinal(bytes)
        return String(final, StandardCharsets.UTF_8)
    }
}