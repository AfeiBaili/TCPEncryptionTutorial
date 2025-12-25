import kotlinx.coroutines.*
import java.io.BufferedReader
import java.io.PrintWriter
import java.net.ServerSocket
import java.net.Socket

//自定义的读取数据
class Reader(socket: Socket, scope: CoroutineScope, token: String = "thisatoken") {
    val cipher = CipherProcessor(token)
    val reader: BufferedReader = socket.getInputStream().bufferedReader()

    init {
        var line: String
        scope.launch {
            runCatching {
                while (reader.readLine().also { line = it } != null) {
                    println(":${cipher.decrypt(line)}")
                }
            }.onFailure { exception ->
                exception.printStackTrace()
                println("连接中断！${exception.message}")
            }
        }
    }

    fun close() {
        reader.close()
    }
}

//写出数据
class Writer(socket: Socket, token: String = "thisatoken") {
    val cipher = CipherProcessor(token)
    val writer = PrintWriter(socket.getOutputStream(), true)

    fun writeLine(line: String) {
        writer.println(cipher.encrypt(line))
    }

    fun close() {
        writer.close()
    }
}

//主要逻辑
class Server {
    val scope = CoroutineScope(Dispatchers.Default)
    val serverSocket = ServerSocket(33394)
    val socketMap = mutableMapOf<Socket, Pair<Reader, Writer>>()

    init {
        serverSocket.soTimeout = 1_000
    }

    fun start() {
        val job: Job = scope.launch {
            println("服务器已开启")
            while (isActive) runCatching {
                val socket: Socket = serverSocket.accept()
                socketMap[socket] = Reader(socket, scope) to Writer(socket)
                scope.launch {
                    delay(5000)
                    socketMap[socket]?.second?.writeLine("im is server")
                }
            }
        }

        runBlocking { job.join() }
    }

    fun shutdown() {
        scope.cancel()
        serverSocket.close()
        socketMap.forEach { (socket, pair) ->
            pair.second.close()
            pair.first.close()
            socket.close()
        }
        socketMap.clear()
    }
}