# [Java/Kotlin] TCP/Socket 加密通讯教程

在普通Socket传输信息中使用 Wireshark 抓取数据包查看，发现内容是明文传输的。  
虽然在Java中有加密的Socket，但是我还是想讲解一下手动加密数据的Socket

## 编写加密类

编写一个加密类必不可少的就是密钥或者叫它 **token** token可以是从外部读取来的，  
也可以内部给它设定死，虽然但是还是建议不要设定死。我为了剩下编写配置文件的代码我就设定死token值了

1. 通过 `MessageDigest` 拿到哈希计算后的值，为接下来的key准备
2. 通过 `SecretKeySpec` 拿到密钥也就是Key
3. 有了密钥然后通过 `Cipher` 拿到加密和解密的对象了。为了对象的重用我就用cipher对象了
4. 接下来就是通过 cipher 对象编写加密和解密方法

### Kotlin 版本

```kotlin
class CipherProcessor(token: String) {
    //哈希计算拿到token32位字符
    private val hashString: ByteArray = MessageDigest.getInstance("SHA-256").digest(token.toByteArray())

    //拿到哈希后的token用来获取密钥，使用AES加密算法
    private val keySpec = SecretKeySpec(hashString, "AES")

    //实例化cipher类
    private val cipher: Cipher = Cipher.getInstance("AES/ECB/PKCS5Padding")

    //加密字符串方法
    fun encrypt(string: String): String {
        //将keySpec传入用于给cipher加密
        cipher.init(Cipher.ENCRYPT_MODE, keySpec)
        //做加密，需要注意的是kotlin中toByteArray是默认使用UTF-8的
        //拿到加密的字节
        val final: ByteArray = cipher.doFinal(string.toByteArray())
        //通过base64将字节转成字符
        return Base64.encode(final)
    }

    //解密字符串方法
    fun decrypt(string: String): String {
        //将base64转成字节
        val bytes: ByteArray = Base64.decode(string)
        //将cipher作为解密使用
        cipher.init(Cipher.DECRYPT_MODE, keySpec)
        //将字节进行解密
        val final: ByteArray = cipher.doFinal(bytes)
        //将解密的字节转为字符串，并指定UTF-8
        return String(final, StandardCharsets.UTF_8)
    }
}
```

### Java 版本

```java
class JavaCipherProcessor {
    //keySpec 属性供 cipher 用
    private final SecretKeySpec keySpec;
    //用于加密和解密的属性
    private final Cipher cipher;

    //将 token 传入初始化keySpec和cipher
    public JavaCipherProcessor(String token) throws NoSuchAlgorithmException, NoSuchPaddingException {
        //获取哈希后的 token
        byte[] sha = MessageDigest.getInstance("SHA-256").digest(token.getBytes(StandardCharsets.UTF_8));
        //创建 keySpec
        keySpec = new SecretKeySpec(sha, "AES");
        //创建 cipher 使用ECB算法
        cipher = Cipher.getInstance("AES/ECB/PKCS5Padding");
    }

    //加密方法，用于加密字符串
    public String encrypt(String string) throws InvalidKeyException, IllegalBlockSizeException, BadPaddingException {
        //将 cipher 设为加密模式
        cipher.init(Cipher.ENCRYPT_MODE, keySpec);
        //将字符串转为 UTF-8 并进行加密
        byte[] doFinal = cipher.doFinal(string.getBytes(StandardCharsets.UTF_8));
        //将加密好的字节数组转为 Base64
        return Base64.getEncoder().encodeToString(doFinal);
    }

    //解密方法，用于解密传进来的Base64
    public String decrypt(String encryptedString) throws InvalidKeyException, IllegalBlockSizeException, BadPaddingException {
        //将 cipher 设为解密模式
        cipher.init(Cipher.DECRYPT_MODE, keySpec);
        //将Base64转为 加密后的字节
        byte[] decode = Base64.getDecoder().decode(encryptedString);
        //解密加密后的字节
        byte[] doFinal = cipher.doFinal(decode);
        //将解密后的字节重新转为字符串
        return new String(doFinal, StandardCharsets.UTF_8);
    }
}
```

## 服务端代码

加密的问题已经解决，就该解决数据传输的问题了。编写服务端代码前先思考服务端都需要做什么

