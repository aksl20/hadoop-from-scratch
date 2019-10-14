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
        boolean error = false;
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

        Integer launch_process() throws InterruptedException {
            String line;
            boolean running = true;
            while(running){

                while (((line = input_stream.queue.poll(this.timeout, TimeUnit.SECONDS)) != null)) {
                    System.out.print(line + "\n");
                }
                while (((line = error_stream.queue.poll()) != null)) {
                    System.out.print(line + "\n");
                }
                // Wait For retourne vrai si le programme est arrete

                running = !process.waitFor(5, TimeUnit.SECONDS);
            }
            return process.waitFor();
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


    public static void main(String[] args) throws InterruptedException {
        // Deploy the
        String[] pc = new String[]{"java -jar /home/axel/IdeaProjects/mapreduce-from-scratch/out/artifacts/mapreduce_from_scratch_jar/mapreduce-from-scratch.jar /home/axel/IdeaProjects/mapreduce-from-scratch/data/deontologie_police_nationale.txt"};
        // Une facon de lancer les threads, sans attendre de retour de leur part:
        Arrays.asList(pc).parallelStream()
                .forEach(command -> {
                    try {
                        new ProcessLauncher(command, 5).launch_process();
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }});

        /* List<Boolean> returnValue = Arrays.asList(pc).parallelStream()
                                    .map(command -> {
                                        try {
                                            return new ProcessLauncher(command, 5).launch_process();
                                        } catch (InterruptedException e) {
                                            e.printStackTrace();
                                            return false;
                                        }}).collect(Collectors.toCollection(ArrayList::new));
        System.out.println(returnValue); */
    }
}

