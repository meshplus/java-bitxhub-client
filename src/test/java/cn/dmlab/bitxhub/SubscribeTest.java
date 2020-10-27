package cn.dmlab.bitxhub;

import cn.dmlab.crypto.ecdsa.ECKeyP256;
import cn.dmlab.utils.SignUtils;
import cn.dmlab.utils.Utils;
import com.google.protobuf.ByteString;
import com.google.protobuf.InvalidProtocolBufferException;
import io.grpc.stub.StreamObserver;
import lombok.extern.slf4j.Slf4j;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import pb.BlockOuterClass;
import pb.Broker;
import pb.TransactionOuterClass;

import java.util.concurrent.CountDownLatch;

@RunWith(JUnit4.class)
@Slf4j
public class SubscribeTest {
    private GrpcClient client;

    private Config config = Config.defaultConfig();
    byte[] from = config.getAddress();
    byte[] to = new ECKeyP256().getAddress();

    @Before
    public void setUp() {
        client = new GrpcClientImpl(config);
    }

    @After
    public void tearDown() throws Exception {
        client.stop();
    }

    public void sendTransaction() {
        TransactionOuterClass.Transaction unsignedTx = TransactionOuterClass.Transaction.newBuilder()
                .setFrom(ByteString.copyFrom(from))
                .setTo(ByteString.copyFrom(to))
                .setTimestamp(Utils.genTimestamp())
                .setPayload(TransactionOuterClass.TransactionData.newBuilder().setAmount(100000L).build().toByteString())
                .build();
        TransactionOuterClass.Transaction signedTx = SignUtils.sign(unsignedTx, config.getEcKey());
        String txHash = client.sendTransaction(signedTx, null);
        Assert.assertNotNull(txHash);
    }


    @Test
    public void subscribe() throws InterruptedException {
        CountDownLatch asyncLatch = new CountDownLatch(1);
        StreamObserver<Broker.Response> observer = new StreamObserver<Broker.Response>() {
            @Override
            public void onNext(Broker.Response response) {
                ByteString data = response.getData();
                BlockOuterClass.Block block = null;
                try {
                    block = BlockOuterClass.Block.parseFrom(data);
                } catch (InvalidProtocolBufferException e) {
                    e.printStackTrace();
                }
                Assert.assertNotNull(block);
                asyncLatch.countDown();
            }

            @Override
            public void onError(Throwable throwable) {
                throwable.printStackTrace();
                asyncLatch.countDown();
            }

            @Override
            public void onCompleted() {
                asyncLatch.countDown();
            }
        };

        client.subscribe(Broker.SubscriptionRequest.Type.BLOCK, observer);

        sendTransaction();
        asyncLatch.await();
    }

}
