package cn.dmlab.bitxhub;

import cn.dmlab.crypto.ecdsa.ECKeyS256;
import cn.dmlab.utils.ByteUtil;
import com.alibaba.fastjson.JSONObject;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.IOUtils;
import org.junit.*;
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

    private GrpcClient adminClient;


    private final Config config = Config.defaultConfig();

    private final Config adminConfig = Config.defaultConfig();

    @Before
    public void setUp() {
        // address: 0xF83625B55Db87bAca3862924777aaCf2E81Cb7eD
        // transfer token to address first
        config.setEcKey(ECKeyS256.fromPrivate(ByteUtil.hexStringToBytes("b44b8cc60b96ce7b3ae948d824df7110f1074a0bb5a19ecf24e2314a942f5325")));
        client = new GrpcClientImpl(config);

        adminConfig.setEcKey(ECKeyS256.fromPrivate(ByteUtil.hexStringToBytes("b6477143e17f889263044f6cf463dc37177ac4526c4c39a7a344198457024a2f")));
        adminClient = new GrpcClientImpl(adminConfig);
    }

    @After
    public void tearDown() throws Exception {
        client.stop();
    }

    @Test
    @Ignore
    public void registerAppchain() {
        ReceiptOuterClass.Receipt receipt = register();

        String ret = receipt.getRet().toStringUtf8();
        JSONObject jsonObject = JSONObject.parseObject(ret);
        String chainType = jsonObject.getString("chain_type");

        Assert.assertEquals(chainType, "hyperchain");
    }

    @Test
    @Ignore
    public void adultAppchain() {
        ReceiptOuterClass.Receipt receipt = register();

        String ret = receipt.getRet().toStringUtf8();
        JSONObject jsonObject = JSONObject.parseObject(ret);
        ArgOuterClass.Arg[] adultArgs = Types.toArgArray(
                Types.string(jsonObject.getString("id")), //应用链ID
                Types.i32(1), //审核通过
                Types.string("")); //desc
        ReceiptOuterClass.Receipt adultReceipt = client.invokeBVMContract(BVMAddr.APPCHAIN_MANAGER_CONTRACT_ADDR, "Audit", adultArgs);
        Assert.assertNotNull(adultReceipt);
    }

    @Test
    @Ignore
    public void deleteAppchain() {
        ReceiptOuterClass.Receipt receipt = register();

        String ret = receipt.getRet().toStringUtf8();
        JSONObject jsonObject = JSONObject.parseObject(ret);
        ArgOuterClass.Arg[] deleteArgs = Types.toArgArray(
                Types.string(jsonObject.getString("id")));
        ReceiptOuterClass.Receipt adultReceipt = client.invokeBVMContract(BVMAddr.APPCHAIN_MANAGER_CONTRACT_ADDR, "DeleteAppchain", deleteArgs);
        Assert.assertNotNull(adultReceipt);
    }

    @Test
    public void registerRule() throws IOException {
        byte[] contractBytes = IOUtils.toByteArray(
                new FileInputStream("target/test-classes/testdata/example.wasm"));
        String contractAddress = client.deployContract(contractBytes);
        Assert.assertNotNull(contractAddress);

        ReceiptOuterClass.Receipt receipt = register();

        String ret = receipt.getRet().toStringUtf8();
        JSONObject jsonObject = JSONObject.parseObject(ret);

        String proposalID = jsonObject.getString("proposal_id");

        ReceiptOuterClass.Receipt voteReceipt = vote(proposalID);
        Assert.assertNotNull(voteReceipt);

        String appchainID = "did:bitxhub:appchain:.";

        ArgOuterClass.Arg[] ruleArgs = Types.toArgArray(
                Types.string(appchainID),
                Types.string(contractAddress),
                Types.string("ruleUrl"));
        ReceiptOuterClass.Receipt ruleReceipt = client.invokeBVMContract(BVMAddr.RULE_MANAGER_CONTRACT_ADDR, "RegisterRuleV2", ruleArgs);
        Assert.assertNotNull(ruleReceipt);

        proposalID = JSONObject.parseObject(ruleReceipt.getRet().toStringUtf8()).getString("proposal_id");
        voteReceipt = vote(proposalID);
        Assert.assertNotNull(voteReceipt);
    }

    ReceiptOuterClass.Receipt vote(String proposalID) {
        // vote for proposal
        ArgOuterClass.Arg[] voteArgs = Types.toArgArray(
                Types.string(proposalID),
                Types.string("approve"),
                Types.string("reason"));
        ReceiptOuterClass.Receipt voteReceipt = adminClient.invokeBVMContract(BVMAddr.GOVERNANCE_CONTRACT_ADDR, "Vote", voteArgs);
        return voteReceipt;
    }

    ReceiptOuterClass.Receipt register() {
        ArgOuterClass.Arg[] args = Types.toArgArray(
                Types.string("appchain0xF83625B55Db87bAca3862924777aaCf2E81Cb7eD"), //method
                Types.string("/ipfs/QmQVxzUqN2Yv2UHUQXYwH8dSNkM8ReJ9qPqwJsf8zzoNUi"), //docAddr
                Types.string("QmQVxzUqN2Yv2UHUQXYwH8dSNkM8ReJ9qPqwJsf8zzoNUi"), //docHash
                Types.string(""), //validators
                Types.string("rbft"), //consensus_type
                Types.string("hyperchain"), //chain_type
                Types.string("税务链"), //name
                Types.string("趣链税务链"), //desc
                Types.string("1.8"),//version
                Types.string(""), //public key
                Types.string("reason"), //reason
                Types.string("0x00000000000000000000000000000000000000a2"), //rule
                Types.string("")); //rule_url
        ReceiptOuterClass.Receipt receipt = client.invokeBVMContract(BVMAddr.APPCHAIN_MANAGER_CONTRACT_ADDR, "RegisterV2", args);
        Assert.assertNotNull(receipt);
        return receipt;
    }

}
