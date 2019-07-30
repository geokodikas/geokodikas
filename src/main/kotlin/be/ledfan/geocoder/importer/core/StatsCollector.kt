package be.ledfan.geocoder.importer.core

import java.lang.Exception
import java.text.NumberFormat
import java.util.*
import java.util.concurrent.TimeUnit
import kotlin.collections.LinkedHashMap
import kotlin.concurrent.timer

class StatsCollector {

    private var showSpinner: Boolean = false

    private var spinnerCount: Int = 0

    private val currentStats = LinkedHashMap<String, LinkedHashMap<String, String>>() // line -> (name -> statistic)

    private val numberFormat = NumberFormat.getInstance()!!

    private val GO_LINE_UP_CHAR = "\u001B[F"

    private val prefixLength = 30

    private var startTime = System.currentTimeMillis()

    private var statsTimer: Timer = timer("StatsCollector", period = 250) {
        printStats()
    }

    private var terminalWidth = 0

    init {
        try {
            val process = ProcessBuilder("/usr/bin/tput", "cols").start()
            process.inputStream.reader(Charsets.UTF_8).use {
                terminalWidth = Integer.parseInt(it.readText())
            }
            process.waitFor(200, TimeUnit.MILLISECONDS)
        } catch (e: Exception) {
            terminalWidth = 150
        }
    }

    fun updateStatistics(lineKey: String, name: String, value: Int) {
        if (!currentStats.containsKey(lineKey)) {
            currentStats[lineKey] = LinkedHashMap()
            currentStats[lineKey]?.set(name, numberFormat.format(value))
            printSingeLine(lineKey)
        } else {
            currentStats[lineKey]?.set(name, numberFormat.format(value))
        }
    }

    fun startShowingSpinner() {
        showSpinner = true
        spinnerCount = 0
    }

    fun stopShowingSpinner() {
        showSpinner = false
        spinnerCount = 0
    }


    private fun printSingeLine(lineKey: String) {
        val el = currentStats[lineKey] ?: return
        val elapsedSeconds = ((System.currentTimeMillis() - startTime) / 1000.0).toInt()
        val prefix = "$elapsedSeconds $lineKey".padEnd(prefixLength)

        print("\r" + " ".repeat(terminalWidth))
        print("\r$prefix --> ")
        for ((key, value) in el) {
            print("$key: $value  ")
        }
        println()
    }

    private fun printStats() {
        repeat(currentStats.size ) { print(GO_LINE_UP_CHAR) }
        currentStats.keys.forEach { printSingeLine(it) }

        if (showSpinner) {
            print("\r" + " ".repeat(terminalWidth))
            print("\r\t[ ")
            when (spinnerCount) {
                0 -> print("\\")
                1 -> print("|")
                2 -> print("/")
                3 -> print("-")
            }
            print(" ]")

            spinnerCount = (spinnerCount + 1) % 4
        }
    }

    fun finish() {
        statsTimer.cancel()
    }

}



