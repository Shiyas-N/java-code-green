import java.util.*;

public class EnergyTestApp {

    public static void main(String[] args) {
        System.out.println("Starting Energy Consumption Test...");

        // 1. Basic operations
        simpleMath();

        // 2. Nested method calls (for calltree)
        processData();

        // 3. Recursion (for method evolution + calltree depth)
        int factResult = factorial(10);
        System.out.println("Factorial(10) = " + factResult);

        // 4. Loops and collections
        collectionTest();

        // 5. Simulated workload (runtime sampling effect)
        simulateWorkload();

        // 6. Sorting algorithm (good for method-based consumption tracking)
        int[] arr = {10, 5, 3, 12, 7, 9, 15};
        bubbleSort(arr);
        System.out.println("Sorted Array: " + Arrays.toString(arr));

        System.out.println("Energy Consumption Test Finished!");
    }

    // Simple math
    public static void simpleMath() {
        double result = 0;
        for (int i = 1; i <= 100000; i++) {
            result += Math.sqrt(i) * Math.sin(i);
        }
        System.out.println("Simple Math Result = " + result);
    }

    // Method with nested calls
    public static void processData() {
        int[] data = generateData(1000);
        int sum = sumArray(data);
        System.out.println("Sum of Data = " + sum);
    }

    private static int[] generateData(int size) {
        Random rand = new Random();
        int[] arr = new int[size];
        for (int i = 0; i < size; i++) {
            arr[i] = rand.nextInt(1000);
        }
        return arr;
    }

    private static int sumArray(int[] arr) {
        int sum = 0;
        for (int num : arr) {
            sum += num;
        }
        return sum;
    }

    // Recursive method
    public static int factorial(int n) {
        if (n <= 1) return 1;
        return n * factorial(n - 1);
    }

    // Collections
    public static void collectionTest() {
        List<String> names = new ArrayList<>();
        names.add("Shiyas");
        names.add("Energy");
        names.add("JoularJX");
        names.add("Test");

        for (String name : names) {
            System.out.println("Processing: " + name.toUpperCase());
        }

        Collections.sort(names);
        System.out.println("Sorted Names: " + names);
    }

    // Simulated workload
    public static void simulateWorkload() {
        try {
            for (int i = 0; i < 5; i++) {
                Thread.sleep(500); // sleep to show runtime sampling effect
                System.out.println("Simulating workload... cycle " + (i + 1));
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    // Sorting algorithm
    public static void bubbleSort(int[] arr) {
        boolean swapped;
        int n = arr.length;
        do {
            swapped = false;
            for (int i = 1; i < n; i++) {
                if (arr[i - 1] > arr[i]) {
                    int temp = arr[i];
                    arr[i] = arr[i - 1];
                    arr[i - 1] = temp;
                    swapped = true;
                }
            }
            n--;
        } while (swapped);
    }
}




