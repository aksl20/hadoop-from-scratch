package slave;

import master.Master;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
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

    public static void write_file(List<String> word_count, String filename, String mode) throws IOException {
        if (mode.equals("a")){
            try{
                FileWriter fstream = new FileWriter(filename,true);
                BufferedWriter writer = new BufferedWriter(fstream);
                for (String line:word_count){
                    writer.write(line + "\n");
                }
                writer.close();
            }catch (Exception e){
                System.err.println("Error while writing to file: " +
                        e.getMessage());
            }
        } else if (mode.equals("w")){
            BufferedWriter writer = new BufferedWriter(new FileWriter(filename));
            for (String line:word_count){
                writer.write(line + "\n");
            }
            writer.close();
        }

    }

    public static void main(String[] args) throws IOException {
        if (args[0].equals("0")){
            File split_file = new File(args[1]);
            String num = args[1].replaceAll("[^\\d]", "");
            File map_directory = new File(new File(split_file.getParent()).getParent() + "/maps");
            if (!map_directory.exists()){
                boolean result = map_directory.mkdir();
                if (!result){
                    System.out.println("Something goes wrong when creating maps folder");
                } else {
                    ArrayList<String> words_count = Slave.words_count(split_file.toString());
                    write_file(words_count, map_directory + "/UM" + num + ".txt", "w");
                }
            } else {
                ArrayList<String> words_count = Slave.words_count(split_file.toString());
                write_file(words_count, map_directory + "/UM" + num + ".txt", "w");
            }
        }else if (args[0].equals("1")){
            ArrayList<String> hostnames = read_file("/tmp/acamara/hostnames.txt");
            int nb_slaves = hostnames.size();
            File shuffle_directory = new File("/tmp/acamara/shuffle");
            if (!shuffle_directory.exists()){
                boolean result = shuffle_directory.mkdir();
                if (!result){
                    System.out.println("Something goes wrong when creating shuffle folder");
                } else {
                    for (String file: Master.list_directory("/tmp/acamara/maps")){
                        ArrayList<String[]> keys_values = tokenize(read_file(file));
                        for (String[] key_value:keys_values){
                            int hash_key = key_value[0].hashCode();
                            int compute_slave = hash_key%nb_slaves;
                            write_file(Arrays.asList(key_value[0] + " " + key_value[1]),
                                    "/tmp/acamara/shuffle" + "/" + hash_key + "_" + compute_slave + ".txt",
                                    "a");
                        }
                    }

                }
            } else {
                for (String file: Master.list_directory("/tmp/acamara/maps")){
                    ArrayList<String[]> keys_values = tokenize(read_file(file));
                    for (String[] key_value:keys_values){
                        int hash_key = key_value[0].hashCode();
                        int compute_slave = hash_key%nb_slaves;
                        write_file(Arrays.asList(key_value[0] + " " + key_value[1]),
                                "/tmp/acamara/shuffle" + "/" + hash_key + "_" + compute_slave + ".txt",
                                "a");
                    }
                }
            }
        }

    }
}