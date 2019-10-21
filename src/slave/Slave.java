package slave;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;

import static java.util.stream.Collectors.*;

public class Slave{

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
            tokenize_corpus.add(line.split(" "));
        }
        return tokenize_corpus;
    }

    public static ArrayList<String> words_count(String filename){
        // Function for the question 1
        ArrayList<String> lines = Slave.read_file(filename);
        ArrayList<String[]> lines_tokenized = Slave.tokenize(lines);
        ArrayList<String> words_count = new ArrayList<>();

        for (String[] line_tokenized: lines_tokenized) {
            for (String word:line_tokenized) {
                if (!word.isEmpty()){
                    words_count.add(word + " 1.0");
                }
            }
        }
        return words_count;
    }

    public static HashMap<String, Double> sorted_map_by_numeric_value(HashMap<String, Double> hash_map){
        // Function for question 2
        HashMap<String, Double> sorted_map = hash_map
                .entrySet()
                .stream()
                .sorted(Collections.reverseOrder(Map.Entry.comparingByValue()))
                .collect(toMap(Map.Entry::getKey, Map.Entry::getValue, (e1, e2) -> e2,
                        LinkedHashMap::new));
        return sorted_map;
    }

    public static HashMap<String, Double> sort(HashMap<String, Double> hash_map){
        // Function for question 3
        HashMap<String, Double> sorted_map = hash_map
                .entrySet()
                .stream()
                .sorted(new Comparator<Entry<String, Double>>() {
                    @Override
                    public int compare(Entry<String, Double> e1, Entry<String, Double> e2) {
                        if (e1.getValue().equals(e2.getValue())) {
                            return e1.getKey().compareTo(e2.getKey());
                        } else {
                            return e2.getValue().compareTo(e1.getValue());
                        }
                    }
                }).collect(toMap(Map.Entry::getKey, Map.Entry::getValue, (e1, e2) -> e1,
                        LinkedHashMap::new));
        return sorted_map;
    }

    public static void print_map(Map<String, Double> hash_map) {
        for (Entry<String, Double> el:hash_map.entrySet()) {
            System.out.println(el.getKey() + " " + el.getValue());
        }
    }

    public static void print_map(Map<String, Double> hash_map, Integer n) {
        Integer count = 0;
        for (Entry<String, Double> el:hash_map.entrySet()) {
            if(count == n){
                break;
            } else {
                System.out.println(el.getKey() + " " + el.getValue());
                count++;
            }
        }
    }

    public static void write_file(ArrayList<String> word_count, String filename) throws IOException {
        BufferedWriter writer = new BufferedWriter(new FileWriter(filename));
        for (String line:word_count){
            writer.write(line + "\n");
        }
        writer.close();
    }

    public static void main(String[] args) throws IOException {
        if (args[0].equals("0")){
            File split_file = new File(args[1]);
            String num = args[1].replaceAll("[^\\d]", "");
            File map_directory = new File(new File(split_file.getParent()).getParent() + "/maps");
            System.out.println(map_directory);
            if (!map_directory.exists()){
                boolean result = map_directory.mkdir();
                if (!result){
                    System.out.println("Something goes wrong when creating maps folder");
                } else {
                    ArrayList<String> words_count = Slave.words_count(split_file.toString());
                    write_file(words_count, map_directory + "/UM" + num + ".txt" );
                }
            } else {
                ArrayList<String> words_count = Slave.words_count(split_file.toString());
                write_file(words_count, map_directory + "/UM" + num + ".txt" );
            }
        }

    }
}