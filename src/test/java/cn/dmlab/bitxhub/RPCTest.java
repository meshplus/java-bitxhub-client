package cn.dmlab.bitxhub;

import cn.dmlab.crypto.ecdsa.ECKeyP256;
import cn.dmlab.crypto.ecdsa.ECKeyS256;
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
import pb.Transaction;


@RunWith(JUnit4.class)
@Slf4j
public class RPCTest {

    private GrpcClient client;

    private Config config = Config.defaultConfig();
    byte[] from = config.getAddress();
    byte[] to = new ECKeyP256().getAddress();

    @Before
    public void setUp() {
        config.setEcKey(ECKeyS256.fromPrivate(ByteUtil.hexStringToBytes("b6477143e17f889263044f6cf463dc37177ac4526c4c39a7a344198457024a2f")));
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
        Transaction.BxhTransaction unsignedTx = Transaction.BxhTransaction.newBuilder()
                .setFrom(ByteString.copyFrom(from))
                .setTo(ByteString.copyFrom(to))
                .setTimestamp(Utils.genTimestamp())
                .setPayload(Transaction.TransactionData.newBuilder().setAmount("100000").build().toByteString())
                .build();
        ReceiptOuterClass.Receipt receipt = client.sendTransactionWithReceipt(unsignedTx, null);

        Broker.GetTransactionResponse transactionResponse = client.getTransaction(ByteUtil.toHexStringWithOx(receipt.getTxHash().toByteArray()));

        Assert.assertEquals(ByteUtil.toHexStringWithOx(transactionResponse.getTx().getTransactionHash().toByteArray()),
                ByteUtil.toHexStringWithOx(receipt.getTxHash().toByteArray()));

    }

    @Test
    public void getReceipt() {
        Transaction.BxhTransaction unsignedTx = Transaction.BxhTransaction.newBuilder()
                .setFrom(ByteString.copyFrom(from))
                .setTo(ByteString.copyFrom(to))
                .setTimestamp(Utils.genTimestamp())
                .setPayload(Transaction.TransactionData.newBuilder()
                        .setAmount("100000")
                        .build().toByteString())
                .build();
        String txHash = client.sendTransaction(unsignedTx, null);
        ReceiptOuterClass.Receipt receipt = client.getReceipt(txHash);

        Assert.assertTrue(ByteUtil.toHexStringWithOx(receipt.getTxHash().toByteArray()).equalsIgnoreCase(txHash));
    }


    @Test
    public void sendTransaction() {
        Transaction.BxhTransaction unsignedTx = Transaction.BxhTransaction.newBuilder()
                .setFrom(ByteString.copyFrom(from))
                .setTo(ByteString.copyFrom(to))
                .setTimestamp(Utils.genTimestamp())
                .setPayload(Transaction.TransactionData.newBuilder().setAmount("100000").build().toByteString())
                .build();
        String txHash = client.sendTransaction(unsignedTx, null);
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