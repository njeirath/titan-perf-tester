package us.wellaware;

import com.thinkaurelius.titan.core.TitanTransaction;

/**
 * Created by nakuljeirath on 1/11/16.
 */
public interface PerfOperation {
    void run(TitanTransaction tx);
}
