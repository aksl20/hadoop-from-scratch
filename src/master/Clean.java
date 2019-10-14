package master;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class Clean {
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

    public static void main(String[] args) throws InterruptedException {
        ArrayList<String> hostnames = Deploy.read_file(args[0]);
        ArrayList<String> remove_folder = new ArrayList<>();
        ArrayList<String> check_remove = new ArrayList<>();

        // Create list of commands for each machines
        assert hostnames != null;
        for (String hostname : hostnames){
            remove_folder.add("ssh acamara@" + hostname + " rm -rf /tmp/acamara");
            check_remove.add("ssh acamara@" + hostname + " ls /tmp/acamara");
        }


        // Apply health checker
        List<Boolean> returnValue = launch_actions_with_return(remove_folder);
        System.out.println(returnValue);
        returnValue = launch_actions_with_return(check_remove);
        System.out.println(returnValue);
    }
}
