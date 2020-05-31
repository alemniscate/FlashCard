package flashcards

import java.io.File

fun main(args: Array<String>) {
    var importFileName = ""
    var exportFileName = ""
    for (i in args.indices) {
        if (args[i] == "-import") {
            importFileName = args[i + 1]
        }

        if (args[i] == "-export") {
            exportFileName = args[i + 1]
        }
    }

    val log = Log()
    val act = FlashCardAction(log)
    val fcs = FlashCardList()

    if (importFileName != "") {
        val n = fcs.import(importFileName, fcs)
        if (n == -1) {
            log.println("File not found.")
        } else {
            log.println("$n cards have been loaded.")
        }
    }

    log.println("Input the action (add, remove, import, export, ask, exit, log.println, hardest card, reset stats):")
    var action = log.readLine()!!

    while (true) {
        when (action) {
            "add" -> act.add(fcs)
            "remove" -> act.remove(fcs)
            "import" -> act.import(fcs)
            "export" -> act.export(fcs)
            "ask" -> act.ask(fcs)
            "exit" -> act.exit()
            "log" -> act.log()
            "hardest card" -> act.hardestCard(fcs)
            "reset stats" -> act.resetStats(fcs)
        }
        if (action == "exit") {
            break
        }
        log.println()
        log.println("Input the action (add, remove, import, export, ask, exit, log.println, hardest card, reset stats):")
        action = log.readLine()!!
    }

    if (exportFileName != "") {
        val n = fcs.export(exportFileName)
        log.println("$n cards have been saved.")
    }
}

data class HardestCard(val errorcount: Int, val list: MutableList<String>)

class FlashCardAction(val log: Log) {
    fun add(fcs: FlashCardList) {
        log.println("The card:")
        val term = log.readLine()!!
        if (fcs.searchCard(term) != null) {
            log.println("The card \"$term\" already exists.")
            return
        }
        log.println("The definition of the card:")
        val definition = log.readLine()!!
        if (fcs.searchDefinition(definition) != null) {
            log.println("The definition \"$definition\" already exists.")
            return
        }
        val fc = FlashCard(term, definition, 0)
        fcs.add(fc)
        log.println("The pair (\"${fc.term}\":\"${fc.definition}\") has been added.")
    }

    fun remove(fcs: FlashCardList) {
        log.println("The card:")
        val term = log.readLine()!!
        val fc = fcs.searchCard(term)
        if (fc == null) {
            log.println("Can't remove \"$term\": there is no such card.")
            return
        }
        fcs.remove(fc)
        log.println("The card has been removed.")
    }

    fun import(fcs: FlashCardList) {
        log.println("File name:")
        val filename = log.readLine()!!
        val n = fcs.import(filename, fcs)
        if (n == -1) {
            log.println("File not found.")
        } else {
            log.println("$n cards have been loaded.")
        }
    }

    fun export(fcs: FlashCardList) {
        log.println("File name:")
        val filename = log.readLine()!!
        val n = fcs.export(filename)
        log.println("$n cards have been saved.")
    }

    fun ask(fcs: FlashCardList) {
        val random = kotlin.random.Random(System.currentTimeMillis())
        log.println("How many times to ask?")
        val n = log.readLine()!!.toInt()
        for (i in 0..n - 1) {
            val r = random.nextInt(0, fcs.getsize())
            val fc = fcs.get(r)
            log.println("Print the definition of \"${fc.term}\":")
            val definition = log.readLine()!!
            if (definition == fc.definition) {
                log.println("Correct answer.")
            } else {
                val fc2 = fcs.searchDefinition(definition)
                if (fc2 == null) {
                    log.println("Wrong answer. The correct one is \"${fc.definition}\".")
                } else {
                    log.println("Wrong answer. (The correct one is \"${fc.definition}\", you've just written the definition of \"${fc2.term}\" card. ")
                }
                fc.mistakes++
                fcs.update(r, fc)
            }
        }
    }

