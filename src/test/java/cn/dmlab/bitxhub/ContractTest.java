package cn.dmlab.bitxhub;

import cn.dmlab.crypto.ecdsa.ECKeyS256;
import cn.dmlab.utils.ByteUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.IOUtils;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import pb.ReceiptOuterClass;
import pb.BxhTransactionOuterClass;

import java.io.FileInputStream;
import java.io.IOException;

@RunWith(JUnit4.class)
@Slf4j
public class ContractTest {

    private GrpcClient client;

    private Config config = Config.defaultConfig();

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
    public void deployContract() throws IOException {
        byte[] contractBytes = IOUtils.toByteArray(new FileInputStream("target/test-classes/testdata/example.wasm"));

        String contractAddress = client.deployContract(contractBytes);
        Assert.assertNotNull(contractAddress);

    }

    @Test
    public void invokeContract() throws IOException {
        byte[] contractBytes = IOUtils.toByteArray(
                new FileInputStream("target/test-classes/testdata/example.wasm"));
        String contractAddress = client.deployContract(contractBytes);

        System.out.println(contractAddress);

        ReceiptOuterClass.Receipt receipt = client.invokeXVMContract(contractAddress, "a", Types.i32(333), Types.i32(1));
        Assert.assertEquals(receipt.getRet().toStringUtf8(), "667");
    }

    @Test
    public void invokeBVMContract() {
        String result = "10";
        ReceiptOuterClass.Receipt receipt = client.invokeContract(BxhTransactionOuterClass.TransactionData.VMType.BVM
                , BVMAddr.STORE_CONTRACT_ADDR, "Set", Types.string("a"), Types.string(result));
        Assert.assertNotNull(receipt);


        ReceiptOuterClass.Receipt receipt1 = client.sendView(client.generateContractTx(BxhTransactionOuterClass.TransactionData.VMType.BVM
                , BVMAddr.STORE_CONTRACT_ADDR, "Get", Types.string("a")));
        Assert.assertEquals(receipt1.getRet().toStringUtf8(), result);

    }

}
