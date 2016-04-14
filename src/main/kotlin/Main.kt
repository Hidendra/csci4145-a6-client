import java.net.URL
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import kotlin.system.exitProcess
import kotlin.system.measureTimeMillis

// formatting helper
fun Double.format(digits: Int) = java.lang.String.format("%.${digits}f", this)

fun main(args: Array<String>) {
    if (args.size != 7) {
        println("Usage: Main <url> <payload size (KB)> <timeout (seconds)> <simple|detailed> <num cycles> <requests per cycle>")
        exitProcess(1)
    }

    val urlPath = args[0]
    val payloadSizeKilobytes = args[2].toInt()
    val timeoutSeconds = args[3].toInt()
    val useDetailedOutput = args[4] == "detailed"
    val numCycles = args[5].toInt()
    val requestsPerCycle = args[6].toInt()

    val url = URL(urlPath)

    var totalRequests = 0
    var totalWallClockTimeTaken = 0L
    var totalTimeTaken = 0
    var totalResponseReadTime = 0
    var totalTimeouts = 0

    println("cycleNumber timeTakenMs wallClockTimeTakenMs responseReadTimeMs totalRequests requestsPerSecond totalTimeouts avgMsPerRequest avgResponseReadTimeMs")

    for (cycleNumber in 1..numCycles) {
        val executor = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors())
        val requests = arrayListOf<Request>()

        // real (wall clock) time taken is important for averages
        val wallClockTimeTaken = measureTimeMillis {
            for (requestNumber in 1..requestsPerCycle) {
                val request = Request(url, payloadSizeKilobytes, timeoutSeconds)

                requests.add(request)

                executor.submit {
                    request.run()
                }
            }

            executor.shutdown()
            executor.awaitTermination(1, TimeUnit.HOURS)
        }

        val timeTaken = requests.sumBy { it.timeTaken.toInt() }
        val responseReadTime = requests.sumBy { it.responseReadTime.toInt() }
        val numTimeouts = requests.count { it.timedOut }

        val timePerRequest = timeTaken.toDouble() / requestsPerCycle
        val responseReadTimePerRequest = responseReadTime.toDouble() / requestsPerCycle

        val requestsPerSecond = requestsPerCycle.toDouble() / (wallClockTimeTaken / 1000)

        totalRequests += requestsPerCycle
        totalWallClockTimeTaken += wallClockTimeTaken
        totalTimeTaken += timeTaken
        totalResponseReadTime += responseReadTime
        totalTimeouts += numTimeouts

        println("${cycleNumber} ${timeTaken} ${wallClockTimeTaken} ${responseReadTime} ${requestsPerCycle} ${requestsPerSecond.format(2)} ${numTimeouts} ${timePerRequest.format(2)} ${responseReadTimePerRequest.format(2)}")
    }

    val averageTimeTaken = totalTimeTaken.toDouble() / totalRequests
    val averageResponseReadTime = totalResponseReadTime.toDouble() / totalRequests

    val requestsPerSecond = totalRequests.toDouble() / (totalWallClockTimeTaken / 1000)

    println("Summary")
    println("numCycles = ${numCycles}")
    println("totalRequests = ${totalRequests}")
    println("requestsPerSecond = ${requestsPerSecond.format(2)}")
    println("totalTimeouts = ${totalTimeouts}")
    println("totalTimeTaken = ${totalTimeTaken} ms")
    println("totalWallClockTimeTaken = ${totalWallClockTimeTaken} ms")
    println("totalResponseReadTime = ${totalResponseReadTime} ms")
    println("averageTimeTaken = ${averageTimeTaken.format(2)}")
    println("averageResponseReadTime = ${averageResponseReadTime.format(2)}")
}