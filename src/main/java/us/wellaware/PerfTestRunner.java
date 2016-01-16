package us.wellaware;

import com.thinkaurelius.titan.core.TitanGraph;
import com.thinkaurelius.titan.core.TitanTransaction;
import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;
import org.apache.tinkerpop.gremlin.structure.Vertex;

import java.util.Date;
import java.util.List;

public class PerfTestRunner {
    public static DescriptiveStatistics test(final TitanGraph graph, int iterations, PerfOperation op) {
        DescriptiveStatistics stats = new DescriptiveStatistics();

        for (int i = 0; i < iterations; i++) {
            TitanTransaction tx = graph.newTransaction();
            Date start = new Date();
            op.run(tx);
            Date end = new Date();

            stats.addValue(end.getTime() - start.getTime());
            tx.rollback();
        }

        return stats;
    }
}
