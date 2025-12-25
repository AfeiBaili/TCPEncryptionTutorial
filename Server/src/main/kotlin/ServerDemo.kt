import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.BufferedReader
import java.net.ServerSocket
import java.net.Socket

/**
 * 不加密的Socket服务器
 *
 *@author AfeiBaili
 *@version 2025/12/23 16:57
 */

fun main() {
    val serverSocket = ServerSocket(33394)
    val scope = CoroutineScope(Dispatchers.Default)

    while (true) {
        val socket: Socket = serverSocket.accept()

        scope.launch {
            val reader: BufferedReader = socket.getInputStream().bufferedReader()

            var line: String
            println("...")
            runCatching {
                while (reader.readLine().also { line = it } != null) {
                    println(line)
                }
            }
        }
    }
}