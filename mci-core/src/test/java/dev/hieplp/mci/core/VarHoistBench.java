package dev.hieplp.mci.core;

import java.lang.management.ManagementFactory;
import java.math.BigInteger;
import java.util.Arrays;
import java.util.Random;

/**
 * Does declaring the loop-local {@code int}s inside the column-addition
 * {@code while} body cost anything versus hoisting them above the loop?
 * Not a JUnit test — run it directly:
 *
 * <pre>{@code
 * cd mci-core
 * ./gradlew compileTestJava
 * java -cp build/classes/java/main:build/classes/java/test dev.hieplp.mci.core.VarHoistBench
 * }</pre>
 *
 * <p>Both variants are the real hot loop of {@link MyBigNumber#sum(String, String)}
 * (arithmetic only — no validation, stripping or logging), differing solely in
 * where {@code firstDigit}, {@code secondDigit}, {@code carryIn},
 * {@code columnTotal} and {@code resultDigit} are declared. Measurements are
 * interleaved A,B,A,B,… so drift, JIT and GC bias hit both alike, and are
 * reported as a Welch two-sample t-test over wall-clock nanoseconds.</p>
 */
public final class VarHoistBench {

    private static final int DIGITS = 10_000;
    private static final int WARMUPS = 2_000;
    private static final int ITERATIONS = 500;

    private static final com.sun.management.ThreadMXBean THREAD_BEAN =
            (com.sun.management.ThreadMXBean) ManagementFactory.getThreadMXBean();

    public static void main(String[] args) {
        String a = digits(DIGITS, 1);
        String b = digits(DIGITS, 2);

        System.out.println("sanity: " + checkEquivalence());
        System.out.println();

        for (int k = 0; k < WARMUPS; k++) {
            sumInside(a, b);
            sumOutside(a, b);
        }

        long[] insideNs = new long[ITERATIONS];
        long[] outsideNs = new long[ITERATIONS];
        long[] insideBytes = new long[ITERATIONS];
        long[] outsideBytes = new long[ITERATIONS];
        for (int k = 0; k < ITERATIONS; k++) {
            time(VarHoistBench::sumInside, a, b, insideNs, insideBytes, k);
            time(VarHoistBench::sumOutside, a, b, outsideNs, outsideBytes, k);
        }

        Stats inside = Stats.of(insideNs, insideBytes);
        Stats outside = Stats.of(outsideNs, outsideBytes);

        System.out.println("| variant | n | mean ms | stddev ms | median ms | min ms | max ms | allocated B/call (median) |");
        System.out.println("|---|---:|---:|---:|---:|---:|---:|---:|");
        System.out.println(inside.row("inside loop "));
        System.out.println(outside.row("outside loop"));
        System.out.println();

        double variance = inside.variance() / inside.n() + outside.variance() / outside.n();
        double t = variance == 0 ? 0 : (inside.mean() - outside.mean()) / Math.sqrt(variance);
        double df = variance == 0 ? 0 : variance * variance
                / (Math.pow(inside.variance() / inside.n(), 2) / (inside.n() - 1)
                + Math.pow(outside.variance() / outside.n(), 2) / (outside.n() - 1));
        double p = tTwoTailedP(t, df);

        System.out.printf("mean difference (inside - outside): %+.6f ms%n", (inside.mean() - outside.mean()) / 1e6);
        System.out.printf("Welch t = %.3f, df = %.1f, p = %.4f -> %s at p<0.05%n",
                t, df, p, p < 0.05 ? "SIGNIFICANT" : "not significant");
    }

