package bench.looplocal;

import dev.hieplp.mci.core.MyBigNumber;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Fork;
import org.openjdk.jmh.annotations.Level;
import org.openjdk.jmh.annotations.Measurement;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.Warmup;
import org.openjdk.jmh.infra.Blackhole;

/**
 * JMH benchmark. The MyBigNumber on the classpath selects old, hoist-only, or updated.
 * Logging is off so the log line is not the cost.
 */
@State(Scope.Benchmark)
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.MICROSECONDS)
@Warmup(iterations = 2, time = 1)
@Measurement(iterations = 3, time = 1)
@Fork(3)
public class AddBench {

    private MyBigNumber adder;
    private String left;
    private String right;

    @Setup(Level.Trial)
    public void setup() {
        Logger.getLogger("dev.hieplp.mci.core").setLevel(java.util.logging.Level.OFF);
        Logger.getLogger("").setLevel(java.util.logging.Level.OFF);
        adder = new MyBigNumber();
        left = "9".repeat(999);
        right = "1".repeat(999);
    }

    @Benchmark
    public void sumWithSteps(Blackhole hole) {
        var result = adder.sumWithSteps(left, right);
        hole.consume(result.sum());
        hole.consume(result.steps());
    }
}
