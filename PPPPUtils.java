package pppp.g1;

import pppp.sim.Point;

public class PPPPUtils {
    /**
     * Compute Euclidean distance between two points in a plane.
     * @param a
     * @param b
     * @return distance between a and b
     */
    public static double distance(Point a, Point b) {
        double dx = a.x - b.x;
        double dy = a.y - b.y;
        return Math.sqrt(dx * dx + dy * dy);
    }

    /**
     * Select the kth largest element in the array.
     * @param input_arr
     * @param k
     * @return kth largest element
     */
    public static double quickSelect(double[] input_arr, int k) {
        if (input_arr == null)
            throw new Error();
        if(input_arr.length == k) {
            double max = Double.MIN_VALUE;
            for(int i = 0; i < input_arr.length; ++i)
            if(max < input_arr[i])
                max = input_arr[i];
            return max;
        }
        if(input_arr.length < k)
            throw new Error();

        // copy to new array
        double[] arr = new double[input_arr.length];
        for (int i = 0; i < arr.length; ++i)
            arr[i] = input_arr[i];

        int from = 0, to = arr.length - 1;

        // if from == to we reached the kth element
        while (from < to) {
            int r = from, w = to;
            double mid = arr[(r + w) / 2];

            // stop if the reader and writer meets
            while (r < w) {

                if (arr[r] >= mid) { // put the large values at the end
                    double tmp = arr[w];
                    arr[w] = arr[r];
                    arr[r] = tmp;
                    w--;
                } else { // the value is smaller than the pivot, skip
                    r++;
                }
            }

            // if we stepped up (r++) we need to step one down
            if (arr[r] > mid)
                r--;

            // the r pointer is on the end of the first k elements
            if (k <= r) {
                to = r;
            } else {
                from = r + 1;
            }
        }

        return arr[k];
    }
}
