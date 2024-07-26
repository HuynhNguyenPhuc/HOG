import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class RectangleHelper {
    public static float IoU(Rectangle rect1, Rectangle rect2) {
        int x1 = Math.max(rect1.x, rect2.x);
        int y1 = Math.max(rect1.y, rect2.y);
        int x2 = Math.min(rect1.x + rect1.width, rect2.x + rect2.width);
        int y2 = Math.min(rect1.y + rect1.height, rect2.y + rect2.height);

        int overlapWidth = Math.max(0, x2 - x1);
        int overlapHeight = Math.max(0, y2 - y1);

        float overlapArea = overlapWidth * overlapHeight;
        float unionArea = rect1.width * rect1.height + rect2.width * rect2.height - overlapArea;

        return overlapArea / unionArea;
    }

    public static float RegularizedIoU(Rectangle rect1, Rectangle rect2) {
        int rect1Area = rect1.width * rect1.height;
        int rect2Area = rect2.width * rect2.height;

        int aL = Math.max(rect1Area, rect2Area);
        int aS = Math.min(rect1Area, rect2Area);

        float threshold = ((float) aS) / (aL + aS);
        
        float lambda = threshold / 2.0f + threshold / 2.0f * ((float) Math.random() - 0.5f);

        int x1 = Math.max(rect1.x, rect2.x);
        int y1 = Math.max(rect1.y, rect2.y);
        int x2 = Math.min(rect1.x + rect1.width, rect2.x + rect2.width);
        int y2 = Math.min(rect1.y + rect1.height, rect2.y + rect2.height);

        int overlapWidth = Math.max(0, x2 - x1);
        int overlapHeight = Math.max(0, y2 - y1);

        float overlapArea = overlapWidth * overlapHeight;

        return (overlapArea / 2.0f) / (lambda * aL + (1 - lambda) * aS - overlapArea / 2.0f);
    }

    public static List<Rectangle> nonMaximumSuppression(List<Rectangle> rectangles, List<Float> weights, float overlapThreshold) {
        List<Rectangle> finalRectangles = new ArrayList<>();

        while (!rectangles.isEmpty()){
            int maxIndex = 0;
            for (int i = 1; i < weights.size(); i++){
                if (weights.get(i) > weights.get(maxIndex)){
                    maxIndex = i;
                }
            }

            Rectangle bestRectangle = rectangles.get(maxIndex);
            finalRectangles.add(bestRectangle);

            List<Rectangle> remainingRectangles = new ArrayList<>();
            List<Float> remainingWeights = new ArrayList<>();

            for (int i = 0; i < rectangles.size(); i++) {
                if (i == maxIndex) continue;

                Rectangle rect = rectangles.get(i);
                if (RectangleHelper.IoU(bestRectangle, rect) < overlapThreshold) {
                    remainingRectangles.add(rect);
                    remainingWeights.add(weights.get(i));
                }
            }

            rectangles = remainingRectangles;
            weights = remainingWeights;
        }
        return finalRectangles;
    }

    public static List<Rectangle> proposedNMS(List<Rectangle> rectangles, List<Float> weights, float overlapThreshold) {
        List<Rectangle> finalRectangles = new ArrayList<>();

        while (!rectangles.isEmpty()){
            int maxIndex = 0;
            for (int i = 1; i < weights.size(); i++){
                if (weights.get(i) > weights.get(maxIndex)){
                    maxIndex = i;
                }
            }

            Rectangle bestRectangle = rectangles.get(maxIndex);
            finalRectangles.add(bestRectangle);

            List<Rectangle> remainingRectangles = new ArrayList<>();
            List<Float> remainingWeights = new ArrayList<>();

            for (int i = 0; i < rectangles.size(); i++) {
                if (i == maxIndex) continue;

                Rectangle rect = rectangles.get(i);

                float threshold = overlapThreshold * (1.0f - (weights.get(maxIndex) - weights.get(i)));

                if (RectangleHelper.RegularizedIoU(bestRectangle, rect) < threshold) {
                    remainingRectangles.add(rect);
                    remainingWeights.add(weights.get(i));
                }
            }

            rectangles = remainingRectangles;
            weights = remainingWeights;
        }
        return finalRectangles;
    }

    public static List<Rectangle> merge(List<Rectangle> rectangles) {
        boolean hasMerged;
        do{
            hasMerged = false;
            List<Rectangle> mergedRectangles = new ArrayList<>();
            boolean[] merged = new boolean[rectangles.size()];

            for (int i = 0; i < merged.length; i++) {
                merged[i] = false;
            }

            for (int i = 0; i < rectangles.size(); i++) {
                if (merged[i]) continue;
                Rectangle rect1 = rectangles.get(i);
                int xMin = rect1.x;
                int yMin = rect1.y;
                int xMax = rect1.x + rect1.width;
                int yMax = rect1.y + rect1.height;
        
                for (int j = i + 1; j < rectangles.size(); j++) {
                    if (merged[j]) continue;
                    Rectangle rect2 = rectangles.get(j);
                    if (intersects(rect1, rect2)) {
                        xMin = Math.min(xMin, rect2.x);
                        yMin = Math.min(yMin, rect2.y);
                        xMax = Math.max(xMax, rect2.x + rect2.width);
                        yMax = Math.max(yMax, rect2.y + rect2.height);
                        merged[j] = true;
                        hasMerged = true;
                    }
                }
                mergedRectangles.add(new Rectangle(xMin, yMin, xMax - xMin, yMax - yMin));
                merged[i] = true;
            }

            rectangles = mergedRectangles;
        } while (hasMerged);
    
        return rectangles;
    }

    public static boolean intersects(Rectangle r1, Rectangle r2) {
        int r1x = r1.x;
        int r1y = r1.y;
        int r1w = r1.width;
        int r1h = r1.height;
        int r2x = r2.x;
        int r2y = r2.y;
        int r2w = r2.width;
        int r2h = r2.height;
    
        if (r1w <= 0 || r1h <= 0 || r2w <= 0 || r2h <= 0) {
            return false;
        }
    
        return (r1x <= r2x + r2w &&
                r1x + r1w >= r2x &&
                r1y <= r2y + r2h &&
                r1y + r1h >= r2y);
    }

    public static boolean areSimilar(Rectangle r1, Rectangle r2, double eps) {
        double deltaX = Math.abs(r1.x - r2.x);
        double deltaY = Math.abs(r1.y - r2.y);
        double deltaW = Math.abs(r1.width - r2.width);
        double deltaH = Math.abs(r1.height - r2.height);
    
        double maxSide1 = Math.max(r1.width, r1.height);
        double maxSide2 = Math.max(r2.width, r2.height);
    
        return deltaX <= eps * maxSide1 && deltaY <= eps * maxSide1 &&
               deltaW <= eps * maxSide2 && deltaH <= eps * maxSide2;
    }

    /*Use disjoint set, reference here: https://en.wikipedia.org/wiki/Disjoint-set_data_structure */
    public static int partition(List<Rectangle> rects, List<Integer> labels, double eps) {
        int n = rects.size();
        DisjointSet ds = new DisjointSet(n);

        for (int i = 0; i < n; i++) {
            for (int j = i + 1; j < n; j++) {
                if (intersects(rects.get(i), rects.get(j))){
                    ds.union(i, j);
                }
            }
        }

        Map<Integer, Integer> clusterMap = new HashMap<>();
        int classCount = 0;
        labels.clear();

        for (int i = 0; i < n; i++) {
            int root = ds.find(i);
            if (!clusterMap.containsKey(root)) {
                clusterMap.put(root, classCount++);
            }
            labels.add(clusterMap.get(root));
        }

        return classCount;
    }

    public static Rectangle computeAverageRectangle(List<Rectangle> cluster) {
        int sumX = 0, sumY = 0, sumW = 0, sumH = 0;
    
        for (Rectangle rect : cluster) {
            sumX += rect.x;
            sumY += rect.y;
            sumW += rect.width;
            sumH += rect.height;
        }
    
        int n = cluster.size();
        return new Rectangle(sumX / n, sumY / n, sumW / n, sumH / n);
    }

    /* Reference: https://docs.opencv.org/2.4/modules/objdetect/doc/cascade_classification.html#void%20groupRectangles(vector%3CRect%3E&%20rectList,%20int%20groupThreshold,%20double%20eps) */
    public static List<Rectangle> groupRectangles(List<Rectangle> rectList, int groupThreshold, double eps) {
        List<Integer> labels = new ArrayList<>();
        int numClasses = partition(rectList, labels, eps);

        Map<Integer, List<Rectangle>> clusters = new HashMap<>();
        for (int i = 0; i < numClasses; i++) {
            clusters.put(i, new ArrayList<>());
        }

        for (int i = 0; i < rectList.size(); i++) {
            clusters.get(labels.get(i)).add(rectList.get(i));
        }

        List<Rectangle> result = new ArrayList<>();
        for (List<Rectangle> cluster : clusters.values()) {
            if (cluster.size() > groupThreshold) {
                result.add(computeAverageRectangle(cluster));
            }
        }

        return result;
    }
    
    
    public static void print(List<Rectangle> rectangles){
        for (Rectangle rect : rectangles){
            System.out.println("Rectangle: " + "<Top-Left: " + "(" + rect.x + ", " + rect.y + ")>, " + "<Width: " + rect.width + ">, " + "<Height: " + rect.height + ">");
        }
    }
}
