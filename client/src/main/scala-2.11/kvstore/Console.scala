package kvstore

import java.io.PrintWriter

import kv.OperationResponse
import org.apache.log4j.{LogManager, Logger, PatternLayout, WriterAppender}
import org.jline.reader.{LineReader, LineReaderBuilder}
import org.jline.terminal.{Terminal, TerminalBuilder}

import scala.collection.mutable
import scala.concurrent.Future

object Console {
    private final val PROMPT: String = "> "
}
class Console(service: ClientService) extends Runnable {
    private val commands = new mutable.HashMap[String, Command]
    private var out: Option[PrintWriter] = None
    private var terminal: Terminal = _
    private var reader: LineReader = _

    commands.put("op", new Command() {
        override def execute(cmdline: Array[String], worker: ClientService): Boolean = {
            if (cmdline.length == 2) {
                val fr = worker.op(cmdline(1))
                out.get.println("Operation sent! Awaiting response.")
                val resp: OperationResponse = fr.get()
                out.get.println("Operation complete! Response was: " + resp.status)
                true
            } else {
                false
            }
        }

        override def usage: String = "op <key>"

        override def help: String = "Just a test operation...replace with proper PUT|GET|CAS"
    })

    val exitCommand: Command = new Command() {
        override def execute(cmdline: Array[String], worker: ClientService): Boolean = {
            out.get.println("Exiting...")
            System.exit(0)
            true
        }

        override def usage: String = "exit|quit"

        override def help: String = "Closes the shell"
    }

    commands.put("exit", exitCommand)
    commands.put("quit", exitCommand)

    var longestCom: Int = 0
    var padTo = 0
    val commandSet: Set[Command] = commands.values.toSet
    for(command <- commandSet) {
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
        val layout: PatternLayout = new PatternLayout() // TODO Make coloredPatternLayout
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
                    val cmdline = line.get.split(" ", 2)
                    var cmd = cmdline(0)
                    var c = commands.get(cmd)
                    if (c == null) {
                        cmd = cmd.toLowerCase
                        c = commands.get(cmd)
                    }
                    if (c.isDefined &&  !c.get.execute(cmdline, service)) {
                        out.get.print("Usage: ")
                        out.get.println(c.get.usage)
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
/*
public class Console implements Runnable {

    @Override
    public void run() {
        try {
                String[] cmdline = line.split(" ", 2);
                String cmd = cmdline[0];
                Command c = commands.get(cmd);
                if (c == null) {
                    cmd = cmd.toLowerCase();
                    c = commands.get(cmd);
                }
                if (c != null) {
                    if (!c.execute(cmdline, service)) {
                        out.print("Usage: ");
                        out.println(c.usage());
                    }
                } else {
                    out.println("Unkown command: " + cmd + " (use 'help' to see available commands)");
                }
            }
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }
    }

    public static abstract class Command {

        public abstract boolean execute(String[] cmdline, ClientService worker);

        public abstract String usage();

        public abstract String help();
    }
}

 */