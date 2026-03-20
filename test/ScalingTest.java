public class ScalingTest {
    public static void main(String[] args) {
        float minVal = 100f;
        float maxVal = 3200f;

        System.out.println("Logarithmic ISO Scaling Test (5000px range):");
        for (float scroll = 0; scroll <= 5000; scroll += 1000) {
            float val = (float) (minVal * Math.pow(maxVal / minVal, scroll / 5000.0f));
            System.out.printf("Scroll: %f -> ISO: %.2f\n", scroll, val);
        }

        System.out.println("\nLinear Shutter Scaling Test (3000px/sec):");
        minVal = 0.001f;
        float pixelsPerSecond = 3000.0f;
        for (float scroll = 0; scroll <= 9000; scroll += 3000) {
            float val = minVal + (scroll / pixelsPerSecond);
            System.out.printf("Scroll: %f -> Shutter: %.4f s (%.1f turns approx if 1000px width)\n",
                scroll, val, scroll/1000.0f);
        }
    }
}
