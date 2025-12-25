import kotlin.test.Test

class TestCipher {
    @Test
    fun test1() {
        val processor = JavaCipherProcessor("password")

        println(processor.decrypt(processor.encrypt("你好")))
    }
}