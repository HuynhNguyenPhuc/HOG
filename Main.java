import java.util.List;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;

import libsvm.svm_model;
import classifier.SVMHelper;
import hog.HOG;

import utils.DataHelper;
import utils.ImageHelper;

public class Main {
    public static int [] size = {160, 96};
    public static int nbins = 18;
    public static short[] pixels_per_cell = new short[]{8, 8};
    public static short[] cells_per_block = new short[]{2, 2};

    public static void main(String[] args){
        /* 
        * Window size: 80 x 80 
        * Window stride: 20 x 20
        */
        String dataPath = "outputs/shipsnet/shipsnet_data.txt";
        String modelPath = "outputs/shipsnet/shipsnet_model.txt";

        String positive_path = "data/shipsnet_data/1/";
        String negative_path = "data/shipsnet_data/0/";

        /* 
        * Image size: 160 x 96
        * Window size: 80 x 48 
        * Window stride: 16 x 16
        * Scale factor: 1.2
        * Positive threshold: 0.4
        * Negative threshold: 0.8
        */
        // String dataPath = "outputs/pedestrian/pedestrian_data.txt";
        // String modelPath = "outputs/pedestrian/pedestrian_model.txt";

        // String positive_path = "data/pedestrian_data/pedestrians/";
        // String negative_path = "data/pedestrian_data/no_pedestrians/";
        
        // preprocess(positive_path, negative_path, dataPath);

        // gridSearch(dataPath, modelPath, 10, "linear");

        // train(dataPath, modelPath);

        svm_model model = SVMHelper.load(modelPath);

        // predictImages(model, positive_path, 0.5f);
        // predictImages(model, negative_path, 0.8f);

        predict(model, "shipsnet_data/scenes/lb_1.png");
        // predict(model, "shipsnet_data/scenes/lb_2.png");
        // predict(model, "shipsnet_data/scenes/lb_3.png");
        // predict(model, "shipsnet_data/scenes/lb_4.png");

        // predict(model, "shipsnet_data/scenes/sfbay_1.png");
        // predict(model, "shipsnet_data/scenes/sfbay_2.png");
        // predict(model, "shipsnet_data/scenes/sfbay_3.png");
        // predict(model, "shipsnet_data/scenes/sfbay_4.png");
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
        float[][] test_image = (float[][]) ImageHelper.load(savePath, null, "grayscale");
        HOG hog = new HOG(test_image, nbins, pixels_per_cell, cells_per_block, model);

        long startTime = System.currentTimeMillis();
        List<Rectangle> rectangles = hog.detectMultiScale(new short[]{80, 80}, new short[]{40, 40}, 500f, 0.5f, 0);
        long endTime = System.currentTimeMillis();
        
        System.out.println("Time: " + (endTime - startTime) + " ms");

        BufferedImage image = ImageHelper.drawRectangles(test_image, rectangles, ImageHelper.RED);
        String fileName = savePath.substring(savePath.lastIndexOf('/') + 1);
        ImageHelper.save(image, "results" + "/" + "test_" + fileName);
    }

    public static void predictImages(svm_model model, String path, float threshold){
        Object[] data = DataHelper.getImages(path, size);
        String[] image_names = (String[]) data[0];
        float[][][] images = (float[][][]) data[1];

        for (int i = 0; i < images.length; i++) {
            HOG hog = new HOG(images[i], nbins, pixels_per_cell, cells_per_block, model);
            List<Rectangle> rectangles = hog.detectMultiScale(new short[]{80, 64}, new short[]{4, 4}, 1.05f, threshold, 0);
            BufferedImage image = ImageHelper.drawRectangles(images[i], rectangles, ImageHelper.RED);
            ImageHelper.save(image, "results" + "/" + path.substring(5) + "detected_" + image_names[i]);
        }
    }
}