package master;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;

public class Deploy {

    public static ArrayList<String> read_file(String filename){
        ArrayList<String> lines = new ArrayList<>();
        try {
            lines = (ArrayList<String>) Files.readAllLines(Paths.get(filename));
        } catch (IOException e) {
            System.out.println("Problem when loading file");
            return null;
        }
        return lines;
    }

    public static ArrayList<String[]> tokenize(ArrayList<String> lines){
        ArrayList<String[]> tokenize_corpus = new ArrayList<>();
        for (String line : lines) {
            // removes punctuations
            line = line.replaceAll("\\p{Punct}","").toLowerCase().trim();

            tokenize_corpus.add(line.split(" "));
        }

        return tokenize_corpus;
    }

    public static void main(String[] args) throws InterruptedException {
        ArrayList<String> hostnames = Deploy.read_file(args[0]);
        ArrayList<String> health_checks = new ArrayList<>();
        ArrayList<String> create_dirs = new ArrayList<>();
        ArrayList<String> check_dir = new ArrayList<>();
        ArrayList<String> copy_jar = new ArrayList<>();

        // Create list of commands for each machines
        assert hostnames != null;
        for (String hostname:hostnames){
            health_checks.add("ssh acamara@" + hostname + " hostname");
            create_dirs.add("ssh acamara@" + hostname + " if test ! -d /tmp/acamara; then mkdir -p /tmp/acamara; fi");
            check_dir.add("ssh acamara@" + hostname + " ls /tmp/acamara");
            copy_jar.add("scp /home/axel/IdeaProjects/mapreduce-from-scratch/out/artifacts/mapreduce_from_scratch_jar/mapreduce-from-scratch.jar acamara@" + hostname + ":/tmp/acamara/slave.jar");
        }

        // Apply health checker
        List<Boolean> returnValue = health_checks.parallelStream()
                .map(command -> {
                    try {
                        return new Master.ProcessLauncher(command, 5).launch_process();
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                        return false;
                    }}).collect(Collectors.toCollection(ArrayList::new));

        // Check all machine are alive and deploy jar
        boolean isNodesOk = returnValue.stream().allMatch(x -> x);
        if (isNodesOk) {
            create_dirs.forEach(command -> {
                try {
                    new Master.ProcessLauncher(command, 2).launch_process();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }});

            // wait for directories creation and check the creation
            Thread.sleep(3000);
            returnValue = check_dir.parallelStream()
                    .map(command -> {
                        try {
                            return new Master.ProcessLauncher(command, 2).launch_process();
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                            return false;
                        }}).collect(Collectors.toCollection(ArrayList::new));

            if (returnValue.stream().allMatch(x -> x)){
                copy_jar.parallelStream().forEach(command -> {
                    try {
                        new Master.ProcessLauncher(command, 2).launch_process();
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }});
            } else{
                System.out.println("Something went wrong during the directories creation");
            }
        } else {
            System.out.println("Not all nodes are safe, please check connection");
        }
    }
}
