package master;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.Stream;

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

    public static List<String> list_directory(String path_to_directory){
        try (Stream<Path> walk = Files.walk(Paths.get(path_to_directory))) {

            return walk.filter(Files::isRegularFile)
                    .map(Path::toString).collect(Collectors.toList());
        } catch (IOException e) {
            e.printStackTrace();
        }
        return Collections.emptyList();
    }


    public static void main(String[] args) throws InterruptedException {
        ArrayList<String> hostnames = Deploy.read_file(args[0]);
        List<String> files = list_directory(args[1]);
        ArrayList<String> health_checks = new ArrayList<>();
        ArrayList<String> create_dirs = new ArrayList<>();
        ArrayList<String> check_dir = new ArrayList<>();
        ArrayList<String> run_copy = new ArrayList<>();
        ArrayList<String> run_jar = new ArrayList<>();
        ArrayList<String> copy_hostnames_file = new ArrayList<>();
        Random rand = new Random();

        for (String file:files){
            String slave = hostnames.get(rand.nextInt(hostnames.size()));
            health_checks.add("ssh -o StrictHostKeyChecking=no acamara@" + slave + " hostname");
            create_dirs.add("ssh acamara@" + slave + " if test ! -d " + args[2] + "; then mkdir -p " + args[2] + "; fi");
            run_copy.add("scp -r " + file + " acamara@" + slave + ":" + args[2]);
            check_dir.add("ssh acamara@" + slave + " ls " + args[2]);
            run_jar.add("ssh acamara@" + slave + " java -jar /tmp/acamara/map.jar 0 " + file);
            copy_hostnames_file.add("scp " + args[0] + " acamara@" + slave + ":/tmp/acamara");
        }
        // Apply health checker
        List<Boolean> returnValue = Deploy.launch_actions_with_return(health_checks);

        // Check all machine are alive and deploy jar
        boolean isNodesOk = returnValue.stream().allMatch(x -> x);
        if (isNodesOk) {
            Deploy.launch_actions_without_return(create_dirs);

            // wait for directories creation and check the creation
            Thread.sleep(3000);
            returnValue = Deploy.launch_actions_with_return(check_dir);

            // if all directories are created, deploy jar file
            if (returnValue.stream().allMatch(x -> x)) {
                Deploy.launch_actions_without_return(run_copy);
            } else {
                System.out.println("Something went wrong during the directories creation");
            }
        } else {
            System.out.println("Not all nodes are safe, please check connection");
        }

        Deploy.deploy("/home/axel/IdeaProjects/mapreduce-from-scratch/data/hostnames.txt",
                "/home/axel/IdeaProjects/mapreduce-from-scratch/jar/map.jar", "/tmp/acamara/");

        Deploy.launch_actions_without_return(run_jar);
        Deploy.launch_actions_without_return(copy_hostnames_file);

        System.out.println("MAP FINISHED");
    }
}

