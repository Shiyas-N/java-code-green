import java.util.*;

public class EnergyTestProgram {

    // Factorial using recursion
    public static long factorial(int n) {
        if (n <= 1) return 1;
        return n * factorial(n - 1);
    }

    // Fibonacci using recursion (inefficient for profiling)
    public static long fibonacci(int n) {
        if (n <= 1) return n;
        return fibonacci(n - 1) + fibonacci(n - 2);
    }

    // Bubble Sort
    public static void bubbleSort(int[] arr) {
        int n = arr.length;
        for (int i = 0; i < n - 1; i++) {
            for (int j = 0; j < n - i - 1; j++) {
                if (arr[j] > arr[j + 1]) {
                    int temp = arr[j];
                    arr[j] = arr[j + 1];
                    arr[j + 1] = temp;
                }
            }
        }
    }

    // Matrix multiplication
    public static int[][] multiplyMatrices(int[][] a, int[][] b) {
        int n = a.length;
        int[][] result = new int[n][n];
        for (int i = 0; i < n; i++) {
            for (int j = 0; j < n; j++) {
                for (int k = 0; k < n; k++) {
                    result[i][j] += a[i][k] * b[k][j];
                }
            }
        }
        return result;
    }

    // Prime number checker
    public static boolean isPrime(int num) {
        if (num < 2) return false;
        for (int i = 2; i <= Math.sqrt(num); i++) {
            if (num % i == 0) return false;
        }
        return true;
    }

    // Generate primes
    public static List<Integer> generatePrimes(int limit) {
        List<Integer> primes = new ArrayList<>();
        for (int i = 2; i <= limit; i++) {
            if (isPrime(i)) primes.add(i);
        }
        return primes;
    }

    // Intensive loop calculation
    public static long heavyComputation(int n) {
        long sum = 0;
        for (int i = 1; i <= n; i++) {
            sum += (long) Math.pow(i, 3) % 1234567;
        }
        return sum;
    }

    // Main method to run everything
    public static void main(String[] args) {
        System.out.println("=== Energy Test Program Started ===");

        // Factorial & Fibonacci
        System.out.println("Factorial(15): " + factorial(15));
        System.out.println("Fibonacci(20): " + fibonacci(20));

        // Sorting
        int[] arr = {9, 5, 1, 8, 3, 7, 2, 6, 4};
        bubbleSort(arr);
        System.out.println("Sorted Array: " + Arrays.toString(arr));

        // Matrix multiplication
        int[][] a = {{1, 2}, {3, 4}};
        int[][] b = {{5, 6}, {7, 8}};
        int[][] result = multiplyMatrices(a, b);
        System.out.println("Matrix Multiplication Result: " + Arrays.deepToString(result));

        // Prime generation
        List<Integer> primes = generatePrimes(50);
        System.out.println("Primes up to 50: " + primes);

        // Heavy computation
        System.out.println("Heavy Computation Result: " + heavyComputation(1000000));

        System.out.println("=== Energy Test Program Finished ===");
    }
}
