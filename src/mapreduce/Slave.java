package mapreduce;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.Map.Entry;
import java.net.InetAddress;

import static java.util.stream.Collectors.*;

public class Slave{

    private static ArrayList<String> transform_key_value(String filename){
        // Function for the question 1
        ArrayList<String> lines = Utils.read_file(filename);
        assert lines != null;
        ArrayList<String[]> lines_tokenized = Utils.tokenize(lines);
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

    private static HashMap<String, Double> words_count(String filename){
        // Function for the question 1
        ArrayList<String> lines = Utils.read_file(filename);
        assert lines != null;
        ArrayList<String[]> lines_tokenized = Utils.tokenize(lines);
        HashMap<String, Double> words_count = new HashMap<>();

        for (String[] line_tokenized: lines_tokenized) {
            String word = line_tokenized[0];
            if (!word.isEmpty()){
                if (words_count.get(word) == null) {
                    words_count.put(word, 1.0);
                } else {
                    words_count.put(word, words_count.get(word)+1);
                }
            }
        }
        return words_count;
    }

    public static HashMap<String, Double> sorted_map_by_numeric_value(HashMap<String, Double> hash_map){
        // Function for question 2
        return hash_map
                .entrySet()
                .stream()
                .sorted(Collections.reverseOrder(Entry.comparingByValue()))
                .collect(toMap(Entry::getKey, Entry::getValue, (e1, e2) -> e2,
                        LinkedHashMap::new));
    }

    public static HashMap<String, Double> sort(HashMap<String, Double> hash_map){
        // Function for question 3
        return hash_map
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
                }).collect(toMap(Entry::getKey, Entry::getValue, (e1, e2) -> e1,
                        LinkedHashMap::new));
    }

    public static void print_map(Map<String, Double> hash_map) {
        for (Entry<String, Double> el:hash_map.entrySet()) {
            System.out.println(el.getKey() + " " + el.getValue());
        }
    }

    public static void print_map(Map<String, Double> hash_map, Integer n) {
        Integer count = 0;
        for (Entry<String, Double> el:hash_map.entrySet()) {
            if(count.equals(n)){
                break;
            } else {
                System.out.println(el.getKey() + " " + el.getValue());
                count++;
            }
        }
    }

    private static void map(String src_file, String map_file) throws IOException {
        ArrayList<String> words_count = Slave.transform_key_value(src_file);
        Utils.write_file(words_count, map_file, "a");
    }

    private static void shuffle(String file, ArrayList<String> hostnames) throws IOException, InterruptedException {
        String current_host = InetAddress.getLocalHost().getHostName();
        int nb_slaves = hostnames.size();

        ArrayList<String[]> keys_values = Utils.tokenize(Objects.requireNonNull(Utils.read_file(file)));
        for (String[] key_value:keys_values){
            int hash_key = key_value[0].hashCode();
            int compute_slave = hash_key%nb_slaves;
            if (!hostnames.get(compute_slave).equals(current_host)){
                File shuffle_file = new File ("/tmp/root/shuffle" + "/" + String.format("%d_%d_%d.txt",
                                                                                            hash_key,
                                                                                            compute_slave,
                                                                                            current_host.hashCode()));
                Utils.write_file(Collections.singletonList(key_value[0] + " " + key_value[1]),
                        shuffle_file.toString(),
                        "a");
                Deploy.deploy(Collections.singletonList(hostnames.get(compute_slave)),
                        shuffle_file.toString(), "/tmp/root/shuffle");
                shuffle_file.delete();
            } else {
                File shuffle_file = new File ("/tmp/root/shuffle" + "/" + hash_key + "_" + compute_slave + ".txt");
                Utils.write_file(Collections.singletonList(key_value[0] + " " + key_value[1]),
                         shuffle_file.toString(),
                        "a");
            }
        }
    }

    private static void reduce(String reduce_directory, String shuffle_directory) throws IOException {
        List<String> files = Utils.list_directory(shuffle_directory);
        HashMap<String, Double> words_count = new HashMap<>();

        for (String file:files) {
            words_count(file).forEach(
                    (key, value) -> words_count.merge(key, value, Double::sum)
            );
        }
        for (Entry<String, Double> key_value:words_count.entrySet()){
            int hash = key_value.getKey().hashCode();
            File reduce_file = new File(reduce_directory + "/" + hash + ".txt");
            Utils.write_file(Collections.singletonList(key_value.getKey() + " " + key_value.getValue()),
                                                    reduce_file.toString(), "w");
        }
    }

    public static void main(String[] args) throws IOException, InterruptedException {
        if (args[0].equals("0")){
            File split_file = new File(args[1]);
            String num = args[1].replaceAll("[^\\d]", "");
            File map_directory = new File(new File(split_file.getParent()).getParent() + "/maps");
            if (!map_directory.exists()){
                boolean result = map_directory.mkdir();
                if (!result){
                    System.out.println("Something goes wrong when creating maps folder");
                } else {
                    map(split_file.toString(), map_directory + "/UM" + num + ".txt");
                }
            } else {
                map(split_file.toString(), map_directory + "/UM" + num + ".txt");
            }
        } else if (args[0].equals("1")){
            ArrayList<String> hostnames = Utils.read_file("/tmp/root/hostnames.txt");
            ArrayList<String> health_checks = new ArrayList<>();
            assert hostnames != null;
            for (String hostname : hostnames)
                health_checks.add("ssh -o StrictHostKeyChecking=no root@" + hostname + " hostname");
            File shuffle_directory = new File("/tmp/root/shuffle");
            if (!shuffle_directory.exists()){
                boolean result = shuffle_directory.mkdir();
                if (!result){
                    System.out.println("Something goes wrong when creating shuffle folder");
                } else {
                    for (String file: Utils.list_directory("/tmp/root/maps")){
                        shuffle(file, hostnames);
                    }
                }
            } else {
                for (String file: Utils.list_directory("/tmp/root/maps")){
                    shuffle(file, hostnames);
                }
            }
        } else if (args[0].equals("2")){
            File reduce_directory = new File("/tmp/root/reduce");
            File shuffle_directory = new File("/tmp/root/shuffle");
            if (!reduce_directory.exists()){
                boolean result = reduce_directory.mkdir();
                if (!result){
                    System.out.println("Something goes wrong when creating reduce folder");
                } else {
                    reduce(reduce_directory.toString(), shuffle_directory.toString());
                }
            } else {
                reduce(reduce_directory.toString(), shuffle_directory.toString());
            }
        }

    }
}