import javax.imageio.ImageIO;
import java.io.File;
import java.io.IOException;
import java.util.List;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.image.BufferedImage;
import java.awt.Rectangle;

public class ImageHelper {
    public static final float[] RED = new float[]{1.0f, 0.0f, 0.0f};

    public static Object load(String filePath, int[] resize, String mode) {
        try {
            BufferedImage image = ImageIO.read(new File(filePath));
            if (resize != null && resize.length == 2) {
                int newWidth = resize[0];
                int newHeight = resize[1];
                image = resizeImage(image, newWidth, newHeight);
            }

            int width = image.getWidth();
            int height = image.getHeight();

            if ("rgb".equals(mode)) {
                return convertToRGBArray(image, width, height);
            } else if ("grayscale".equals(mode)) {
                return convertToGrayArray(image, width, height);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    private static float[][][] convertToRGBArray(BufferedImage image, int width, int height) {
        float[][][] imageArray = new float[height][width][3];
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int pixel = image.getRGB(x, y);
                Color color = new Color(pixel);
                imageArray[y][x][0] = color.getRed() / 255.0f;
                imageArray[y][x][1] = color.getGreen() / 255.0f;
                imageArray[y][x][2] = color.getBlue() / 255.0f;
            }
        }
        return imageArray;
    }

    private static float[][] convertToGrayArray(BufferedImage image, int width, int height) {
        float[][] imageArray = new float[height][width];
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int pixel = image.getRGB(x, y);
                Color color = new Color(pixel);
                float grayscale = (color.getRed() * 0.299f + color.getGreen() * 0.587f + color.getBlue() * 0.114f) / 255.0f;
                imageArray[y][x] = grayscale;
            }
        }
        return imageArray;
    }

    public static void save(Object imageArray, String name, String mode) {
        String directoryPath = "image_outputs";
        String filePath = directoryPath + "/" + name;

        File directory = new File(directoryPath);
        if (!directory.exists()) {
            directory.mkdirs();
        }

        BufferedImage image = null;
        if ("rgb".equals(mode)) {
            image = createRGBImage((float[][][]) imageArray);
        } else if ("grayscale".equals(mode)) {
            image = createGrayImage((float[][]) imageArray);
        }

        try {
            ImageIO.write(image, "png", new File(filePath));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static BufferedImage createRGBImage(float[][][] colorArray) {
        int width = colorArray[0].length;
        int height = colorArray.length;
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int r = Math.min(255, Math.max(0, (int) (colorArray[y][x][0] * 255)));
                int g = Math.min(255, Math.max(0, (int) (colorArray[y][x][1] * 255)));
                int b = Math.min(255, Math.max(0, (int) (colorArray[y][x][2] * 255)));
                int rgb = (r << 16) | (g << 8) | b;
                image.setRGB(x, y, rgb);
            }
        }
        return image;
    }

    private static BufferedImage createGrayImage(float[][] grayArray) {
        int width = grayArray[0].length;
        int height = grayArray.length;
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_BYTE_GRAY);
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int gray = Math.min(255, Math.max(0, (int) (grayArray[y][x] * 255)));
                int rgb = gray | (gray << 8) | (gray << 16);
                image.setRGB(x, y, rgb);
            }
        }
        return image;
    }

    public static Object crop(Object imageArray, int startX, int startY, int width, int height) {
        if (startX < 0 || startY < 0 || imageArray == null) return null;
        if (imageArray instanceof float[][][]) {
            return cropRGBArray((float[][][]) imageArray, startX, startY, width, height);
        } else if (imageArray instanceof float[][]) {
            return cropGrayArray((float[][]) imageArray, startX, startY, width, height);
        }
        return null;
    }

    private static float[][][] cropRGBArray(float[][][] rgbArray, int startX, int startY, int width, int height) {
        if (startX + width > rgbArray[0].length || startY + height > rgbArray.length) return null;
        float[][][] croppedArray = new float[height][width][3];
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                croppedArray[y][x][0] = rgbArray[startY + y][startX + x][0];
                croppedArray[y][x][1] = rgbArray[startY + y][startX + x][1];
                croppedArray[y][x][2] = rgbArray[startY + y][startX + x][2];
            }
        }
        return croppedArray;
    }

    private static float[][] cropGrayArray(float[][] grayArray, int startX, int startY, int width, int height) {
        if (startX + width > grayArray[0].length || startY + height > grayArray.length) return null;
        float[][] croppedArray = new float[height][width];
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                croppedArray[y][x] = grayArray[startY + y][startX + x];
            }
        }
        return croppedArray;
    }

    public static Object resize(Object imageArray, int newWidth, int newHeight, String mode) {
        if (imageArray == null) return null;
        if (imageArray instanceof float[][][]) {
            float[][][] rgbArray = (float[][][]) imageArray;
            float[][][] resizedArray = new float[newHeight][newWidth][3];
            float ratioX = rgbArray[0].length / (float) newWidth;
            float ratioY = rgbArray.length / (float) newHeight;

            if ("nn".equals(mode)) {
                for (int y = 0; y < newHeight; y++) {
                    for (int x = 0; x < newWidth; x++) {
                        int nx = Math.min((int) (x * ratioX), rgbArray[0].length - 1);
                        int ny = Math.min((int) (y * ratioY), rgbArray.length - 1);
                        resizedArray[y][x][0] = rgbArray[ny][nx][0];
                        resizedArray[y][x][1] = rgbArray[ny][nx][1];
                        resizedArray[y][x][2] = rgbArray[ny][nx][2];
                    }
                }
            } else if ("bilinear".equals(mode)) {
                for (int y = 0; y < newHeight; y++) {
                    for (int x = 0; x < newWidth; x++) {
                        float origX = x * ratioX;
                        float origY = y * ratioY;
                        int x1 = (int) origX;
                        int y1 = (int) origY;
                        int x2 = Math.min(x1 + 1, rgbArray[0].length - 1);
                        int y2 = Math.min(y1 + 1, rgbArray.length - 1);
                        float dx = origX - x1;
                        float dy = origY - y1;

                        for (int c = 0; c < 3; c++) {
                            float value = (1 - dx) * (1 - dy) * rgbArray[y1][x1][c]
                                        + dx * (1 - dy) * rgbArray[y1][x2][c]
                                        + (1 - dx) * dy * rgbArray[y2][x1][c]
                                        + dx * dy * rgbArray[y2][x2][c];
                            resizedArray[y][x][c] = value;
                        }
                    }
                }
            }
            return resizedArray;
        } else if (imageArray instanceof float[][]) {
            float[][] grayArray = (float[][]) imageArray;
            float[][] resizedArray = new float[newHeight][newWidth];
            float ratioX = grayArray[0].length / (float) newWidth;
            float ratioY = grayArray.length / (float) newHeight;

            if ("nn".equals(mode)) {
                for (int y = 0; y < newHeight; y++) {
                    for (int x = 0; x < newWidth; x++) {
                        int nx = Math.min((int) (x * ratioX), grayArray[0].length - 1);
                        int ny = Math.min((int) (y * ratioY), grayArray.length - 1);
                        resizedArray[y][x] = grayArray[ny][nx];
                    }
                }
            } else if ("bilinear".equals(mode)) {
                for (int y = 0; y < newHeight; y++) {
                    for (int x = 0; x < newWidth; x++) {
                        float origX = x * ratioX;
                        float origY = y * ratioY;
                        int x1 = (int) origX;
                        int y1 = (int) origY;
                        int x2 = Math.min(x1 + 1, grayArray[0].length - 1);
                        int y2 = Math.min(y1 + 1, grayArray.length - 1);
                        float dx = origX - x1;
                        float dy = origY - y1;

                        float value = (1 - dx) * (1 - dy) * grayArray[y1][x1]
                                    + dx * (1 - dy) * grayArray[y1][x2]
                                    + (1 - dx) * dy * grayArray[y2][x1]
                                    + dx * dy * grayArray[y2][x2];
                        resizedArray[y][x] = value;
                    }
                }
            }
            return resizedArray;
        }
        return null;
    }

    public static Object scale(Object imageArray, float k) {
        if (imageArray == null || k <= 0) return null;
        if (imageArray instanceof float[][][]) {
            float[][][] rgbArray = (float[][][]) imageArray;
            int newWidth = (int) (rgbArray[0].length * k);
            int newHeight = (int) (rgbArray.length * k);
            return resize(rgbArray, newWidth, newHeight, "bilinear");
        } else if (imageArray instanceof float[][]) {
            float[][] grayArray = (float[][]) imageArray;
            int newWidth = (int) (grayArray[0].length * k);
            int newHeight = (int) (grayArray.length * k);
            return resize(grayArray, newWidth, newHeight, "bilinear");
        }
        return null;
    }

    public static float[][][] applyFilter(float[][] imageArray, float[][] kernelX, float[][] kernelY) {
        if (imageArray == null) return null;
        
        float[][][] result = new float[imageArray.length][imageArray[0].length][2];
        for (int y = 0; y < imageArray.length; y++) {
            for (int x = 0; x < imageArray[0].length; x++) {
                float sumX = 0, sumY = 0;
                for (int ky = -1; ky <= 1; ky++) {
                    for (int kx = -1; kx <= 1; kx++) {
                        if (y + ky >= 0 && y + ky < imageArray.length && x + kx >= 0 && x + kx < imageArray[0].length) {
                            sumX += kernelX[ky + 1][kx + 1] * imageArray[y + ky][x + kx];
                            sumY += kernelY[ky + 1][kx + 1] * imageArray[y + ky][x + kx];
                        }
                    }
                }
                result[y][x][0] = sumX;
                result[y][x][1] = sumY;
            }
        }
        return result;
    }

    public static float[][] getMagnitude(float[][][] gradients) {
        float[][] magnitude = new float[gradients.length][gradients[0].length];
        for (int y = 0; y < gradients.length; y++) {
            for (int x = 0; x < gradients[0].length; x++) {
                float xSq = gradients[y][x][0] * gradients[y][x][0];
                float ySq = gradients[y][x][1] * gradients[y][x][1];
                magnitude[y][x] = (float) Math.sqrt(xSq + ySq);
            }
        }
        return magnitude;
    }

    public static float[][] getAngle(float[][][] gradients) {
        float[][] angle = new float[gradients.length][gradients[0].length];
        for (int y = 0; y < gradients.length; y++) {
            for (int x = 0; x < gradients[0].length; x++) {
                float radians = (float) Math.atan2(gradients[y][x][1], gradients[y][x][0]);
                angle[y][x] = (float) Math.toDegrees(radians);
            }
        }
        return angle;
    }

    public static float[][][] getImagePyramid(float[][] image, int numLevels, float scaleFactor) {
        float[][][] imagePyramid = new float[numLevels][][];
        for (int i = 0; i < numLevels; i++) {
            if (i == 0) {
                imagePyramid[i] = image;
            } else {
                imagePyramid[i] = (float[][]) scale(imagePyramid[i - 1], scaleFactor);
            }
        }
        return imagePyramid;
    }

    private static BufferedImage resizeImage(BufferedImage originalImage, int newWidth, int newHeight) {
        Image tempImage = originalImage.getScaledInstance(newWidth, newHeight, Image.SCALE_SMOOTH);
        BufferedImage resizedImage = new BufferedImage(newWidth, newHeight, BufferedImage.TYPE_INT_RGB);
        Graphics2D g2d = resizedImage.createGraphics();
        g2d.drawImage(tempImage, 0, 0, null);
        g2d.dispose();
        return resizedImage;
    }

    public static BufferedImage scaleImage(BufferedImage image, double scaleFactor) {
        int newWidth = (int) (image.getWidth() * scaleFactor);
        int newHeight = (int) (image.getHeight() * scaleFactor);

        BufferedImage scaledImage = new BufferedImage(newWidth, newHeight, BufferedImage.TYPE_INT_RGB);
        Graphics2D g2d = scaledImage.createGraphics();
        Image tmp = image.getScaledInstance(newWidth, newHeight, Image.SCALE_SMOOTH);
        g2d.drawImage(tmp, 0, 0, null);
        g2d.dispose();

        return scaledImage;
    }

    public static BufferedImage convertToRGB(float[][] grayArray) {
        int width = grayArray[0].length;
        int height = grayArray.length;
        BufferedImage rgbImage = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int gray = Math.min(255, Math.max(0, (int) (grayArray[y][x] * 255)));
                int rgb = (gray << 16) | (gray << 8) | gray;
                rgbImage.setRGB(x, y, rgb);
            }
        }
        
        return rgbImage;
    }

    public static float[][] convertToGrayscaleArray(BufferedImage rgbImage) {
        int width = rgbImage.getWidth();
        int height = rgbImage.getHeight();
        float[][] grayArray = new float[height][width];
        
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int rgb = rgbImage.getRGB(x, y);
                int red = (rgb >> 16) & 0xFF;
                grayArray[y][x] = red / 255.0f;
            }
        }
        
        return grayArray;
    }

    public static BufferedImage drawRectangles(float[][] grayImageArray, List<Rectangle> rectangles, float[] floatColorArray) {
        BufferedImage image = convertToRGB(grayImageArray);
        Graphics2D g2d = image.createGraphics();
        
        Color color = new Color(
            Math.min(255, Math.max(0, (int) (floatColorArray[0] * 255))),
            Math.min(255, Math.max(0, (int) (floatColorArray[1] * 255))),
            Math.min(255, Math.max(0, (int) (floatColorArray[2] * 255))),
            255
        );

        g2d.setColor(color);
        g2d.setStroke(new java.awt.BasicStroke(2));

        for (Rectangle rect : rectangles) {
            g2d.drawRect(rect.x, rect.y, rect.width, rect.height);
        }
        
        g2d.dispose();
        return image;
    }

    public static void save(BufferedImage image, String path) {
        BufferedImage rgbImage = new BufferedImage(
            image.getWidth(),
            image.getHeight(),
            BufferedImage.TYPE_INT_RGB);

        Graphics2D g = rgbImage.createGraphics();
        g.drawImage(image, 0, 0, null);
        g.dispose();

        String filePath = path.substring(0, path.lastIndexOf('/'));
        String fileName = path.substring(path.lastIndexOf('/') + 1);
        
        File directory = new File(filePath);
        if (!directory.exists()) {
            directory.mkdirs();
        }

        try {
            ImageIO.write(rgbImage, "png", new File(filePath + "/" + fileName));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}

