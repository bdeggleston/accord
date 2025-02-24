package accord.topology;

import accord.Utils;
import accord.api.Key;
import accord.api.KeyRange;
import accord.impl.IntKey;
import accord.impl.TopologyFactory;
import accord.local.Node;
import accord.txn.Keys;
import com.google.common.collect.Iterables;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.List;

import static accord.impl.IntKey.key;
import static accord.impl.IntKey.range;

public class TopologyTest
{

    private static void assertRangeForKey(Topology topology, int key, int start, int end)
    {
        Key expectedKey = key(key);
        Shard shard = topology.forKey(key(key));
        KeyRange expectedRange = range(start, end);
        Assertions.assertTrue(expectedRange.containsKey(expectedKey));
        Assertions.assertTrue(shard.range.containsKey(expectedKey));
        Assertions.assertEquals(expectedRange, shard.range);

        Shards shards = topology.forKeys(Keys.of(expectedKey));
        shard = Iterables.getOnlyElement(shards);
        Assertions.assertTrue(shard.range.containsKey(expectedKey));
        Assertions.assertEquals(expectedRange, shard.range);
    }

    private static Topology topology(List<Node.Id> ids, int rf, KeyRange... ranges)
    {
        TopologyFactory<IntKey> topologyFactory = new TopologyFactory<>(rf, ranges);
        return topologyFactory.toShards(ids);
    }

    private static Topology topology(int numNodes, int rf, KeyRange... ranges)
    {
        return topology(Utils.ids(numNodes), rf, ranges);
    }

    private static Topology topology(KeyRange... ranges)
    {
        return topology(1, 1, ranges);
    }

    private static KeyRange<IntKey> r(int start, int end)
    {
        return IntKey.range(start, end);
    }

    @Test
    void forKeyTest()
    {
        Topology topology = topology(r(0, 100), r(100, 200), r(200, 300));
        assertRangeForKey(topology, 50, 0, 100);
        assertRangeForKey(topology, 100, 0, 100);
    }

    @Test
    void forRangesTest()
    {

    }

    @Test
    void sequentialRanges()
    {
        // TODO: confirm non-sequential ranges are handled properly
    }
}