1. 一个一直循环的接收系统
2. 一个管理 Socket 的集合
3. 一个输入模块（Reader），一个输出模块（Writer）

> 实际的项目中，需要考虑的事情更多，这里只是给一个比较详细的思路

在创建服务端前需要先把两个前置类给写出来  
分别为：Reader、Writer  
一个用于管理接收进来的消息，一个负责输出消息

## Reader 编写

一个读取器对象，不可缺少的就是一个socket了  
还需要一个线程来不断读取传入进来的信息  
还有一个关闭资源的方法

> 传入进来的信息需要进行解密

### Kotlin 版本

```kotlin
//接收一个 socket 和 一个scope（可以理解为小线程）
class Reader(token: String, socket: Socket, scope: CoroutineScope) {
    //拿到reader
    val reader = socket.getInputStream().bufferedReader()

    //创建CipherProcessor对象
    private val cipher = CipherProcessor(token)

    //开启读取线程，读取每一行
    val job = scope.launch {
        //包裹readLine防止报错
        runCatching {
            //用于存储传入的数据
            var line: String
            //只要接收到一行数据就进行打印
            while (reader.readLine().also { line = it } != null) {
                //如果 token 不对或者信息被篡改就报错，使用 try-catch 进行捕捉
                runCatching {
                    //解密并打印数据
                    println("$socket: ${cipher.decrypt(line)}")
                }.onFailure { exception ->
                    println("解密时出错：${exception.message}")
                }
            }
        }.onFailure { exception ->
            //如果连接断开
            println("断开连接：${exception.message}")
        }
    }

    //关闭资源，并捕捉错误防止报错
    fun close() = runCatching {
        reader.close()
    }
}
```

### Java 版本

```java
public class JavaReader {
    //定义 cipher 对象
    private JavaCipherProcessor javaCipherProcessor;

    //定义 Reader 对象
    BufferedReader bufferedReader;

    //构造方法
    public JavaReader(String token, Socket socket) {
        try {
            //赋值 reader 对象
            bufferedReader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            //赋值 cipher 对象
            javaCipherProcessor = new JavaCipherProcessor(token);
        } catch (Exception ignore) {
            //忽略异常
        }

        //开启一个线程用来持续读取接收到的信息
        Thread thread = new Thread(() -> {
            //定义行属性
            String line;
            //循环读取接收到的内容，并捕捉断开连接异常
            try {
                //循环读取
                while ((line = bufferedReader.readLine()) != null) {
                    //解密并打印接收到的信息
                    System.out.println(socket.toString() + ":" + javaCipherProcessor.decrypt(line));
                }
            } catch (Exception ignore) {
                System.out.println("连接断开");
            }
        });
        //开启线程
        thread.start();
    }

    //关闭方法，用于关闭资源
    public void close() {
        try {
            //关闭 reader
            bufferedReader.close();
        } catch (IOException ignore) {
            //忽略异常
        }
    }
}
```

## Writer 编写

编写 writer 就比较简单了，只需要一个写入方法和关闭方法就可以了

> 还有解密

### Kotlin 版本

```kotlin
//因为写入数据不需要一个单独的线程，所以用不到scope
class Writer(token: String, socket: Socket) {
    //拿到 writer 对象
    val writer = PrintWriter(socket.getOutputStream(), true)

    //获取自己写的加密类
    private val cipher = CipherProcessor(token)

    //声名写出的方法
    fun writeLine(line: String) {
        //如果流关闭，捕捉异常对象
        runCatching {
            //写入加密后的数据
            writer.println(cipher.encrypt(line))
        }.onFailure {
            println("已断开连接")
        }
    }

    //关闭资源
    fun close() {
        writer.close()
    }
}
```

### Java 版本

```java
public class JavaWriter {

    //定义 writer 对象
    PrintWriter writer;

    //定义 cipher 对象
    JavaCipherProcessor javaCipherProcessor;

    public JavaWriter(String token, Socket socket) {
        try {
            //初始化 cipher 和 writer
            writer = new PrintWriter(new OutputStreamWriter(socket.getOutputStream()));
            javaCipherProcessor = new JavaCipherProcessor(token);
        } catch (Exception ignore) {
            //忽略异常
        }
    }

    //编写写入数据方法
    public void writeLine(String line) {
        try {
            //写入加密后的数据
            writer.println(javaCipherProcessor.encrypt(line));
        } catch (Exception ignore) {
            System.out.println("已经断开连接");
        }
    }

    //关闭流的方法
    public void close() {
        writer.close();
    }
}
```

