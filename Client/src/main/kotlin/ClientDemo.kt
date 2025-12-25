import java.io.PrintWriter
import java.net.Socket
import java.util.*

/**
 * 客户端样例
 *
 *@author AfeiBaili
 *@version 2025/12/23 17:08
 */

fun main() {
    val socket = Socket("localhost", 33394)
    val printWriter = PrintWriter(socket.getOutputStream().writer())
    val cipher = CipherProcessor("iamistoken")

    while (true) {
        print(":")
        val scanner = Scanner(System.`in`)
        val string: String = scanner.nextLine()
        printWriter.println(cipher.encrypt(string))
        printWriter.flush()
    }
}