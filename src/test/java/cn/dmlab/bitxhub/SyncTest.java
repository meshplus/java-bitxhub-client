package cn.dmlab.bitxhub;

import cn.dmlab.crypto.ecdsa.ECKeyP256;
import cn.dmlab.utils.ByteUtil;
import cn.dmlab.utils.SignUtils;
import cn.dmlab.utils.Utils;
import com.alibaba.fastjson.JSONObject;
import com.google.protobuf.ByteString;
import io.grpc.stub.StreamObserver;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.IOUtils;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import pb.*;

import java.io.FileInputStream;
import java.io.IOException;
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
    public void getInterchainTxWrapper() throws InterruptedException {
        CountDownLatch asyncLatch = new CountDownLatch(1);
        StreamObserver<Broker.InterchainTxWrapper> observer = new StreamObserver<Broker.InterchainTxWrapper>() {
            @Override
            public void onNext(Broker.InterchainTxWrapper interchainTxWrapper) {
                Assert.assertNotNull(interchainTxWrapper);
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

        client.getInterchainTxWrapper("node1", 1L, 2L, observer);
        sendInterchaintx();
        asyncLatch.await();
    }

    @Test
    public void getBlockHeaders() throws InterruptedException {
        CountDownLatch asyncLatch = new CountDownLatch(1);
        StreamObserver<BlockOuterClass.BlockHeader> observer = new StreamObserver<BlockOuterClass.BlockHeader>() {
            @Override
            public void onNext(BlockOuterClass.BlockHeader blockHeader) {
                Assert.assertNotNull(blockHeader);
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

        client.getBlockHeaders(1L, 2L, observer);
        sendTransaction();
        asyncLatch.await();
    }

    void sendInterchaintx() {
        ArgOuterClass.Arg[] args = Types.toArgArray(
                Types.string(""), //validators
                Types.i32(0), //consensus_type
                Types.string("hyperchain"), //chain_type
                Types.string("税务链"), //name
                Types.string("趣链税务链"), //desc
                Types.string("1.8"),//version
                Types.string("")); //public key
        ReceiptOuterClass.Receipt receipt = client.invokeBVMContract(BVMAddr.APPCHAIN_MANAGER_CONTRACT_ADDR, "Register", args);
        Assert.assertNotNull(receipt);


        String ret = receipt.getRet().toStringUtf8();
        JSONObject jsonObject = JSONObject.parseObject(ret);
        String appchainID = jsonObject.getString("id");
        ArgOuterClass.Arg[] adultArgs = Types.toArgArray(
                Types.string(appchainID), //应用链ID
                Types.i32(1), //审核通过
                Types.string("")); //desc
        ReceiptOuterClass.Receipt adultReceipt = client.invokeBVMContract(BVMAddr.APPCHAIN_MANAGER_CONTRACT_ADDR, "Audit", adultArgs);
        Assert.assertNotNull(adultReceipt);

        byte[] contractBytes = new byte[0];
        try {
            contractBytes = IOUtils.toByteArray(
                    new FileInputStream("target/test-classes/testdata/example.wasm"));
        } catch (IOException e) {
            e.printStackTrace();
        }
        String contractAddress = client.deployContract(contractBytes);

        ArgOuterClass.Arg[] ruleArgs = Types.toArgArray(
                Types.string(appchainID),
                Types.string(contractAddress));
        ReceiptOuterClass.Receipt ruleReceipt = client.invokeBVMContract(BVMAddr.RULE_MANAGER_CONTRACT_ADDR, "RegisterRule", ruleArgs);
        Assert.assertNotNull(ruleReceipt);

        Ibtp.IBTP ibtp = getIBTP();
        ArgOuterClass.Arg[] ibtpArgs = Types.toArgArray(
                Types.bytes(ibtp.toByteArray())
        );
        client.invokeBVMContract(BVMAddr.INTER_CHAIN_CONTRACT_ADDR, "HandleIBTP", ibtpArgs);
    }

    Ibtp.IBTP getIBTP() {
        Ibtp.content content = Ibtp.content.newBuilder()
                .setSrcContractId(ByteUtil.toHexStringWithOx(from))
                .setDstContractId(ByteUtil.toHexStringWithOx(to))
                .setFunc("set")
                .addArgs(Types.string("Alice").toByteString())
                .build();

        Ibtp.payload payload = Ibtp.payload.newBuilder()
                .setContent(content.toByteString())
                .setEncrypted(false)
                .build();
        Ibtp.IBTP ib = Ibtp.IBTP.newBuilder()
                .setFrom(ByteUtil.toHexStringWithOx(from))
                .setTo(ByteUtil.toHexStringWithOx(to))
                .setIndex(1)
                .setPayload(payload.toByteString())
                .setType(Ibtp.IBTP.Type.INTERCHAIN)
                .setTimestamp(Utils.genTimestamp())
                .build();
        return ib;
    }


}
