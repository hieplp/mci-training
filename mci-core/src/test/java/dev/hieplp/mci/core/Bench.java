package dev.hieplp.mci.core;

import java.lang.management.ManagementFactory;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

/**
 * Before/after benchmark for {@link MyBigNumber}. Not a JUnit test — run it
 * directly:
 *
 * <pre>{@code
 * cd mci-core
 * ./gradlew compileTestJava
 * java -cp build/classes/java/main:build/classes/java/test dev.hieplp.mci.core.Bench
 * }</pre>
 *
 * <p>Reports wall-clock time (median of {@value #RUNS} runs after
 * {@value #WARMUPS} warmups) and heap bytes allocated per call
 * ({@code com.sun.management.ThreadMXBean#getThreadAllocatedBytes}), for the
 * pre-refactor algorithm ({@link Old}, a verbatim copy) and the current one.</p>
 */
public final class Bench {

    private static final int WARMUPS = 3;
    private static final int RUNS = 7;
    private static final int[] SIZES = {1_000, 2_000, 10_000, 50_000, 100_000};
    private static final int LOGGING_SIZE = 2_000;

    private static final com.sun.management.ThreadMXBean THREAD_BEAN =
            (com.sun.management.ThreadMXBean) ManagementFactory.getThreadMXBean();

    private final MyBigNumber current = new MyBigNumber();

    public static void main(String[] args) {
        Logger jul = Logger.getLogger(MyBigNumber.class.getName());
        Level previousLevel = jul.getLevel();
        boolean previousParents = jul.getUseParentHandlers();
        Handler formatting = new FormattingHandler();
        try {
            jul.setUseParentHandlers(false);
            jul.setLevel(Level.WARNING); // INFO disabled: the hot path under test

            System.out.println("sanity: " + checkEquivalence());
            System.out.println();
            System.out.println("== INFO disabled ==");
            List<Case> quiet = new ArrayList<>();
            for (int n : SIZES) {
                String a = digits(n, 1);
                String b = digits(n, 2);
                quiet.add(new Case(n, "old sum()", () -> new Old().sum(a, b)));
                quiet.add(new Case(n, "new sum()", () -> new Bench().current.sum(a, b)));
                quiet.add(new Case(n, "old sumWithSteps()", () -> new Old().sumWithSteps(a, b)));
                quiet.add(new Case(n, "new sumWithSteps()", () -> new Bench().current.sumWithSteps(a, b)));
            }
            report(quiet);

            jul.addHandler(formatting);
            jul.setLevel(Level.ALL); // INFO enabled: the logging path
            System.out.println();
            System.out.println("== INFO enabled (records formatted by the handler into a discard sink) ==");
            List<Case> logging = new ArrayList<>();
            String a = digits(LOGGING_SIZE, 1);
            String b = digits(LOGGING_SIZE, 2);
            logging.add(new Case(LOGGING_SIZE, "old sum()", () -> new Old().sum(a, b)));
            logging.add(new Case(LOGGING_SIZE, "new sum()", () -> new Bench().current.sum(a, b)));
            logging.add(new Case(LOGGING_SIZE, "old sumWithSteps()", () -> new Old().sumWithSteps(a, b)));
            logging.add(new Case(LOGGING_SIZE, "new sumWithSteps()", () -> new Bench().current.sumWithSteps(a, b)));
            report(logging);
        } finally {
            jul.removeHandler(formatting);
            jul.setUseParentHandlers(previousParents);
            jul.setLevel(previousLevel);
        }
    }

    /** One measured variant at one operand length. */
    private record Case(int n, String variant, Runnable op) {
    }

    /**
     * Warms every case before measuring any of them — otherwise the first
     * measured variant pays for JIT compilation the others no longer do —
     * then prints one markdown row per case.
     */
    private static void report(List<Case> cases) {
        System.out.println("| n | variant | median ms | allocated B/call |");
        System.out.println("|---:|---|---:|---:|");
        for (Case c : cases) {
            for (int k = 0; k < WARMUPS; k++) {
                c.op.run();
            }
        }
        for (Case c : cases) {
            long[] times = new long[RUNS];
            long[] bytes = new long[RUNS];
            for (int k = 0; k < RUNS; k++) {
                long before = allocatedBytes();
                long start = System.nanoTime();
                c.op.run();
                times[k] = System.nanoTime() - start;
                bytes[k] = allocatedBytes() - before;
            }
            Arrays.sort(times);
            Arrays.sort(bytes);
            System.out.printf("| %d | %s | %.3f | %,d |%n",
                    c.n, c.variant, times[RUNS / 2] / 1e6, bytes[RUNS / 2]);
        }
    }

