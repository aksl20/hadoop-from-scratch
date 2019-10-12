package slave;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;

import static java.util.stream.Collectors.toMap;

public class Slave {
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

    public static HashMap<String, Double> words_count(String filename) {
        ArrayList<String> lines = Slave.read_file(filename);
        ArrayList<String[]> lines_tokenized = Slave.tokenize(lines);
        HashMap<String, Double> words_count = new HashMap<>();

        for (String[] line_tokenized : lines_tokenized) {
            for (String word : line_tokenized) {
                if (word.isEmpty() != true) {
                    if (words_count.get(word) == null) {
                        words_count.put(word, 1.0);
                    } else {
                        words_count.put(word, words_count.get(word) + 1);
                    }
                }

            }

        }

        return words_count;
    }

    public static HashMap<String, Double> sorted_map_by_numeric_value(HashMap<String, Double> hash_map) {
        HashMap<String, Double> sorted_map = hash_map
                .entrySet()
                .stream()
                .sorted(Collections.reverseOrder(Map.Entry.comparingByValue()))
                .collect(toMap(Map.Entry::getKey, Map.Entry::getValue, (e1, e2) -> e2,
                        LinkedHashMap::new));
        return sorted_map;
    }

    public static HashMap<String, Double> sort(HashMap<String, Double> hash_map) {
        HashMap<String, Double> sorted_map = hash_map
                .entrySet()
                .stream()
                .sorted(new Comparator<Map.Entry<String, Double>>() {
                    @Override
                    public int compare(Map.Entry<String, Double> e1, Map.Entry<String, Double> e2) {
                        if (e1.getValue().equals(e2.getValue())) {
                            return e1.getKey().compareTo(e2.getKey());
                        } else {
                            return e2.getValue().compareTo(e1.getValue());
                        }
                    }
                })
                .collect(toMap(Map.Entry::getKey, Map.Entry::getValue, (e1, e2) -> e1,
                        LinkedHashMap::new));
        return sorted_map;
    }


    public static void print_map(Map<String, Double> hash_map) {
        for (Map.Entry<String, Double> el : hash_map.entrySet()) {
            System.out.println(el.getKey() + " " + el.getValue());
        }
    }

    public static void main(String[] args) throws InterruptedException {
        // long startTime = System.currentTimeMillis();

        String input_file = args[0];
        HashMap<String, Double> words_count = Slave.words_count(input_file);
        HashMap<String, Double> words_count_sorted = Slave.sort(words_count);
        // Thread.sleep(10000);
        Slave.print_map(words_count_sorted);

        // long endTime = System.currentTimeMillis();
        // long totalTime = endTime - startTime;
        // System.out.print(totalTime + " ms");
    }
}
