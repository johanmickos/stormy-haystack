package kvstore

import java.io.PrintWriter

import org.apache.log4j.{LogManager, Logger, PatternLayout, WriterAppender}
import org.jline.reader.{LineReader, LineReaderBuilder}
import org.jline.terminal.{Terminal, TerminalBuilder}
import stormy.kv.OperationResponse

import scala.collection.mutable

object Console {
    private final val PROMPT: String = "> "
}

class Console(service: ClientService) extends Runnable {
    val exitCommand: Command = new Command() {
        override def execute(cmdline: Array[String], worker: ClientService): Boolean = {
            out.get.println("Exiting...")
            System.exit(0)
            true
        }

        override def usage: String = "exit|quit"

        override def help: String = "Closes the shell"
    }
    private val commands = new mutable.HashMap[String, Command]
    var longestCom: Int = 0

    commands.put("get", new Command() {
        override def execute(cmdline: Array[String], worker: ClientService): Boolean = {
            if (cmdline.length == 2) {
                val fr = worker.op(cmdline)
                val resp: OperationResponse = fr.get()
                out.get.println(s"[${resp.status}] ${resp.content.getOrElse("")}")
                true
            } else {
                false
            }
        }

        override def usage: String = "get <key>"

        override def help: String = "Attempts to recover the value held at <key?"
    })
    commands.put("put", new Command() {
        override def execute(cmdline: Array[String], worker: ClientService): Boolean = {
            if (cmdline.length == 3) {
                val fr = worker.op(cmdline)
                val resp: OperationResponse = fr.get()
                out.get.println(s"[${resp.status}] ${resp.content.getOrElse("")}")
                true
            } else {
                false
            }
        }

        override def usage: String = "put <key> <value>"

        override def help: String = "Stores <value> at <key>"
    })
    commands.put("cas", new Command() {
        override def execute(cmdline: Array[String], worker: ClientService): Boolean = {
            if (cmdline.length == 4) {
                val fr = worker.op(cmdline)
                val resp: OperationResponse = fr.get()
                out.get.println(s"[${resp.status}] ${resp.content.getOrElse("")}")
                true
            } else {
                false
            }
        }

        override def usage: String = "cas <key> <refValue> <newValue>"

        override def help: String = "Performs a compare-and-swap for <key>. If the value at <key> equals <refValue>, <newValue> is stored."
    })


    var padTo = 0

    commands.put("exit", exitCommand)
    commands.put("quit", exitCommand)

    val commandSet: Set[Command] = commands.values.toSet


    private var out: Option[PrintWriter] = None
    private var terminal: Terminal = _
    private var reader: LineReader = _
    for (command <- commandSet) {
        val useLength: Int = command.usage.length
        if (useLength > longestCom) {
            longestCom = useLength
        }
        padTo = longestCom + 4
    }

    override def run(): Unit = {
        terminal = TerminalBuilder.terminal
        reader = LineReaderBuilder.builder()
            .terminal(terminal)
            .build()
        out = Some(terminal.writer())
        val layout: PatternLayout = new PatternLayout()
        // TODO Make coloredPatternLayout
        val rootLogger: Logger = LogManager.getRootLogger
        rootLogger.removeAllAppenders()
        rootLogger.addAppender(new WriterAppender(layout, out.get))
        while (true) {
            var line: Option[String] = None
            try {
                line = Some(reader.readLine(Console.PROMPT, null, null, null))
            } catch {
                case _: Throwable => // Ignore
            }
            if (line.isDefined) {
                line = Some(line.get.trim)
                if (!line.get.isEmpty) {
                    val cmdline = line.get.split(" ")
                    var cmd = cmdline(0)
                    var c = commands.get(cmd)
                    if (c == null) {
                        cmd = cmd.toLowerCase
                        c = commands.get(cmd)
                    }
                    if (c.isDefined ) {
                        if (!c.get.execute(cmdline, service)) {
                            out.get.print("Usage: ")
                            out.get.println(c.get.usage)
                        }
                    }
                    else out.get.println("Unknown command: " + cmd + " (use 'help' to see available commands)")
                }
            }
        }
    }

    abstract class Command {
        def execute(cmdline: Array[String], worker: ClientService): Boolean

        def usage: String

        def help: String
    }

}
