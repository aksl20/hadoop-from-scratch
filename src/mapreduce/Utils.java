package mapreduce;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.Stream;

class Utils {


    static ArrayList<String> read_file(String filename) {
        ArrayList<String> lines;
        try {
            lines = (ArrayList<String>) Files.readAllLines(Paths.get(filename));
        } catch (IOException e) {
            System.out.println("Problem when loading file");
            return null;
        }
        return lines;
    }

    static ArrayList<String[]> tokenize(ArrayList<String> lines) {
        ArrayList<String[]> tokenize_corpus = new ArrayList<>();
        for (String line : lines) {
            // removes punctuations
            line = line.toLowerCase().trim();

            tokenize_corpus.add(line.split(" "));
        }

        return tokenize_corpus;
    }

    static void launch_actions_without_return(ArrayList<String> actions) throws InterruptedException {
        ArrayList<ProcessLauncher> launchers = new ArrayList<>();
        for (String command : actions) {
            launchers.add(new ProcessLauncher(command, 2));
            System.out.println(command);
        }

        launchers.parallelStream().forEach(ProcessLauncher::launch_process);

        for (ProcessLauncher launcher : launchers)
            launcher.input_stream.join();
    }

    static List<Boolean> launch_actions_with_return(ArrayList<String> actions) throws InterruptedException {
        ArrayList<ProcessLauncher> launchers = new ArrayList<>();
        for (String command : actions) {
            launchers.add(new ProcessLauncher(command, 2));
        }
        List<Boolean> returnValue = launchers.parallelStream()
                .map(ProcessLauncher::launch_process).collect(Collectors.toCollection(ArrayList::new));
        for (ProcessLauncher launcher : launchers)
            launcher.input_stream.join();
        return returnValue;
    }

    static List<String> list_directory(String path_to_directory){
        try (Stream<Path> walk = Files.walk(Paths.get(path_to_directory))) {

            return walk.filter(Files::isRegularFile)
                    .map(Path::toString).collect(Collectors.toList());
        } catch (IOException e) {
            e.printStackTrace();
        }
        return Collections.emptyList();
    }

    static void write_file(List<String> word_count, String filename, String mode) throws IOException {
        if (mode.equals("a")){
            try{
                FileWriter fstream = new FileWriter(filename,true);
                BufferedWriter writer = new BufferedWriter(fstream);
                for (String line:word_count){
                    writer.write(line + "\n");
                }
                writer.close();
            }catch (Exception e){
                System.err.println("Error while writing to file: " +
                        e.getMessage());
            }
        } else if (mode.equals("w")){
            BufferedWriter writer = new BufferedWriter(new FileWriter(filename));
            for (String line:word_count){
                writer.write(line + "\n");
            }
            writer.close();
        }
    }

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
            return !tooLong && process.exitValue()==0;
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
}
