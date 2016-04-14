import java.net.URL
import kotlin.system.measureTimeMillis

class Request(val url: URL, val payloadSizeKilobytes: Int, val timeoutSeconds: Int): Runnable {

    var timeTaken = 0L
    var responseReadTime = 0L
    var timedOut = false

    override fun run() {
        timeTaken = measureTimeMillis {
            try {
                var connection = url.openConnection()

                connection.connectTimeout = timeoutSeconds * 1000
                connection.readTimeout = timeoutSeconds * 1000

                connection.doOutput = true

                responseReadTime = measureTimeMillis {
                    connection.inputStream.use {
                        // Read the stream fully
                        while (it.available() > 0) {
                            it.read()
                        }
                    }
                }
            } catch (e: Exception) {
                timedOut = true
            }
        }
    }

}