    /**
     * {@link MyBigNumber#sum(String, String)}'s arithmetic loop, current style:
     * the five column ints are declared inside the {@code while} body.
     */
    private static String sumInside(String stn1, String stn2) {
        StringBuilder result = new StringBuilder(Math.max(stn1.length(), stn2.length()) + 1);
        int carry = 0;

        int i = stn1.length() - 1;
        int j = stn2.length() - 1;

        while (i >= 0 || j >= 0 || carry > 0) {
            int firstDigit = i >= 0 ? stn1.charAt(i--) - '0' : 0;
            int secondDigit = j >= 0 ? stn2.charAt(j--) - '0' : 0;

            int carryIn = carry;
            int columnTotal = firstDigit + secondDigit + carryIn;
            int resultDigit = columnTotal % 10;
            carry = columnTotal / 10;
            result.append((char) ('0' + resultDigit));
        }

        return result.reverse().toString();
    }

    /**
     * Identical to {@link #sumInside} except the five ints are declared once
     * before the loop and assigned inside it.
     */
    private static String sumOutside(String stn1, String stn2) {
        int firstDigit;
        int secondDigit;
        int carryIn;
        int columnTotal;
        int resultDigit;

        StringBuilder result = new StringBuilder(Math.max(stn1.length(), stn2.length()) + 1);
        int carry = 0;

        int i = stn1.length() - 1;
        int j = stn2.length() - 1;

        while (i >= 0 || j >= 0 || carry > 0) {
            firstDigit = i >= 0 ? stn1.charAt(i--) - '0' : 0;
            secondDigit = j >= 0 ? stn2.charAt(j--) - '0' : 0;

            carryIn = carry;
            columnTotal = firstDigit + secondDigit + carryIn;
            resultDigit = columnTotal % 10;
            carry = columnTotal / 10;
            result.append((char) ('0' + resultDigit));
        }

        return result.reverse().toString();
    }

    /** One variant's parameter type — lets both methods be measured by the same code. */
    private interface Op {
        String run(String a, String b);
    }

    /** One interleaved measurement: nanoseconds and allocated bytes around a single call. */
    private static void time(Op op, String a, String b, long[] ns, long[] bytes, int k) {
        long before = allocatedBytes();
        long start = System.nanoTime();
        op.run(a, b);
        ns[k] = System.nanoTime() - start;
        bytes[k] = allocatedBytes() - before;
    }

    /** Summary of one variant's samples. */
    private record Stats(int n, double mean, double stddev, double median, double min, double max, long bytesMedian) {

        static Stats of(long[] ns, long[] bytes) {
            long[] sorted = ns.clone();
            Arrays.sort(sorted);
            long[] sortedBytes = bytes.clone();
            Arrays.sort(sortedBytes);
            double mean = Arrays.stream(ns).average().orElseThrow();
            double variance = 0;
            for (long v : ns) {
                variance += (v - mean) * (v - mean);
            }
            variance /= ns.length - 1;
            return new Stats(ns.length, mean, Math.sqrt(variance), sorted[ns.length / 2],
                    sorted[0], sorted[sorted.length - 1], sortedBytes[sortedBytes.length / 2]);
        }

        double variance() {
            return stddev * stddev;
        }

        String row(String variant) {
            return String.format("| %s | %d | %.6f | %.6f | %.6f | %.6f | %.6f | %,d |",
                    variant, n, mean / 1e6, stddev / 1e6, median / 1e6, min / 1e6, max / 1e6, bytesMedian);
        }
    }

    /**
     * Sanity: both variants agree with each other and with {@link BigInteger}
     * on several operand shapes — equal length, the all-nines carry cascade,
     * and a 1:3 length ratio.
     */
    private static String checkEquivalence() {
        int shapes = 0;
        for (int n : new int[]{1, 2, 17, 1_000, 10_000}) {
            for (String[] pair : new String[][]{
                    {digits(n, 1), digits(n, 2)},
                    {"9".repeat(n), "1"},
                    {"1", "9".repeat(n)},
                    {digits(n, 3), digits(Math.max(1, n / 3), 4)}}) {
                String expected = new BigInteger(pair[0]).add(new BigInteger(pair[1])).toString();
                String inside = sumInside(pair[0], pair[1]);
                String outside = sumOutside(pair[0], pair[1]);
                if (!expected.equals(inside) || !expected.equals(outside)) {
                    throw new AssertionError("mismatch at n=" + n + ": "
                            + pair[0] + " + " + pair[1]);
                }
                shapes++;
            }
        }
        return "inside == outside == BigInteger for 5 sizes x 4 shapes (" + shapes + " cases)";
    }

