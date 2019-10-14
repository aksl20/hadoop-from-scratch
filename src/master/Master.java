package master;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Arrays;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

public class Master {

    static class ProcessLauncher {
        Process process;
        Integer timeout;
        String[] command;
        ThreadReaderStream input_stream;
        ThreadReaderStream error_stream;

        ProcessLauncher(String command, Integer timeout) {
            this.timeout = timeout;
            this.command = command.split(" ");
            ProcessBuilder builder = new ProcessBuilder(this.command);
            try {
                this.process = builder.start();
            } catch (IOException e1) {
                e1.printStackTrace();
            }
            assert process != null;
            input_stream = new ThreadReaderStream(process.getInputStream());
            error_stream = new ThreadReaderStream(process.getErrorStream());
            input_stream.start();
            error_stream.start();
        }

        boolean launch_process() {
            String line;
            boolean running = true;
            boolean tooLong = false;
            while (running) {
                try {
                    // Wait For retourne vrai si le programme est arrete
                    boolean stillRunning = !process.waitFor(5, TimeUnit.SECONDS);
                    // On lit la sortie standard. Si on a eu quelque chose, on continue
                    // ​
                    if (!input_stream.queue.isEmpty()) {
                        // On a du monde dans le buffer. On les recupere.
                        // Si on ne veut pas les récuperer, on peut faire un "reset"
                        // reader.reset();
                        while (((line = input_stream.queue.poll()) != null)) {
                            System.out.println(line);
                        }
                    } else if (!error_stream.queue.isEmpty()) {
                        // On a du monde dans le buffer. On les recupere.
                        // Si on ne veut pas les récuperer, on peut faire un "reset"
                        // reader.reset();
                        while (((line = error_stream.queue.poll()) != null)) {
                            System.out.println(line);
                        }
                    } else if (stillRunning) {
                        // Le process n'a rien écris pendant les 5 secondes. On le tue
                        tooLong = true;
                        process.destroy();
                    }
                    running = stillRunning && !tooLong;
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
            return !tooLong;
        }
    }

    static class ThreadReaderStream extends Thread {
        LinkedBlockingQueue<String> queue = new LinkedBlockingQueue<>();
        BufferedReader reader;

        ThreadReaderStream(InputStream stream) {
            this.reader = new BufferedReader(new InputStreamReader(stream));
        }

        @Override
        public void run() {
            try {
                String line;
                while ((line = reader.readLine()) != null) {
                    queue.put(line);
                }
            } catch (IOException | InterruptedException e) {
                e.printStackTrace();
            }
        }
    }


    public static void main(String[] args) throws InterruptedException {}
}

