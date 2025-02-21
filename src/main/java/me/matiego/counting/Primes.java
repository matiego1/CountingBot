package me.matiego.counting;

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

    // O(1) for numbers <= MAX_PRIME; O(sqrt(n)) otherwise
    public static boolean isPrime(long number) {
        if (number <= MAX_PRIME) return primes[(int) number];
        for (long i = 2; i * i < number; i++) {
            if (number % i == 0) return false;
        }
        return true;
    }

    public static boolean isSemiPrime(long number) {
        if (isPrime(number)) return false;
        for (long i = 2; i * i <= number; i++) {
            if (number % i == 0 && isPrime(i) && isPrime(number / i)) return true;
        }
        return false;
    }

    public static boolean isSphenic(long number) {
        if (isPrime(number)) return false;
        for (long i = 2; i * i <= number; i++) {
            if (number % i == 0 && isPrime(i) && isSemiPrimeDifferent(number / i, i)) return true;
        }
        return false;
    }

    private static boolean isSemiPrimeDifferent(long number, long different) {
        if (isPrime(number)) return false;
        for (long i = 2; i * i < number; i++) {
            if (number % i != 0) continue;
            if (!isPrime(i)) continue;
            if (!isPrime(number / i)) continue;
            if (i == different) continue;
            if (number / i == different) continue;
            return true;
        }
        return false;
    }
}