    /**
     * Sanity: the refactored sum must agree with the baseline on every size
     * measured above, and on random operands.
     */
    private static String checkEquivalence() {
        MyBigNumber current = new MyBigNumber();
        Old old = new Old();
        for (int n : new int[]{1, 2, 17, 1_000, 10_000, 50_000}) {
            for (String[] pair : new String[][]{
                    {digits(n, 1), digits(n, 2)},
                    {"9".repeat(n), "1"},
                    {"1", "9".repeat(n)},
                    {digits(n, 3), digits(Math.max(1, n / 3), 4)}}) {
                String expected = old.sum(pair[0], pair[1]);
                if (!expected.equals(current.sum(pair[0], pair[1]))
                        || !expected.equals(current.sumWithSteps(pair[0], pair[1]).sum())) {
                    throw new AssertionError("mismatch at n=" + n + ": " + pair[0] + " + " + pair[1]);
                }
            }
        }
        Random rnd = new Random(7);
        for (int k = 0; k < 50; k++) {
            String[] pair = {digits(1 + rnd.nextInt(5_000), rnd.nextInt()), digits(1 + rnd.nextInt(5_000), rnd.nextInt())};
            if (!old.sum(pair[0], pair[1]).equals(current.sum(pair[0], pair[1]))) {
                throw new AssertionError("mismatch on " + pair[0] + " + " + pair[1]);
            }
        }
        return "new sum() == old sum() for 6 sizes x 4 shapes + 50 random pairs";
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

    /**
     * Formats each record the way a real handler (e.g. {@code SimpleFormatter})
     * does, into a discard sink — so the INFO-enabled numbers include message
     * formatting, not just record creation.
     */
    private static final class FormattingHandler extends Handler {

        private long chars;

        @Override
        public void publish(LogRecord record) {
            Object[] params = record.getParameters();
            String text = params == null || params.length == 0
                    ? record.getMessage()
                    : MessageFormat.format(record.getMessage(), params);
            chars += text.length();
        }

        @Override
        public void flush() {
        }

        @Override
        public void close() {
        }
    }

    /**
     * {@code MyBigNumber} as it was before the optimization: {@code sum()}
     * delegates to {@code sumWithSteps()}, every column builds a
     * {@code StringBuilder} snapshot, a {@code Step} and an {@code ArrayList}
     * entry, and the logger name is the one the bench configures.
     */
    private static final class Old {

        private static final System.Logger LOG = System.getLogger(MyBigNumber.class.getName());

        String sum(String stn1, String stn2) {
            return sumWithSteps(stn1, stn2).sum();
        }

        MyBigNumber.SumResult sumWithSteps(String stn1, String stn2) {
            LOG.log(System.Logger.Level.INFO, "Input: stn1=\"{0}\", stn2=\"{1}\"", stn1, stn2);

            NumberStrings.requireDigits(stn1, "stn1");
            NumberStrings.requireDigits(stn2, "stn2");
            stn1 = NumberStrings.stripLeadingZeros(stn1);
            stn2 = NumberStrings.stripLeadingZeros(stn2);

            StringBuilder result = new StringBuilder(Math.max(stn1.length(), stn2.length()) + 1);
            int carry = 0;
            int stepIndex = 0;
            List<MyBigNumber.Step> steps = new ArrayList<>();

            int i = stn1.length() - 1;
            int j = stn2.length() - 1;

            while (i >= 0 || j >= 0 || carry > 0) {
                int firstDigit = i >= 0 ? stn1.charAt(i--) - '0' : 0;
                int secondDigit = j >= 0 ? stn2.charAt(j--) - '0' : 0;

                int carryIn = carry;
                int columnTotal = firstDigit + secondDigit + carryIn;
                int resultDigit = columnTotal % 10;
                carry = columnTotal / 10;
                result.append(resultDigit);

                String resultSoFar = new StringBuilder(result).reverse().toString();
                MyBigNumber.Step step = new MyBigNumber.Step(++stepIndex, firstDigit, secondDigit,
                        carryIn, columnTotal, resultDigit, carry, resultSoFar);
                LOG.log(System.Logger.Level.INFO, "{0}", step);
                steps.add(step);
            }

            String sum = result.reverse().toString();
            LOG.log(System.Logger.Level.INFO, "Final: {0} + {1} = {2}", stn1, stn2, sum);

            return new MyBigNumber.SumResult(sum, List.copyOf(steps));
        }
    }

    private Bench() {
    }
}
