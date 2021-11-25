package cn.dmlab.bitxhub;

import cn.dmlab.crypto.ecdsa.ECKeyS256;
import cn.dmlab.utils.ByteUtil;
import cn.dmlab.utils.SignUtils;
import cn.dmlab.utils.Utils;
import com.alibaba.fastjson.JSONObject;
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
import pb.*;

import java.util.concurrent.CountDownLatch;

@RunWith(JUnit4.class)
@Slf4j
public class SubscribeAuditTest {
    private GrpcClient adminCli1, adminCli2, adminCli3, appchainCli, nodeCli;
    byte[] adminAddr1, appchainAdmin, nodeAccount;
    ECKeyS256 key1;
    public static final String HAPPY_RULE = "0x00000000000000000000000000000000000000a2";

    private Config config1 = Config.defaultConfig();
    private Config config2 = Config.defaultConfig();
    private Config config3 = Config.defaultConfig();
    private Config config4 = Config.defaultConfig();
    private Config config5 = Config.defaultConfig();

    @Before
    public void setUp() {
        config1.setEcKey(ECKeyS256.fromPrivate(ByteUtil.hexStringToBytes("b6477143e17f889263044f6cf463dc37177ac4526c4c39a7a344198457024a2f")));
        adminCli1 = new GrpcClientImpl(config1);
        adminAddr1 = config1.getAddress();
        key1 = config1.getEcKey();

        config2.setEcKey(ECKeyS256.fromPrivate(ByteUtil.hexStringToBytes("05c3708d30c2c72c4b36314a41f30073ab18ea226cf8c6b9f566720bfe2e8631")));
        adminCli2 = new GrpcClientImpl(config2);

        config3.setEcKey(ECKeyS256.fromPrivate(ByteUtil.hexStringToBytes("85a94dd51403590d4f149f9230b6f5de3a08e58899dcaf0f77768efb1825e854")));
        adminCli3 = new GrpcClientImpl(config3);

        config4.setEcKey(ECKeyS256.fromPrivate(ByteUtil.hexStringToBytes("c0a264f1ebedddea680727dd3177adbe765393e3eb6f9ce75417d9675e19a4ad")));
        appchainCli = new GrpcClientImpl(config4);
        appchainAdmin = config4.getAddress();

        config5.setEcKey(ECKeyS256.fromPrivate(ByteUtil.hexStringToBytes("020314db58805cd11f45c72be847db05643a48137f0d9302c0f3699967545077")));
        nodeCli = new GrpcClientImpl(config5);
        nodeAccount = config5.getAddress();

        transfer(appchainAdmin);
        transfer(nodeAccount);
    }

    @After
    public void tearDown() throws Exception {
        adminCli1.stop();
        adminCli2.stop();
        adminCli3.stop();
        appchainCli.stop();
        nodeCli.stop();
    }

    @Test
    public void sendTransaction() {
        Transaction.BxhTransaction unsignedTx = Transaction.BxhTransaction.newBuilder()
                .setFrom(ByteString.copyFrom(adminAddr1))
                .setTo(ByteString.copyFrom(appchainAdmin))
                .setTimestamp(Utils.genTimestamp())
                .setPayload(Transaction.TransactionData.newBuilder().setAmount("100000").build().toByteString())
                .build();
        Transaction.BxhTransaction signedTx = SignUtils.sign(unsignedTx, config1.getEcKey());
        String txHash = adminCli1.sendTransaction(signedTx, null);
        Assert.assertNotNull(txHash);
    }

    public void transfer(byte[] to){
        Transaction.BxhTransaction unsignedTx = Transaction.BxhTransaction.newBuilder()
                .setFrom(ByteString.copyFrom(adminAddr1))
                .setTo(ByteString.copyFrom(to))
                .setTimestamp(Utils.genTimestamp())
                .setPayload(Transaction.TransactionData.newBuilder().setAmount("100000000000000000").build().toByteString())
                .build();

        Transaction.BxhTransaction signedTx = SignUtils.sign(unsignedTx, key1);
        String txHash = adminCli1.sendTransaction(signedTx, null);
        Assert.assertNotNull(txHash);
    }

    public void vote(String proposalID){
        ArgOuterClass.Arg[] args = Types.toArgArray(
                Types.string(proposalID),
                Types.string("approve"),
                Types.string("reason"));
        ReceiptOuterClass.Receipt receipt = adminCli1.invokeBVMContract(BVMAddr.GOVERNANCE_CONTRACT_ADDR, "Vote", args);
        Assert.assertNotNull(receipt);
        receipt = adminCli2.invokeBVMContract(BVMAddr.GOVERNANCE_CONTRACT_ADDR, "Vote", args);
        Assert.assertNotNull(receipt);
        receipt = adminCli3.invokeBVMContract(BVMAddr.GOVERNANCE_CONTRACT_ADDR, "Vote", args);
        Assert.assertNotNull(receipt);
    }

    public void registerAppchain(String chainID, String chainName, String appchainAdmin){
        ArgOuterClass.Arg[] args = Types.toArgArray(
                Types.string(chainID),
                Types.string(chainName),
                Types.string("ETH"),
                Types.bytes(new byte[0]),
                Types.string("broker"),
                Types.string("des"),
                Types.string(HAPPY_RULE),
                Types.string("url"),
                Types.string(appchainAdmin),
                Types.string("reason"));
        ReceiptOuterClass.Receipt receipt = appchainCli.invokeBVMContract(BVMAddr.APPCHAIN_MANAGER_CONTRACT_ADDR, "RegisterAppchain", args);
        Assert.assertNotNull(receipt);
        String ret = receipt.getRet().toStringUtf8();
        JSONObject jsonObject = JSONObject.parseObject(ret);
        String proposalID = jsonObject.getString("proposal_id");
        vote(proposalID);
    }

    public void registerNode(String nodeAccout, String nodeName, String appchainID){
        ArgOuterClass.Arg[] args = Types.toArgArray(
                Types.string(nodeAccout),
                Types.string("nvpNode"),
                Types.string(""),
                Types.u64((long) 0),
                Types.string(nodeName),
                Types.string(appchainID),
                Types.string("reason"));
        ReceiptOuterClass.Receipt receipt = adminCli1.invokeBVMContract(BVMAddr.NODE_MANAGER_CONTRACT_ADDR, "RegisterNode", args);
        Assert.assertNotNull(receipt);
        String ret = receipt.getRet().toStringUtf8();
        JSONObject jsonObject = JSONObject.parseObject(ret);
        String proposalID = jsonObject.getString("proposal_id");
        vote(proposalID);
    }

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

        System.out.printf("adminAddr: %s\n", Keys.toChecksumAddress(ByteUtil.toHexStringWithOx(adminAddr1)));
        System.out.printf("appchainAddr: %s\n", Keys.toChecksumAddress(ByteUtil.toHexStringWithOx(appchainAdmin)));
        System.out.printf("nodeAccount: %s\n", Keys.toChecksumAddress(ByteUtil.toHexStringWithOx(nodeAccount)));

        try {
            registerAppchain("appchain1", "应用链1", Keys.toChecksumAddress(ByteUtil.toHexStringWithOx(appchainAdmin)));
            registerNode(Keys.toChecksumAddress(ByteUtil.toHexStringWithOx(nodeAccount)), "审计节点", "appchain1");
            Thread.sleep(5000);
            nodeCli.subscribeAuditInfo(AuditInfo.AuditSubscriptionRequest.Type.AUDIT_NODE, new Long(1), observer);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        asyncLatch.await();
    }

}
