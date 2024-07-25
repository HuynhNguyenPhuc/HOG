import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;

public class DataHelper {
    public static void getTrainingData(String positive_path, String negative_path, String savePath, int[] size, int nbins, short[] pixels_per_cell, short[] cells_per_block){
        String[] positive_files = DataHelper.listFiles(positive_path);
        String[] negative_files = DataHelper.listFiles(negative_path);

        int hog_feature_length = HOG.computeHOGFeatureLength(size, nbins, pixels_per_cell, cells_per_block);
        int total_positive_samples = positive_files.length;
        int total_negative_samples = negative_files.length;
        int total_samples = total_positive_samples + total_negative_samples;
        
        float[][] data = new float[total_samples][hog_feature_length];
        int[] label = new int[total_samples];

        float[][] imageArray;
        HOG hog;

        for (int i = 0; i<total_positive_samples; i++){
            imageArray = (float[][]) ImageHelper.load(positive_path + positive_files[i], size, "grayscale");
            hog = new HOG(imageArray, nbins, pixels_per_cell, cells_per_block, null);
            data[i] = hog.compute();
            label[i] = 1;
        }

        for (int i = 0; i<total_negative_samples; i++){
            imageArray = (float[][]) ImageHelper.load(negative_path + negative_files[i], size, "grayscale");
            hog = new HOG(imageArray, nbins, pixels_per_cell, cells_per_block, null);
            data[total_positive_samples + i] = hog.compute();
            label[total_positive_samples + i] = 0;
        }

        
        DataHelper.save(data, label, savePath);
    }

    public static Object[] getImages(String path, int[] size){
        String[] files = DataHelper.listFiles(path);

        int total_samples = files.length;
        
        float[][][] images = new float[total_samples][size[0]][size[1]];

        for (int i = 0; i<total_samples; i++){
            images[i] = (float[][]) ImageHelper.load(path + files[i], size, "grayscale");
        }

        return new Object[]{files, images};
    }

    public static void save(float[][] data, int[] labels, String outputPath) {
        try{
            String filePath = outputPath.substring(0, outputPath.lastIndexOf('/'));
            
            File directory = new File(filePath);
            if (!directory.exists()) {
                directory.mkdirs();
            }

            BufferedWriter writer = new BufferedWriter(new FileWriter(outputPath, false));
        
            for (int i = 0; i < data.length; i++) {
                StringBuilder line = new StringBuilder();
                for (float value : data[i]) {
                    line.append(value).append(" ");
                }
                line.append(labels[i]);
                writer.write(line.toString());
                writer.newLine();
            }

            writer.close();
            System.out.println("Data saved successfully to " + outputPath);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static Object[] load(String filePath) {
        ArrayList<float[]> data = new ArrayList<>();
        ArrayList<Float> labels = new ArrayList<>();
        
        try{
            BufferedReader reader = new BufferedReader(new FileReader(filePath));
            
            String line;
            while ((line = reader.readLine()) != null) {
                String[] tokens = line.split(" ");
                int length = tokens.length;
                float[] features = new float[length - 1];
                
                for (int i = 0; i < length - 1; i++) {
                    features[i] = Float.parseFloat(tokens[i]);
                }
                
                float label = Float.parseFloat(tokens[length - 1]);
                data.add(features);
                labels.add(label);
            }

            reader.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

        float[][] dataArray = new float[data.size()][];
        for (int i = 0; i < data.size(); i++) {
            dataArray[i] = data.get(i);
        }

        float[] labelArray = new float[labels.size()];
        for (int i = 0; i < labels.size(); i++) {
            labelArray[i] = labels.get(i);
        }

        return new Object[]{dataArray, labelArray};
    }

    public static String[] listFiles(String path){
        File folder = new File(path);
        String[] result = null;
        
        if (folder.exists() && folder.isDirectory()) {
            File[] files = folder.listFiles();
            
            if (files != null) {
                result = new String[files.length];
                for (int i = 0; i < files.length; i++) {
                    if (files[i].isFile()) {
                        result[i] = files[i].getName();
                    }
                }
            } else {
                System.out.println("The folder is empty.");
            }
        } else {
            System.out.println("The folder does not exist or is not a directory.");
        }
        
        return result;
    }
}
