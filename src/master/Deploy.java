package master;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;

public class Deploy {

    public static ArrayList<String> read_file(String filename) {
        ArrayList<String> lines = new ArrayList<>();
        try {
            lines = (ArrayList<String>) Files.readAllLines(Paths.get(filename));
        } catch (IOException e) {
            System.out.println("Problem when loading file");
            return null;
        }
        return lines;
    }

    public static ArrayList<String[]> tokenize(ArrayList<String> lines) {
        ArrayList<String[]> tokenize_corpus = new ArrayList<>();
        for (String line : lines) {
            // removes punctuations
            line = line.replaceAll("\\p{Punct}", "").toLowerCase().trim();

            tokenize_corpus.add(line.split(" "));
        }

        return tokenize_corpus;
    }

    public static void launch_actions_without_return(ArrayList<String> actions) throws InterruptedException {
        ArrayList<Master.ProcessLauncher> launchers = new ArrayList<>();
        for (String command : actions)
            launchers.add(new Master.ProcessLauncher(command, 2));

        launchers.forEach(launcher -> {
            try {
                launcher.launch_process();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        });

        for (Master.ProcessLauncher launcher : launchers)
            launcher.input_stream.join();
    }

    public static List<Integer> launch_actions_with_return(ArrayList<String> actions) throws InterruptedException {
        ArrayList<Master.ProcessLauncher> launchers = new ArrayList<>();
        for (String command : actions) {
            launchers.add(new Master.ProcessLauncher(command, 2));
        }
        List<Integer> returnValue = launchers.parallelStream()
                .map(launcher -> {
                    try {
                        return launcher.launch_process();
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                        return 1;
                    }
                }).collect(Collectors.toCollection(ArrayList::new));
        for (Master.ProcessLauncher launcher : launchers)
            launcher.input_stream.join();
        return returnValue;
    }

    public static void main(String[] args) throws InterruptedException {
        ArrayList<String> hostnames = Deploy.read_file(args[0]);
        ArrayList<String> health_checks = new ArrayList<>();
        ArrayList<String> create_dirs = new ArrayList<>();
        ArrayList<String> check_dir = new ArrayList<>();
        ArrayList<String> copy_jar = new ArrayList<>();

        // Create list of commands for each machines
        assert hostnames != null;
        for (String hostname : hostnames) {
            health_checks.add("ssh -o StrictHostKeyChecking=no acamara@" + hostname + " hostname");
            create_dirs.add("ssh acamara@" + hostname + " if test ! -d /tmp/acamara; then mkdir -p /tmp/acamara; fi");
            check_dir.add("ssh acamara@" + hostname + " ls /tmp/acamara");
            copy_jar.add("scp /home/axel/IdeaProjects/mapreduce-from-scratch/out/artifacts/mapreduce_from_scratch_jar/mapreduce-from-scratch.jar acamara@" + hostname + ":/tmp/acamara/slave.jar");
        }

        // Apply health checker
        List<Integer> returnValue = launch_actions_with_return(health_checks);

        // Check all machine are alive and deploy jar
        boolean isNodesOk = returnValue.stream().allMatch(x -> x == 0);
        if (isNodesOk) {
            launch_actions_without_return(create_dirs);

            // wait for directories creation and check the creation
            Thread.sleep(3000);
            returnValue = launch_actions_with_return(check_dir);

            // if all directories are created, deploy jar file
            if (returnValue.stream().allMatch(x -> x == 0)) {
                launch_actions_without_return(copy_jar);
            } else {
                System.out.println("Something went wrong during the directories creation");
            }
        } else {
            System.out.println("Not all nodes are safe, please check connection");
        }
    }
}
