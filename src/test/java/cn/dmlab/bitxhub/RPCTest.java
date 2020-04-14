package cn.dmlab.bitxhub;

import cn.dmlab.crypto.ecdsa.ECKeyP256;
import cn.dmlab.utils.ByteUtil;
import cn.dmlab.utils.SignUtils;
import cn.dmlab.utils.Utils;
import com.google.protobuf.ByteString;
import lombok.extern.slf4j.Slf4j;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import pb.Broker;
import pb.ReceiptOuterClass;
import pb.TransactionOuterClass;


@RunWith(JUnit4.class)
@Slf4j
public class RPCTest {

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


    @Test
    public void getAccountBalance() {
        Broker.Response response = client.getAccountBalance(ByteUtil.toHexStringWithOx(from));
        Assert.assertNotNull(response);
    }

    @Test
    public void getTransaction() {
        TransactionOuterClass.Transaction unsignedTx = TransactionOuterClass.Transaction.newBuilder()
                .setFrom(ByteString.copyFrom(from))
                .setTo(ByteString.copyFrom(to))
                .setTimestamp(Utils.genTimestamp())
                .setNonce(Utils.genNonce())
                .setData(TransactionOuterClass.TransactionData.newBuilder().setAmount(100000L).build())
                .build();
        TransactionOuterClass.Transaction signedTx = SignUtils.sign(unsignedTx, config.getEcKey());
        ReceiptOuterClass.Receipt receipt = client.sendTransactionWithReceipt(signedTx);

        Broker.GetTransactionResponse transactionResponse = client.getTransaction(ByteUtil.toHexStringWithOx(receipt.getTxHash().toByteArray()));

        Assert.assertEquals(ByteUtil.toHexStringWithOx(transactionResponse.getTx().getTransactionHash().toByteArray()),
                ByteUtil.toHexStringWithOx(receipt.getTxHash().toByteArray()));

    }

    @Test
    public void getReceipt() {
        TransactionOuterClass.Transaction unsignedTx = TransactionOuterClass.Transaction.newBuilder()
                .setFrom(ByteString.copyFrom(from))
                .setTo(ByteString.copyFrom(to))
                .setTimestamp(Utils.genTimestamp())
                .setNonce(Utils.genNonce())
                .setData(TransactionOuterClass.TransactionData.newBuilder()
                        .setAmount(100000L)
                        .build())
                .build();
        TransactionOuterClass.Transaction signedTx = SignUtils.sign(unsignedTx, config.getEcKey());
        String txHash = client.sendTransaction(signedTx);
        ReceiptOuterClass.Receipt receipt = client.getReceipt(txHash);
        Assert.assertEquals(ByteUtil.toHexStringWithOx(receipt.getTxHash().toByteArray()), txHash);

    }


    @Test
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
    public void getNetworkMeta() {
        Broker.Response response = client.getNetworkMeta();
        Assert.assertNotNull(response);
    }

    @Test
    public void getValidators() {
        Broker.Response response = client.getValidators();
        Assert.assertNotNull(response);
    }

}