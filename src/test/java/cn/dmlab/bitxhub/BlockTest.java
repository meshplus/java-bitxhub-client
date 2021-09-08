package cn.dmlab.bitxhub;

import lombok.extern.slf4j.Slf4j;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import pb.BlockOuterClass;
import pb.Broker;
import pb.Chain;

@RunWith(JUnit4.class)
@Slf4j
public class BlockTest {

    private GrpcClient client;

    private Config config = Config.defaultConfigWithTLS();

    @Before
    public void setUp() {
        client = new GrpcClientImpl(config);
    }

    @After
    public void tearDown() throws Exception {
        client.stop();
    }

    @Test
    public void getBlock() {
        BlockOuterClass.Block block = client.getBlock("1", Broker.GetBlockRequest.Type.HEIGHT);
        Assert.assertNotNull(block);
        Assert.assertEquals(1, block.getBlockHeader().getNumber());
    }

    @Test
    public void getChainStatus() {
        Broker.Response response = client.getChainStatus();
        Assert.assertNotNull(response);
    }

    @Test
    public void getChainMeta() {
        Chain.ChainMeta chainMeta = client.getChainMeta();
        Assert.assertTrue(chainMeta.getHeight() > 0);
    }

    @Test
    public void getBlocks() {
        Broker.GetBlocksResponse response = client.getBlocks(1L, 10L);
        Assert.assertTrue(response.getBlocksCount() > 0);
    }
}
