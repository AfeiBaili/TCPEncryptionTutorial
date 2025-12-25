import kotlinx.coroutines.*
import java.io.BufferedReader
import java.io.PrintWriter
import java.net.Socket
import java.util.*

/**
 * 客户端代码
 *
 *@author AfeiBaili
 *@version 2025/12/25 18:38
 */

class Client {
    lateinit var socket: Socket
    lateinit var reader: BufferedReader
    lateinit var writer: PrintWriter
    val scope = CoroutineScope(Dispatchers.Default)
    val cipher = CipherProcessor("thisatoken")

    fun connect() {
        socket = Socket("127.0.0.1", 33394)
        reader = socket.getInputStream().bufferedReader()
        writer = PrintWriter(socket.getOutputStream(), true)

        val job: Job = scope.launch {
            var line: String
            runCatching {
                while (reader.readLine().also { line = it } != null) runCatching {
                    println(";${cipher.decrypt(line)}")
                }.onFailure {
                    println("token不正确或者消息被窜改")
                }
            }.onFailure {
                println("连接中断")
            }
        }

        scope.launch {
            val scanner = Scanner(System.`in`)
            while (isActive) {
                print(":")
                val line: String = scanner.nextLine()
                writer.println(cipher.encrypt(line))
            }
        }

        runBlocking { job.join() }
    }

    fun close() {
        runCatching {
            reader.close()
            writer.close()
            scope.cancel()
            socket.close()
        }
    }
}

fun main() {
    val client = Client()
    client.connect()
}