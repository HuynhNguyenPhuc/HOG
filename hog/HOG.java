package hog;

import java.util.ArrayList;
import java.util.List;
import java.awt.Rectangle;

import classifier.SVMHelper;
import libsvm.svm_model;

import utils.ImageHelper;
import utils.RectangleHelper;
import utils.SobelFilter;

public class HOG {
    private float[][] image;
    private float[][][] gradients;
    private float[][] magnitudes;
    private float[][] angles;
    private svm_model model;

    private int[] size;

    private int nbins;
    private short[] pixels_per_cell;
    private short[] cells_per_block;

    public HOG(float[][] image, int nbins, short[] pixels_per_cell, short[] cells_per_block, svm_model model) {
        this.image = image;
        this.nbins = nbins;
        this.pixels_per_cell = pixels_per_cell;
        this.cells_per_block = cells_per_block;
        this.model = model;

        this.size = new int[] {this.image.length, this.image[0].length}; 

        this.gradients = ImageHelper.applyFilter(this.image, SobelFilter.kernelX, SobelFilter.kernelY);
        this.magnitudes = ImageHelper.getMagnitude(gradients);
        this.angles = ImageHelper.getAngle(gradients);
    }

    public float[] compute() {
        int n_x_cells = this.image[0].length / this.pixels_per_cell[1];
        int n_y_cells = this.image.length / this.pixels_per_cell[0];

        float[][][] histogram = new float[n_y_cells][n_x_cells][this.nbins];

        for (int y = 0; y < n_y_cells; y++) {
            for (int x = 0; x < n_x_cells; x++) {
                histogram[y][x] = this.computeHistogram(x, y);
            }
        }

        return this.computeDescriptor(histogram);
    }

    private float[] computeHistogram(int x, int y) {
        float[] histogram = new float[this.nbins];
        float degree_per_bin = 360.0f / this.nbins;

        for (int dy = 0; dy < pixels_per_cell[0]; dy++) {
            for (int dx = 0; dx < pixels_per_cell[1]; dx++) {
                int pixel_x = x * pixels_per_cell[1] + dx;
                int pixel_y = y * pixels_per_cell[0] + dy;

                if (pixel_x >= image[0].length || pixel_y >= image.length) {
                    continue;
                }

                float magnitude = this.magnitudes[pixel_y][pixel_x];
                float angle = this.angles[pixel_y][pixel_x];

                // Calculate the bin index
                int bin_0 = (((int) (angle / degree_per_bin)) % nbins + nbins) % nbins;
                int bin_1 = (bin_0 + 1) % nbins;

                // Interpolate the value for bin l and l + 1
                float angle_0 = bin_0 * degree_per_bin;
                float angle_1 = bin_1 * degree_per_bin;
                float t = (angle_1 - angle) / (angle_1 - angle_0 + 1e-6f);

                histogram[bin_0] += magnitude * t;
                histogram[bin_1] += magnitude * (1 - t);
            }
        }

        return histogram;
    }

    private float[] computeDescriptor(float[][][] histogram) {
        int n_x_cells = this.image[0].length / this.pixels_per_cell[1];
        int n_y_cells = this.image.length / this.pixels_per_cell[0];

        int n_x_blocks = n_x_cells - this.cells_per_block[1] + 1;
        int n_y_blocks = n_y_cells - this.cells_per_block[0] + 1;
        int block_descriptor_length = this.cells_per_block[0] * this.cells_per_block[1] * this.nbins;

        float[] descriptor = new float[n_x_blocks * n_y_blocks * block_descriptor_length];

        int descriptor_index = 0;

        for (int y = 0; y < n_y_blocks; y++) {
            for (int x = 0; x < n_x_blocks; x++) {
                float[] block_descriptor = new float[block_descriptor_length];

                int block_index = 0;
                float squared_sum = 0.0f;

                for (int dy = 0; dy < this.cells_per_block[0]; dy++) {
                    for (int dx = 0; dx < this.cells_per_block[1]; dx++) {
                        for (int i = 0; i < this.nbins; i++) {
                            block_descriptor[block_index] = histogram[y + dy][x + dx][i];
                            squared_sum += block_descriptor[block_index] * block_descriptor[block_index];
                            block_index++;
                        }
                    }
                }

                float sqrt_sum = (float) Math.sqrt(squared_sum);

                for (int i = 0; i < block_index; i++) {
                    float normalized_value = (float) (block_descriptor[i] / (sqrt_sum + 1e-6));
                    descriptor[descriptor_index++] = Math.max(0.0f, Math.min(1.0f, normalized_value));
                }
            }
        }

        return descriptor;
    }

    public List<Rectangle> detectMultiScale(short[] winSize, short[] winStride, float scaleFactor, float threshold, int groupThreshold) {
        List<Rectangle> detections = new ArrayList<>();
        List<Float> weights = new ArrayList<>();
    
        float scale = 1.0f;
    
        while (true) {
            float[][] scaledImage = (float[][]) ImageHelper.scale(this.image, scale);

            int scaledHeight = scaledImage.length;
            int scaledWidth = scaledImage[0].length;
    
            int windowHeight = winSize[0];
            int windowWidth = winSize[1];
    
            if (scaledHeight < windowHeight || scaledWidth < windowWidth) break;
    
            for (int y = 0; y < scaledHeight - windowHeight; y += winStride[0]) {
                for (int x = 0; x < scaledWidth - windowWidth; x += winStride[1]) {
                    float[][] window = (float[][]) ImageHelper.crop(scaledImage, x, y, windowWidth, windowHeight);
                    if (window == null) continue;
                    window = (float[][]) ImageHelper.resize(window, this.size[1], this.size[0], "bilinear");
                    HOG hog = new HOG(window, nbins, pixels_per_cell, cells_per_block, model);
                    float[] descriptor = hog.compute();
                    double[] scores = SVMHelper.predict_probability(this.model, descriptor);
                    // System.out.println("Scores: " + scores[0] + ", " + scores[1]);
                    if (scores[0] > threshold) {
                        int originalX = (int) (x / scale);
                        int originalY = (int) (y / scale);
                        int originalWidth = (int) (windowWidth / scale);
                        int originalHeight = (int) (windowHeight / scale);
    
                        detections.add(new Rectangle(originalX, originalY, originalWidth, originalHeight));
                        weights.add((float) scores[0]);
                    }
                }
            }
            scale /= scaleFactor;
        }

        List<Rectangle> rects = RectangleHelper.proposedNMS(detections, weights, 0.3f);
        List<Rectangle> mergedRects = RectangleHelper.groupRectangles(rects, groupThreshold, 0.5f);
        if (mergedRects.size() == 0){
            System.out.println("No rectangles detected.");
        }
        else{
            System.out.println("Number of detected rectangles: " + mergedRects.size());
            RectangleHelper.print(mergedRects);
        }    
        return mergedRects;
    }


    public static int computeHOGFeatureLength(int[] imageSize, int nbins, short[] pixels_per_cell, short[] cells_per_block) {
        int image_width = imageSize[0];
        int image_height = imageSize[1];

        int cellWidth = pixels_per_cell[1];
        int cellHeight = pixels_per_cell[0];
        int blockWidth = cells_per_block[1];
        int blockHeight = cells_per_block[0];

        int n_x_cells = image_width / cellWidth;
        int n_y_cells = image_height / cellHeight;
        int n_x_blocks = n_x_cells - blockWidth + 1;
        int n_y_blocks = n_y_cells - blockHeight + 1;

        int block_descriptor_length = blockWidth * blockHeight * nbins;
        return n_x_blocks * n_y_blocks * block_descriptor_length;
    }
}