> 现在已经编写完毕前置类了，接下来就是主要的服务端类了

## 主要类编写

一个服务端管理类，需要一个常驻的线程来源源不断接收客户端的请求  
还需要一个socket集合来存储socket  
当然还有token用于加密和解密用，这里方便直接写死了token，实际项目中可能需要外部引入

### Kotlin 版本

```kotlin
class Server {
    //这里用到了 Kotlin 的协程来作为小线程使用
    val scope = CoroutineScope(Dispatchers.Default)

    //一个 hashmap 用于管理Socket，传入的是自定义的Reader和Write
    val socketMap = mutableMapOf<Socket, Pair<Reader, Writer>>()

    //这里把 token 的值写死了
    val token = "iamistoken"

    //开启端口为33394的服务端
    val serverSocket = ServerSocket(33394)

    //一个开启服务器的方法
    fun start() {
        val job: Job = scope.launch {
            println("服务器已开启")
            //设置连接的超时时间
            serverSocket.soTimeout = 10_000
            //只要不关闭就一直循环下去（isActive是协程的属性）
            while (isActive) runCatching {
                //接收客户端的Socket
                val socket: Socket = serverSocket.accept()
                //将客户端存入map中
                socketMap[socket] = Reader(token, socket, scope) to Writer(token, socket)
            }
        }

        //阻塞主线程
        runBlocking { job.join() }
    }

    fun shutdown() {
        //关闭协程
        scope.cancel()
        //断开所有客户端
        socketMap.forEach { (socket, pair) ->
            pair.first.close()
            pair.second.close()
            socket.close()
        }
        //关闭服务器连接
        serverSocket.close()
        //清理map
        socketMap.clear()
    }
}
```

### Java 版本

```java
public class JavaServer {
    //写死 token
    private String token = "iamistoken";

    //定义一个二元组类，用于map使用
    static class Tuple<A, B> {
        //定义两个属性，用于存放两个值
        A first;
        B second;

        //构造方法
        public Tuple(A first, B second) {
            this.first = first;
            this.second = second;
        }
    }

    //一个用于存储 socket 的 map
    private final HashMap<Socket, Tuple<JavaReader, JavaWriter>> socketMap = new HashMap<>();

    //服务端 socket 对象
    private ServerSocket serverSocket;

    //服务端线程
    private Thread serverThread;

    //开启服务端的方法
    public void start() throws InterruptedException, IOException {
        //赋值服务端 socket 对象
        serverSocket = new ServerSocket(33394);
        //设置超时
        serverSocket.setSoTimeout(10000);

        //开启一个线程来不断接收客户端请求
        serverThread = new Thread("Server Thread") {

            //重写线程方法
            @Override
            public void run() {
                System.out.println("服务器已开启");
                //循环接收客户端对象
                while (!super.isInterrupted()) {
                    //如果超时可以进行 try-catch 捕捉，防止报错
                    try {
                        //接收客户端连接
                        Socket socket = serverSocket.accept();
                        //将客户端信息存入 map
                        socketMap.put(socket, new Tuple<>(new JavaReader(token, socket), new JavaWriter(token, socket)));
                    } catch (Exception ignored) {
                        //如果超时就忽略
                    }
                }
            }
        };

        //开启线程
        serverThread.start();
        //阻塞线程
        serverThread.join();
    }

    public void shutdown() {
        //中断线程
        serverThread.interrupt();
        //断开所有 socket 连接
        socketMap.forEach((Socket socket, Tuple<JavaReader, JavaWriter> tuple) -> {
            tuple.first.close();
            tuple.second.close();
            try {
                socket.close();
            } catch (IOException ignore) {
                //忽略报错
            }
        });
        //清理 map
        socketMap.clear();
    }
}
```

## 客户端代码

客户端做的事情相比服务端来说比较简单  
客户端只需要读取和写入  
在实际的项目中还需要一个心跳线程来源源不断的发送心跳包  
我为了方便就不写了

### Kotlin

