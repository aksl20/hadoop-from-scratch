package master;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;
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

    public static boolean sequentialProcessLauncher(String command) {

        ProcessBuilder builder = new ProcessBuilder(command.split(" "));
        builder.redirectErrorStream(true);
        Process process = null;
        try {
            process = builder.start();
        } catch (IOException e1) {
            e1.printStackTrace();
        }
        BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));

        boolean running = true;
        boolean tooLong = false;
        while (running) {
            try {
                // Wait For retourne vrai si le programme est arrete
                boolean stillRunning = !process.waitFor(5, TimeUnit.SECONDS);
                // On lit la sortie standard. Si on a eu quelque chose, on continue
                if (reader.ready()) {
                    // On a du monde dans le buffer. On les recupere.
                    // Si on ne veut pas les récuperer, on peut faire un "reset"
                    // reader.reset();
                    while (reader.ready()) {
                        int c = reader.read();
                        System.out.print((char) c);
                    }
                } else if(stillRunning) {
                    // Le process n'a rien écris pendant les 5 secondes. On le tue
                    tooLong = true;
                    process.destroy();
                }
                running = stillRunning && !tooLong;
            } catch (IOException | InterruptedException e) {
                e.printStackTrace();
            }
        }
        return !tooLong;
    }

    static class MonRunnable implements Runnable {

        int position;
        boolean values[];
        String command;

        public MonRunnable(String command, boolean values[], int pos) {
            position = pos;
            this.values = values;
            this.command = command;
        }

        @Override
        public void run() {
            boolean b = sequentialProcessLauncher(command);
            values[position] = b;
            System.out.println("Valeur du retour: " + b);
        }
    }

    public static void main(String[] args) throws InterruptedException {
        ArrayList<String> hostnames = Deploy.read_file(args[0]);
        ArrayList<String> commands = new ArrayList<>();

        for (String hostname:hostnames){
            commands.add("ssh " + hostname + " hostname");
        }
        List<Boolean> returnValue = commands.parallelStream()
                .map(Deploy::sequentialProcessLauncher)
                .collect(Collectors.toCollection(ArrayList::new));

        System.out.println(returnValue);

    }
}
