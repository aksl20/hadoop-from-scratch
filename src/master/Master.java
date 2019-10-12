package master;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

public class Master {
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

    public static void main(String[] args) throws InterruptedException {
        // Deploy the
        String pc[] = new String[]{"sleep 10",
                "java -jar /home/axel/IdeaProjects/mapreduce-from-scratch/out/artifacts/mapreduce_from_scratch_jar/mapreduce-from-scratch.jar /home/axel/workspace/deontologie_police_nationale.txt",
                "sleep 10"};
        // Une facon de lancer les threads, sans attendre de retour de leur part:
        // Arrays.asList(pc).parallelStream().forEach(Deploy::sequentialProcessLauncher);

        // Une autre, et on fait une liste avec les valeurs de retour.
        List<Boolean> returnValue = Arrays.asList(pc).parallelStream()
                                    .map(Master::sequentialProcessLauncher)
                                    .collect(Collectors.toCollection(ArrayList::new));

        System.out.println(returnValue);
    }
}

