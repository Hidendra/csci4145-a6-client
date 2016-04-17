import java.net.URL
import kotlin.system.measureTimeMillis

class Request(val url: URL, val requestId: Int, val payloadSizeKilobytes: Int, val timeoutSeconds: Int, val apiToken: String): Runnable {

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

                connection.addRequestProperty("Content-Type", "application/json")
                connection.addRequestProperty("Authorization", "Token: ${apiToken}")

                connection.outputStream.use {
                    val body = "{\"cloud_computing\": {\"id\": ${requestId}, \"payload_size\": ${payloadSizeKilobytes}}}";

                    it.write(body.toByteArray(Charsets.UTF_8))
                }

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