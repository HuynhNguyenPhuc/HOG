import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.List;

public class RectangleHelper {
    public static float computeOverlap(Rectangle rect1, Rectangle rect2) {
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
                if (RectangleHelper.computeOverlap(bestRectangle, rect) < overlapThreshold) {
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
    
    public static void print(List<Rectangle> rectangles){
        for (Rectangle rect : rectangles){
            System.out.println("Rectangle: " + "<Top-Left: " + "(" + rect.x + ", " + rect.y + ")>, " + "<Width: " + rect.width + ">, " + "<Height: " + rect.height + ">");
        }
    }
}