    fun exit() {
        log.println("Bye bye!")
    }

    fun log() {
        log.println("File name:")
        val filename = log.readLine()!!
        val n = log.export(filename)
        log.println("The log has been saved.")
    }

    fun hardestCard(fcs: FlashCardList) {
        val hardest = fcs.getHardestCard()
        when (hardest.list.size) {
            0 -> {
                log.println("There are no cards with errors.")
            }
            1 -> {
                log.println("The hardest card is \"${hardest.list[0]}\". You have ${hardest.errorcount} errors answering it.")
            }
            else -> {
                var terms = ""
                for (term in hardest.list) {
                    if (terms != "") terms += ", "
                    terms += "\"$term\""
                }
                log.println("The hardest cards are $terms. You have ${hardest.errorcount} errors answering them.")
            }
        }
    }

    fun resetStats(fcs: FlashCardList) {
        fcs.resetStats()
        log.println("Card statistics has been reset.")
    }
}

data class FlashCard(val term: String, val definition: String, var mistakes: Int)

class FlashCardList {
    val fclist = mutableListOf<FlashCard>()

    fun add(fc: FlashCard) {
        fclist.add(fc)
    }

    fun remove(fc: FlashCard) {
        fclist.remove(fc)
    }

    fun update(index: Int, fc: FlashCard) {
        fclist[index] = fc
    }

    fun export(filename: String): Int {
        val file = File(filename)
        var text = ""
        for (fc in fclist) {
            text += fc.term + "," + fc.definition + "," + fc.mistakes + "\n"
        }
        file.writeText(text)
        return fclist.size
    }

    fun import(filename: String, fcs: FlashCardList): Int {
        val file = File(filename)
        if (!file.exists()) {
            return -1
        }
        val lines = file.readLines()
        for (line in lines) {
            val strs = line.split(",")
            val term = strs[0]
            val definition = strs[1]
            var mistakes = 0
            if (strs.size > 2) mistakes = strs[2].toInt()
            val fc = FlashCard(term, definition, mistakes)
            val i = searchCardIndex(fc.term)
            if (i == -1) {
                fclist.add(fc)
            } else {
                fclist[i] = fc
            }
        }
        return lines.size
    }

    fun get(index: Int): FlashCard {
        return fclist[index]
    }

    fun getsize(): Int {
        return fclist.size
    }

    fun searchCard(term: String): FlashCard? {
        for (fc in fclist) {
            if (fc.term == term) {
                return fc
            }
        }
        return null
    }

    fun searchCardIndex(term: String): Int {
        for (i in fclist.indices) {
            if (fclist[i].term == term) {
                return i
            }
        }
        return -1
    }

    fun searchDefinition(definition: String): FlashCard? {
        for (fc in fclist) {
            if (fc.definition == definition) {
                return fc
            }
        }
        return null
    }

    fun getHardestCard(): HardestCard {
        val list = mutableListOf<String>()
        var max = 0
        for (fc in fclist) {
            if (fc.mistakes > max) {
                max = fc.mistakes
                list.clear()
                list.add(fc.term)
                continue
            }

            if (fc.mistakes == max) {
                list.add(fc.term)
            }
        }
        if (max == 0) {
            list.clear()
        }
        val hardest = HardestCard(max, list)
        return hardest
    }

    fun resetStats() {
        for (fc in fclist) {
            fc.mistakes = 0
        }
    }
}

class Log {
    val logs = mutableListOf<String>()

    fun println() {
        kotlin.io.println()
        logs.add("")
    }

    fun println(text: String) {
        kotlin.io.println(text)
        logs.add(text)
    }

    fun readLine(): String? {
        val input = kotlin.io.readLine()!!
        logs.add(input)
        return input
    }

    fun export(filename: String) {
        val file = File(filename)
        var texts = ""
        for (text in logs) {
            texts += text + "\n"
        }
        file.writeText(texts)
    }
}