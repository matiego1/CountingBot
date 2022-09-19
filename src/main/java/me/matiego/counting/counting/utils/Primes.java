package me.matiego.counting.counting.utils;

import org.jetbrains.annotations.Range;

import java.util.Arrays;

public class Primes {
    private static final boolean[] primes;
    private static final int MAX_PRIME = 1_000_000;

    static {
        primes = new boolean[MAX_PRIME + 1];
        Arrays.fill(primes, true);
        primes[0] = primes[1] = false;
        for (int i = 2; i * i <= MAX_PRIME; i++) {
            if (primes[i]) {
                for (int j = i * i; j <= MAX_PRIME; j += i) {
                    primes[j] = false;
                }
            }
        }
    }

    /**
     * Checks whether the given number is a prime number. <br>
     * If the number is greater than {@link #MAX_PRIME}, it will have a complexity of O(sqrt(n)), otherwise O(1).
     * @param number the number
     * @return {@code true} or {@code false}
     */
    public static boolean isPrime(@Range(from = 2, to = Integer.MAX_VALUE) int number) {
        if (number <= MAX_PRIME) return primes[number];
        for (int i = 2; i * i < number; i++) {
            if (number % i == 0) return false;
        }
        return true;
    }

    /**
     * Checks whether the given number is a semi-prime number.
     * @param number the number
     * @return {@code true} or {@code false}
     */
    public static boolean isSemiPrime(int number) {
        if (isPrime(number)) return false;
        for (int i = 2; i * i <= number; i++) {
            if (number % i == 0 && isPrime(i) && isPrime(number / i)) return true;
        }
        return false;
    }

    /**
     * Checks whether the given number is a sphenic number.
     * @param number the number
     * @return {@code true} or {@code false}
     */
    public static boolean isSphenic(int number) {
        if (isPrime(number)) return false;
        for (int i = 2; i * i <= number; i++) {
            if (number % i == 0 && isPrime(i) && isSemiPrimeDifferent(number / i, i)) return true;
        }
        return false;
    }

    /**
     * Checks whether the given number is a semi-prime number and: <br>
     * - divisors are different from each other <br>
     * - divisors are different from {@code different} param
     * @param number the number
     * @param different the number from which the divisors must be different
     * @return {@code true} or {@code false}
     */
    private static boolean isSemiPrimeDifferent(int number, int different) {
        if (isPrime(number)) return false;
        for (int i = 2; i * i < number; i++) {
            if (number % i == 0 && isPrime(i) && isPrime(number / i) && i != different && number / i != different) return true;
        }
        return false;
    }
}