```kotlin
class Client {
    //token需要和服务端的token一致
    private val token = "iamistoken"

    //声名 socket
    private lateinit var socket: Socket

    //reader 小线程
    private lateinit var readerJob: Job

    //writer 小线程
    private lateinit var writerJob: Job

    //读取器
    private lateinit var reader: BufferedReader

    //写入器
    private lateinit var writer: PrintWriter

    //获取cipher
    private val cipher = CipherProcessor(token)

    //开启一个scope来管理线程
    private val scope = CoroutineScope(Dispatchers.Default)

    //创建scanner，模拟信息获取
    private lateinit var scanner: Scanner

    fun connect() {
        //建立socket连接
        socket = Socket("localhost", 33394)

        //获取reader和writer
        reader = socket.getInputStream().bufferedReader()
        writer = PrintWriter(socket.getOutputStream(), true)

        //拿到连接分辨编写读取器和写入器，这里读取器直接用scope管理，写入用Scanner类

        //获取读取器，让读取器一直读取流信息
        readerJob = scope.launch {
            var line: String
            //捕捉关闭流异常
            runCatching {
                //捕捉解密失败的异常
                while (reader.readLine().also { line = it } != null) runCatching {
                    //解密并打印信息
                    println("Server: ${cipher.decrypt(line)}")
                }.onFailure { exception ->
                    println("解密失败：${exception.message}")
                }
            }
        }

        //编写写入器程序，让Scanner模拟输入信息传入服务器
        writerJob = scope.launch {
            //创建scanner对象
            scanner = Scanner(System.`in`)
            //只要不关闭就一直循环
            while (isActive) {
                print(":")
                //获取一行信息
                val line: String = scanner.nextLine()
                //加密并发送信息
                writer.println(cipher.encrypt(line))
            }
        }

        println("已打开连接")

        //阻塞线程
        runBlocking {
            readerJob.join()
            writerJob.join()
        }
    }

    //关闭资源
    fun close() {
        //防止捕捉时出错
        runCatching {
            socket.close()
            writer.close()
            reader.close()
            scanner.close()
            scope.cancel()
        }
    }
}
```

### Java

```java
public class JavaClient {
    //写死 token
    private String token = "iamistoken";

    //声明读取线程
    private Thread readerThread;

    //声明模拟写入线程
    private Thread writerThread;

    //模拟信息写入
    private Scanner scanner = new Scanner(System.in);

    //客户端 socket
    private Socket socket;

    //reader
    private BufferedReader reader;

    //writer
    private PrintWriter writer;

    //获取加密对象
    private JavaCipherProcessor cipherProcessor;

    //代码块，给 cipher 赋值用
    {
        try {
            //创建 cipher 对象
            cipherProcessor = new JavaCipherProcessor(token);
        } catch (Exception ignored) {
        }
    }

    //开启连接的方法
    public void connect() {
        //连接服务器并忽略异常
        try {
            socket = new Socket("localhost", 33394);

            //获取 reader和writer
            reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            writer = new PrintWriter(socket.getOutputStream(), true);

            //创建读取器线程，一直读取服务器传入的信息
            readerThread = new Thread(() -> {
                String line;
                //捕捉异常，防止报错
                try {
                    while ((line = reader.readLine()) != null) {
                        //解密并打印信息
                        System.out.println(cipherProcessor.decrypt(line));
                    }
                } catch (Exception ignored) {
                }
            });

            //创建写入器线程，模拟信息写入
            writerThread = new Thread(() -> {
                try {
                    //只要不关闭就一直循环
                    while (!writerThread.isInterrupted()) {
                        System.out.print(":");
                        //读取一行
                        String line = scanner.nextLine();
                        //加密并发送信息
                        writer.println(cipherProcessor.encrypt(line));
                    }
                } catch (Exception ignored) {
                }
            });

            //线程开启
            readerThread.start();
            writerThread.start();

            System.out.println("已打开连接");

            //阻塞线程
            readerThread.join();
            writerThread.join();
        } catch (Exception ignored) {
        }
    }

    //关闭连接的方法
    public void close() {
        //关闭资源，并忽略报错0
        try {
            writer.close();
            reader.close();
            socket.close();
            writerThread.interrupt();
            scanner.close();
        } catch (Exception ignored) {
        }
    }
}
```