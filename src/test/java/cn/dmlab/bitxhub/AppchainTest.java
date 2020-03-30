package cn.dmlab.bitxhub;

import com.alibaba.fastjson.JSONObject;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.IOUtils;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import pb.ArgOuterClass;
import pb.ReceiptOuterClass;

import java.io.FileInputStream;
import java.io.IOException;

@RunWith(JUnit4.class)
@Slf4j
public class AppchainTest {

    private GrpcClient client;


    private Config config = Config.defaultConfig();

    @Before
    public void setUp() {
        client = new GrpcClientImpl(config);
    }

    @After
    public void tearDown() throws Exception {
        client.stop();
    }

    @Test
    public void registerAppchain() {
        ArgOuterClass.Arg[] args = Types.toArgArray(
                Types.string(""), //validators
                Types.i32(0), //consensus_type
                Types.string("hyperchain"), //chain_type
                Types.string("税务链"), //name
                Types.string("趣链税务链"), //desc
                Types.string("1.8")); //version
        ReceiptOuterClass.Receipt receipt = client.invokeBVMContract(BVMAddr.INTER_CHAIN_CONTRACT_ADDR, "Register", args);
        Assert.assertNotNull(receipt);

        String ret = receipt.getRet().toStringUtf8();
        JSONObject jsonObject = JSONObject.parseObject(ret);
        String chainType = jsonObject.getString("chain_type");

        Assert.assertEquals(chainType, "hyperchain");
    }

    @Test
    public void adultAppchain() {
        ArgOuterClass.Arg[] args = Types.toArgArray(
                Types.string(""), //validators
                Types.i32(0), //consensus_type
                Types.string("hyperchain"), //chain_type
                Types.string("税务链"), //name
                Types.string("趣链税务链"), //desc
                Types.string("1.8")); //version
        ReceiptOuterClass.Receipt receipt = client.invokeBVMContract(BVMAddr.INTER_CHAIN_CONTRACT_ADDR, "Register", args);
        Assert.assertNotNull(receipt);


        String ret = receipt.getRet().toStringUtf8();
        JSONObject jsonObject = JSONObject.parseObject(ret);
        ArgOuterClass.Arg[] adultArgs = Types.toArgArray(
                Types.string(jsonObject.getString("id")), //应用链ID
                Types.i32(1), //审核通过
                Types.string("")); //desc
        ReceiptOuterClass.Receipt adultReceipt = client.invokeBVMContract(BVMAddr.INTER_CHAIN_CONTRACT_ADDR, "Audit", adultArgs);
        Assert.assertNotNull(adultReceipt);
    }

    @Test
    public void deleteAppchain() {
        ArgOuterClass.Arg[] args = Types.toArgArray(
                Types.string(""), //validators
                Types.i32(0), //consensus_type
                Types.string("hyperchain"), //chain_type
                Types.string("税务链"), //name
                Types.string("趣链税务链"), //desc
                Types.string("1.8")); //version
        ReceiptOuterClass.Receipt receipt = client.invokeBVMContract(BVMAddr.INTER_CHAIN_CONTRACT_ADDR, "Register", args);
        Assert.assertNotNull(receipt);

        String ret = receipt.getRet().toStringUtf8();
        JSONObject jsonObject = JSONObject.parseObject(ret);
        ArgOuterClass.Arg[] deleteArgs = Types.toArgArray(
                Types.string(jsonObject.getString("id")));
        ReceiptOuterClass.Receipt adultReceipt = client.invokeBVMContract(BVMAddr.INTER_CHAIN_CONTRACT_ADDR, "Audit", deleteArgs);
        Assert.assertNotNull(adultReceipt);
    }

    @Test
    public void registerRule() throws IOException {
        byte[] contractBytes = IOUtils.toByteArray(
                new FileInputStream("target/test-classes/testdata/example.wasm"));
        String contractAddress = client.deployContract(contractBytes);
        Assert.assertNotNull(contractAddress);
        ArgOuterClass.Arg[] args = Types.toArgArray(
                Types.string(""), //validators
                Types.i32(0), //consensus_type
                Types.string("hyperchain"), //chain_type
                Types.string("税务链"), //name
                Types.string("趣链税务链"), //desc
                Types.string("1.8")); //version
        ReceiptOuterClass.Receipt receipt = client.invokeBVMContract(BVMAddr.INTER_CHAIN_CONTRACT_ADDR, "Register", args);
        Assert.assertNotNull(receipt);

        String ret = receipt.getRet().toStringUtf8();
        JSONObject jsonObject = JSONObject.parseObject(ret);

        String appchainID = jsonObject.getString("id");

        ArgOuterClass.Arg[] ruleArgs = Types.toArgArray(
                Types.string(appchainID),
                Types.string(contractAddress));
        ReceiptOuterClass.Receipt ruleReceipt = client.invokeBVMContract(BVMAddr.RULE_MANAGER_CONTRACT_ADDR, "RegisterRule", ruleArgs);
        Assert.assertNotNull(ruleReceipt);

    }
}
