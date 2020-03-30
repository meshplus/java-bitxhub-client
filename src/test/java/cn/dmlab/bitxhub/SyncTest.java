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
public class SyncTest {
    private GrpcClient client;

    private Config config = Config.defaultConfig();
    byte[] from = config.getEcKey().getAddress();
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
                .setNonce(Utils.genNonce())
                .setData(TransactionOuterClass.TransactionData.newBuilder().setAmount(100000L).build())
                .build();
        TransactionOuterClass.Transaction signedTx = SignUtils.sign(unsignedTx, config.getEcKey());
        String txHash = client.sendTransaction(signedTx);
        Assert.assertNotNull(txHash);
    }

    @Test
    public void syncMerkleWrapper() throws InterruptedException {
        CountDownLatch asyncLatch = new CountDownLatch(1);
        StreamObserver<Broker.Response> observer = new StreamObserver<Broker.Response>() {
            @Override
            public void onNext(Broker.Response response) {
                ByteString data = response.getData();
                BlockOuterClass.MerkleWrapper merkleWrapper = null;
                try {
                    merkleWrapper = BlockOuterClass.MerkleWrapper.parseFrom(data);
                } catch (InvalidProtocolBufferException e) {
                    e.printStackTrace();
                }
                Assert.assertNotNull(merkleWrapper);
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

        client.syncMerkleWrapper("node1", observer);
        sendTransaction();
        asyncLatch.await();
    }

    @Test
    public void getMerkleWrapper() throws InterruptedException {
        CountDownLatch asyncLatch = new CountDownLatch(1);
        StreamObserver<Broker.Response> observer = new StreamObserver<Broker.Response>() {
            @Override
            public void onNext(Broker.Response response) {
                ByteString data = response.getData();
                BlockOuterClass.MerkleWrapper merkleWrapper = null;
                try {
                    merkleWrapper = BlockOuterClass.MerkleWrapper.parseFrom(data);
                } catch (InvalidProtocolBufferException e) {
                    e.printStackTrace();
                }
                Assert.assertNotNull(merkleWrapper);
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

        client.getMerkleWrapper("node1", 1L, 2L, observer);
        sendTransaction();
        asyncLatch.await();
    }
}
