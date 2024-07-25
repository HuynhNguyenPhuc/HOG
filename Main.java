import java.util.List;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;

import libsvm.svm_model;

public class Main {
    public static int [] size = {192, 192};
    public static int nbins = 9;
    public static short[] pixels_per_cell = new short[]{8, 8};
    public static short[] cells_per_block = new short[]{2, 2};

    public static void main(String[] args){
        String dataPath = "outputs/human/human_data.txt";
        String modelPath = "outputs/human/human_model.txt";

        // String positive_path = "human_data/1/";
        // String negative_path = "human_data/0/";
        // preprocess(positive_path, negative_path, "outputs/human/human_data.txt");

        // gridSearch(10, "linear");

        train(dataPath, modelPath);

        svm_model model = SVMHelper.load(modelPath);

        // predictImages(model, "human_data/1/");
        // predictImages(model, "human_data/0/");

        // predict(model, "human_data/1/0.png");
    }

    public static void preprocess(String positive_path, String negative_path, String savePath){
        DataHelper.getTrainingData(positive_path, negative_path, savePath, size, nbins, pixels_per_cell, cells_per_block);
    }

    public static void gridSearch(String dataPath, String modelPath, int numLevels, String mode){    
        Object[] objects = DataHelper.load(dataPath);

        float[][] training_data = (float[][]) objects[0];
        float[] labels = (float[]) objects[1];

        double[] C_values = new double[numLevels];
        double[] gamma_values = new double[numLevels];

        if (mode == "RBF"){
            for (int i = 0; i < numLevels; i++){
                C_values[i] = Math.pow(2, -(numLevels/2) + i + 1);
                gamma_values[i] = Math.pow(2, -(numLevels/2) + i + 1);
            }
        }
        else if (mode == "linear"){
            gamma_values = new double[1];
            gamma_values[0] = 0;
            for (int i = 0; i < numLevels; i++){
                C_values[i] = Math.pow(2, numLevels);
            }
        }

        svm_model model = SVMHelper.gridSearch(training_data, labels, C_values, gamma_values, 5, mode);
        SVMHelper.save(model, modelPath);
    }

    public static void train(String dataPath, String modelPath){
        Object[] objects = DataHelper.load(dataPath);

        float[][] training_data = (float[][]) objects[0];
        float[] labels = (float[]) objects[1];

        svm_model model = SVMHelper.fit(training_data, labels, 1);
        SVMHelper.save(model, modelPath);
    }

    public static void predict(svm_model model, String savePath){
        float[][] test_image = (float[][]) ImageHelper.load(savePath, size, "grayscale");
        HOG hog = new HOG(test_image, nbins, pixels_per_cell, cells_per_block, model);

        long startTime = System.currentTimeMillis();
        List<Rectangle> rectangles = hog.detectMultiScale(new short[]{64, 64}, new short[]{16, 16}, new short[]{8, 8}, 1.5f, 0.7f);
        long endTime = System.currentTimeMillis();
        
        System.out.println("Time: " + (endTime - startTime) + " ms");

        BufferedImage image = ImageHelper.drawRectangles(test_image, rectangles, ImageHelper.RED);
        String fileName = savePath.substring(savePath.lastIndexOf('/') + 1);
        ImageHelper.save(image, "results" + "/" + "test_" + fileName);
    }

    public static void predictImages(svm_model model, String path){
        Object[] data = DataHelper.getImages(path, size);
        String[] image_names = (String[]) data[0];
        float[][][] images = (float[][][]) data[1];

        for (int i = 0; i < images.length; i++) {
            HOG hog = new HOG(images[i], nbins, pixels_per_cell, cells_per_block, model);
            List<Rectangle> rectangles = hog.detectMultiScale(new short[]{64, 64}, new short[]{16, 16}, new short[]{8, 8}, 1.5f, 0.8f);
            BufferedImage image = ImageHelper.drawRectangles(images[i], rectangles, ImageHelper.RED);
            ImageHelper.save(image, "results" + "/" + path + "detected_" + image_names[i]);
        }
    }
}