    private static long allocatedBytes() {
        return THREAD_BEAN.getThreadAllocatedBytes(Thread.currentThread().threadId());
    }

    /** Fixed-width digit string, no leading zero (seed keeps runs reproducible). */
    private static String digits(int len, int seed) {
        Random rnd = new Random(seed);
        StringBuilder sb = new StringBuilder(len);
        sb.append((char) ('1' + rnd.nextInt(9)));
        for (int i = 1; i < len; i++) {
            sb.append((char) ('0' + rnd.nextInt(10)));
        }
        return sb.toString();
    }

    /** Two-tailed p-value of Student's t with {@code df} degrees of freedom. */
    private static double tTwoTailedP(double t, double df) {
        if (df <= 0) {
            return 1;
        }
        return betaRegularized(df / 2, 0.5, df / (df + t * t));
    }

    /** Regularized incomplete beta {@code I_x(a,b)}, continued fraction (Lentz). */
    private static double betaRegularized(double a, double b, double x) {
        if (x <= 0) {
            return 0;
        }
        if (x >= 1) {
            return 1;
        }
        double front = Math.exp(logGamma(a + b) - logGamma(a) - logGamma(b)
                + a * Math.log(x) + b * Math.log1p(-x));
        return x < (a + 1) / (a + b + 2)
                ? front * betaFraction(a, b, x) / a
                : 1 - front * betaFraction(b, a, 1 - x) / b;
    }

    private static double betaFraction(double a, double b, double x) {
        double qab = a + b;
        double qap = a + 1;
        double qam = a - 1;
        double c = 1;
        double d = 1 - qab * x / qap;
        if (Math.abs(d) < 1e-30) {
            d = 1e-30;
        }
        d = 1 / d;
        double h = d;
        for (int m = 1; m <= 200; m++) {
            int m2 = 2 * m;
            double aa = m * (b - m) * x / ((qam + m2) * (a + m2));
            d = 1 + aa * d;
            if (Math.abs(d) < 1e-30) {
                d = 1e-30;
            }
            c = 1 + aa / c;
            if (Math.abs(c) < 1e-30) {
                c = 1e-30;
            }
            d = 1 / d;
            h *= d * c;
            aa = -(a + m) * (qab + m) * x / ((a + m2) * (qap + m2));
            d = 1 + aa * d;
            if (Math.abs(d) < 1e-30) {
                d = 1e-30;
            }
            c = 1 + aa / c;
            if (Math.abs(c) < 1e-30) {
                c = 1e-30;
            }
            d = 1 / d;
            double del = d * c;
            h *= del;
            if (Math.abs(del - 1) < 1e-12) {
                break;
            }
        }
        return h;
    }

    /** Lanczos log-gamma. */
    private static double logGamma(double x) {
        double[] g = {676.5203681218851, -1259.1392167224028, 771.32342877765313,
                -176.61502916214059, 12.507343278686905, -0.13857109526572012,
                9.9843695780195716e-6, 1.5056327351493116e-7};
        if (x < 0.5) {
            return Math.log(Math.PI / Math.sin(Math.PI * x)) - logGamma(1 - x);
        }
        x -= 1;
        double a = 0.99999999999980993;
        for (int i = 0; i < g.length; i++) {
            a += g[i] / (x + i + 1);
        }
        double t = x + g.length - 0.5;
        return 0.5 * Math.log(2 * Math.PI) + (x + 0.5) * Math.log(t) - t + Math.log(a);
    }

    private VarHoistBench() {
    }
}
