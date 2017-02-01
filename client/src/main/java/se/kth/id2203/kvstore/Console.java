/*
 * The MIT License
 *
 * Copyright 2017 Lars Kroll <lkroll@kth.se>.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package se.kth.id2203.kvstore;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.apache.log4j.PatternLayout;
import org.apache.log4j.WriterAppender;
import org.jline.reader.EndOfFileException;
import org.jline.reader.LineReader;
import org.jline.reader.LineReaderBuilder;
import org.jline.reader.UserInterruptException;
import org.jline.terminal.Terminal;
import org.jline.terminal.TerminalBuilder;
import util.log4j.ColoredPatternLayout;

/**
 *
 * @author Lars Kroll <lkroll@kth.se>
 */
public class Console implements Runnable {

    private static final String PROMPT = ">";

    private LineReader reader;
    private Terminal terminal;
    private PrintWriter out;
    private final ClientService service;
    private final Map<String, Command> commands = new HashMap<>();
    private final int padTo;

    public Console(ClientService service) {
        this.service = service;
    }

    {
        commands.put("op", new Command() {

            @Override
            public boolean execute(String[] cmdline, ClientService worker) {
                if (cmdline.length == 2) {
                    Future<OpResponse> fr = worker.op(cmdline[1]);
                    out.println("Operation sent! Awaiting response...");
                    try {
                        OpResponse r = fr.get();
                        out.println("Operation complete! Response was: " + r.status);
                        return true;
                    } catch (InterruptedException | ExecutionException ex) {
                        ex.printStackTrace(out);
                        return false;
                    }

                } else {
                    return false;
                }
            }

            @Override
            public String usage() {
                return "op <key>";
            }

            @Override
            public String help() {
                return "Just a test operation...replace with proper put get";
            }
        });
        commands.put("help", new Command() {

            @Override
            public boolean execute(String[] cmdline, ClientService worker) {
                //out.println("Interface currently connected to " + bootstrapAddr + ":" + bootstrapPort + ".\n\n");
                out.println("Available commands: \n\n");
                Set<Command> comSet = new HashSet<>(commands.values());
                StringBuilder sb = new StringBuilder();
                for (Command c : comSet) {
                    sb.append(c.usage());
                    for (int i = c.usage().length(); i < padTo; i++) {
                        sb.append(' ');
                    }
                    sb.append(c.help());
                    sb.append('\n');
                }
                out.println(sb.toString());
                return true;
            }

            @Override
            public String usage() {
                return "help";
            }

            @Override
            public String help() {
                return "shows this help";
            }
        });
        Command exitcom = new Command() {

            @Override
            public boolean execute(String[] cmdline, ClientService worker) {
                out.println("Exiting...");
                System.exit(0);
                return true; // clearly an unreachable statement, mr java compiler -.-
            }

            @Override
            public String usage() {
                return "exit|quit";
            }

            @Override
            public String help() {
                return "closes the shell";
            }
        };
        commands.put("exit", exitcom);
        commands.put("quit", exitcom);
        int longestCom = 0;
        Set<Command> comSet = new HashSet<Command>(commands.values());
        for (Command c : comSet) {
            int useLength = c.usage().length();
            if (useLength > longestCom) {
                longestCom = useLength;
            }
        }
        padTo = longestCom + 4;
    }

    @Override
    public void run() {
        try {
            terminal = TerminalBuilder.terminal();
            reader = LineReaderBuilder.builder()
                    .terminal(terminal)
                    .build();
            out = terminal.writer();
            PatternLayout layout = new ColoredPatternLayout("%d{[HH:mm:ss,SSS]} %-5p {%c{1}} %m%n");
            Logger rootL = LogManager.getRootLogger();
            rootL.removeAllAppenders();
            rootL.addAppender(new WriterAppender(layout, out));
            while (true) {
                String line = null;
                try {
                    line = reader.readLine(PROMPT, null, null, null);
                } catch (UserInterruptException e) {
                    // Ignore
                } catch (EndOfFileException e) {
                    return;
                }
                if (line == null) {
                    continue;
                }
                line = line.trim();
                if (line.isEmpty()) {
                    continue;
                }
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
