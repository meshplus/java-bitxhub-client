package cn.dmlab.bitxhub;

import cn.dmlab.crypto.ecdsa.ECKeyS256;
import cn.dmlab.utils.ByteUtil;
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
import org.web3j.crypto.Keys;
import pb.AuditInfo;
import pb.BlockOuterClass;
import pb.Broker;
import pb.Transaction;

import java.util.concurrent.CountDownLatch;

@RunWith(JUnit4.class)
@Slf4j
public class SubscribeTest {
    private GrpcClient client;

    private Config config = Config.defaultConfig();
    byte[] from = config.getAddress();
    byte[] to = new ECKeyS256().getAddress();

    @Before
    public void setUp() {
        // The corresponding address is 0x9E1D8be61dee418B83A47BE54a1777ca70e10E0F
        config.setEcKey(ECKeyS256.fromPrivate(ByteUtil.hexStringToBytes("c0a264f1ebedddea680727dd3177adbe765393e3eb6f9ce75417d9675e19a4ad")));
        from = config.getAddress();
        client = new GrpcClientImpl(config);
    }

    @After
    public void tearDown() throws Exception {
        client.stop();
    }

    public void sendTransaction() {
        Transaction.BxhTransaction unsignedTx = Transaction.BxhTransaction.newBuilder()
                .setFrom(ByteString.copyFrom(from))
                .setTo(ByteString.copyFrom(to))
                .setTimestamp(Utils.genTimestamp())
                .setPayload(Transaction.TransactionData.newBuilder().setAmount("100000").build().toByteString())
                .build();
        Transaction.BxhTransaction signedTx = SignUtils.sign(unsignedTx, config.getEcKey());
        String txHash = client.sendTransaction(signedTx, null);
        Assert.assertNotNull(txHash);
    }


    @Test
    public void subscribe() throws InterruptedException {
        CountDownLatch asyncLatch = new CountDownLatch(1);
        StreamObserver<Broker.Response> observer = new StreamObserver<Broker.Response>() {
            @Override
            public void onNext(Broker.Response response) {
                System.out.println("================================================");
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

    // Need to be registered a id for the application of appchain1 chain,
    // and register an audit node with address 0x9E1D8be61dee418B83A47BE54a1777ca70e10E0F, permission appchain1
    @Test
    public void subscribeAudit() throws InterruptedException {
        CountDownLatch asyncLatch = new CountDownLatch(1);
        StreamObserver<Broker.Response> observer = new StreamObserver<Broker.Response>() {
            @Override
            public void onNext(Broker.Response response) {
                ByteString data = response.getData();
                AuditInfo.AuditTxInfo info = null;
                Transaction.TransactionData txData = null;
                Transaction.InvokePayload payload = null;
                try {
                    info = AuditInfo.AuditTxInfo.parseFrom(data);
                    if (!info.getTx().hasIBTP()){
                        txData = Transaction.TransactionData.parseFrom(info.getTx().getPayload());
                        payload = Transaction.InvokePayload.parseFrom(txData.getPayload());
                        System.out.printf("from: %s, to: %s\n",
                                Keys.toChecksumAddress(ByteUtil.toHexStringWithOx(info.getTx().getFrom().toByteArray())),
                                Keys.toChecksumAddress(ByteUtil.toHexStringWithOx(info.getTx().getTo().toByteArray()))
                        );
                        System.out.printf("method: %s\n", payload.getMethod());
                    }
                } catch (InvalidProtocolBufferException e) {
                    e.printStackTrace();
                }
                Assert.assertNotNull(info);
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


        System.out.println(Keys.toChecksumAddress(ByteUtil.toHexStringWithOx(config.getAddress())));
        client.subscribeAuditInfo(AuditInfo.AuditSubscriptionRequest.Type.AUDIT_NODE, new Long(1), observer);
        asyncLatch.await();
    }

}
