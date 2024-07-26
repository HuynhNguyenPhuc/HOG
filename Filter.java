public interface Filter {
    public static final float[][] kernelX = {
        {0, 0, 0},
        {0, 1, 0},
        {0, 0, 0}
    };
    public static final float[][] kernelY = {
        {0, 0, 0},
        {0, -1, 0},
        {0, 0, 0}
    };
}
