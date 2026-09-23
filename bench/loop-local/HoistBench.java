import dev.hieplp.mci.core.MyBigNumber;
import java.lang.management.GarbageCollectorMXBean;
import java.lang.management.ManagementFactory;
import java.math.BigInteger;
import java.util.logging.Level;
import java.util.logging.Logger;

/** Times whichever MyBigNumber is on the classpath. One variant per JVM. */
public final class HoistBench {
    public static void main(String[] args) {
        String mode = args[0];
        int digits = Integer.parseInt(args[1]);
        int calls = Integer.parseInt(args[2]);
        boolean logOff = args.length > 3 && "off".equals(args[3]);
        if (logOff) {
            Logger.getLogger("dev.hieplp.mci.core").setLevel(Level.OFF);
            Logger.getLogger("").setLevel(Level.OFF);
        }

        String a = "9".repeat(digits);
        String b = "1".repeat(digits);
        MyBigNumber adder = new MyBigNumber();
        String once = adder.sum(a, b);
        String expect = new BigInteger(a).add(new BigInteger(b)).toString();
        if (!expect.equals(once)) {
            throw new IllegalStateException(once + " != " + expect);
        }

        if ("check".equals(mode)) {
            var steps = adder.sumWithSteps(a, b);
            System.out.println("sum_len=" + steps.sum().length() + " steps=" + steps.steps().size());
            return;
        }
        if ("ram".equals(mode)) {
            long empty = usedAfterGc();
            com.sun.management.ThreadMXBean threads =
                    (com.sun.management.ThreadMXBean) ManagementFactory.getThreadMXBean();
            long alloc0 = threads.getCurrentThreadAllocatedBytes();
            Object kept = null;
            for (int i = 0; i < calls; i++) {
                kept = adder.sumWithSteps(a, b);
            }
            long allocated = threads.getCurrentThreadAllocatedBytes() - alloc0;
            long withResult = usedAfterGc();
            kept = null;
            long dropped = usedAfterGc();
            System.out.printf(
                    "mode=ram digits=%d calls=%d heap_empty=%d alloc_bytes=%d heap_with_result=%d heap_dropped=%d%n",
                    digits, calls, empty, allocated, withResult, dropped);
            return;
        }

        System.gc();
        com.sun.management.ThreadMXBean threads =
                (com.sun.management.ThreadMXBean) ManagementFactory.getThreadMXBean();
        long alloc0 = threads.getCurrentThreadAllocatedBytes();
        long gc0 = gcCount();
        long gcTime0 = gcTime();
        long t0 = System.nanoTime();
        long acc = 0;
        Object kept = null;
        for (int i = 0; i < calls; i++) {
            if ("steps".equals(mode)) {
                var result = adder.sumWithSteps(a, b);
                acc += result.sum().length() + result.steps().size();
                kept = result;
            } else if ("sum".equals(mode)) {
                String sum = adder.sum(a, b);
                acc += sum.length();
                kept = sum;
            } else {
                throw new IllegalArgumentException(mode);
            }
        }
        long elapsed = System.nanoTime() - t0;
        long allocated = threads.getCurrentThreadAllocatedBytes() - alloc0;
        long gc = gcCount() - gc0;
        long gcMs = gcTime() - gcTime0;
        System.gc();
        long used = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory();
        System.out.printf(
                "mode=%s digits=%d calls=%d log=%s ns=%d alloc_bytes=%d gc_count=%d gc_ms=%d heap_after_gc=%d acc=%d kept=%s%n",
                mode, digits, calls, logOff ? "off" : "on", elapsed, allocated, gc, gcMs, used, acc,
                kept.getClass().getSimpleName());
    }
    private static long usedAfterGc() {
        System.gc();
        try {
            Thread.sleep(20);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        System.gc();
        return Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory();
    }


    private static long gcCount() {
        long n = 0;
        for (GarbageCollectorMXBean bean : ManagementFactory.getGarbageCollectorMXBeans()) {
            long c = bean.getCollectionCount();
            if (c > 0) {
                n += c;
            }
        }
        return n;
    }

    private static long gcTime() {
        long n = 0;
        for (GarbageCollectorMXBean bean : ManagementFactory.getGarbageCollectorMXBeans()) {
            long c = bean.getCollectionTime();
            if (c > 0) {
                n += c;
            }
        }
        return n;
    }
}
