public class SobelFilter implements Filter{
    public static final float[][] kernelX = {
        {-1, 0, 1},
        {-2, 0, 2},
        {-1, 0, 1}
    };

    public static final float[][] kernelY = {
        {-1, -2, -1},
        {0, 0, 0},
        {1, 2, 1}
    };
}
