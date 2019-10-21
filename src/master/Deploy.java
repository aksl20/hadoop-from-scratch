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

        launchers.forEach(Master.ProcessLauncher::launch_process);

        for (Master.ProcessLauncher launcher : launchers)
            launcher.input_stream.join();
    }

    public static List<Boolean> launch_actions_with_return(ArrayList<String> actions) throws InterruptedException {
        ArrayList<Master.ProcessLauncher> launchers = new ArrayList<>();
        for (String command : actions) {
            launchers.add(new Master.ProcessLauncher(command, 2));
        }
        List<Boolean> returnValue = launchers.parallelStream()
                .map(Master.ProcessLauncher::launch_process).collect(Collectors.toCollection(ArrayList::new));
        for (Master.ProcessLauncher launcher : launchers)
            launcher.input_stream.join();
        return returnValue;
    }

    public static Boolean deploy(String path_to_hostnames, String host_path, String remote_path) throws InterruptedException {
        ArrayList<String> hostnames = Deploy.read_file(path_to_hostnames);
        ArrayList<String> health_checks = new ArrayList<>();
        ArrayList<String> check_dir = new ArrayList<>();
        ArrayList<String> create_dirs = new ArrayList<>();
        ArrayList<String> run_copy = new ArrayList<>();

        // Create list of commands for each machines
        assert hostnames != null;
        for (String hostname : hostnames) {
            health_checks.add("ssh -o StrictHostKeyChecking=no acamara@" + hostname + " hostname");
            create_dirs.add("ssh acamara@" + hostname + " if test ! -d " + remote_path + "; then mkdir -p " + remote_path + "; fi");
            check_dir.add("ssh acamara@" + hostname + " ls /tmp/acamara");
            run_copy.add("scp -r " + host_path + "acamara@" + hostname + ":" + remote_path);
        }

        // Apply health checker
        List<Boolean> returnValue = launch_actions_with_return(health_checks);

        // Check all machine are alive and deploy jar
        boolean isNodesOk = returnValue.stream().allMatch(x -> x);
        if (isNodesOk) {
            launch_actions_without_return(create_dirs);

            // wait for directories creation and check the creation
            Thread.sleep(3000);
            returnValue = launch_actions_with_return(check_dir);

            // if all directories are created, deploy jar file
            if (returnValue.stream().allMatch(x -> x)) {
                launch_actions_without_return(run_copy);
            } else {
                System.out.println("Something went wrong during the directories creation");
                return false;
            }
        } else {
            System.out.println("Not all nodes are safe, please check connection");
            return false;
        }
        return true;
    }

    public static void main(String[] args) throws InterruptedException {
        System.out.println("Hello from deploy!");
    }
}
