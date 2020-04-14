package cn.dmlab.bitxhub;

import cn.dmlab.crypto.ecdsa.ECKeyP256;
import cn.dmlab.utils.ByteUtil;
import cn.dmlab.utils.SignUtils;
import cn.dmlab.utils.Utils;
import com.google.common.base.Strings;
import com.google.protobuf.ByteString;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.StatusRuntimeException;
import io.grpc.stub.StreamObserver;
import lombok.extern.slf4j.Slf4j;
import net.jodah.failsafe.Failsafe;
import net.jodah.failsafe.RetryPolicy;
import net.jodah.failsafe.function.CheckedSupplier;
import pb.*;

import java.time.Duration;
import java.util.Objects;
import java.util.concurrent.TimeUnit;


@Slf4j
public class GrpcClientImpl implements GrpcClient {

    private final ManagedChannel channel;
    private final ChainBrokerGrpc.ChainBrokerBlockingStub blockingStub;
    private final ChainBrokerGrpc.ChainBrokerStub asyncStub;


    private Config config;

    /**
     * Construct client connecting to server at {@code host:port}.
     */
    public GrpcClientImpl(Config config) {
        if (config == null) {
            config = Config.defaultConfig();
        }
        config.checkConfig();
        this.config = config;
        this.channel = ManagedChannelBuilder.forAddress(config.getHost(), config.getPort())
                .usePlaintext()
                .build();
        blockingStub = ChainBrokerGrpc.newBlockingStub(channel)
                .withDeadlineAfter(20, TimeUnit.SECONDS);
        asyncStub = ChainBrokerGrpc.newStub(channel).withDeadlineAfter(20, TimeUnit.SECONDS);


    }


    public void shutdown() throws InterruptedException {
        channel.shutdown().awaitTermination(5, TimeUnit.SECONDS);
    }

    /**
     * stop client.
     */
    @Override
    public void stop() throws InterruptedException {
        shutdown();
    }

    @Override
    public void subscribe(Broker.SubscriptionRequest.Type type, StreamObserver<Broker.Response> observer) {
        check(Objects.nonNull(observer), "Observer must not be null");
        check(Objects.nonNull(type), "Subscription type must not be null");
        Broker.SubscriptionRequest request = Broker.SubscriptionRequest.newBuilder()
                .setType(type)
                .build();
        asyncStub.subscribe(request, observer);
    }

    @Override
    public void setECKey(ECKeyP256 ecKey) {
        check(!Objects.isNull(ecKey), "Ecdsa key  must not be null");
        this.config.setEcKey(ecKey);
    }


    @Override
    public String sendTransaction(TransactionOuterClass.Transaction transaction) {
        check(!Objects.isNull(transaction.getFrom()), "From address must not be null");
        check(!Objects.isNull(transaction.getTo()), "To address must not be null");
        check(!Objects.isNull(transaction.getSignature()), "Signature must not be null");

        Broker.SendTransactionRequest req = Broker.SendTransactionRequest.newBuilder()
                .setVersion(transaction.getVersion())
                .setFrom(transaction.getFrom())
                .setTo(transaction.getTo())
                .setTimestamp(transaction.getTimestamp())
                .setData(transaction.getData())
                .setNonce(transaction.getNonce())
                .setSignature(transaction.getSignature())
                .setExtra(transaction.getExtra())
                .build();


        Broker.TransactionHashMsg transactionHashMsg = blockingStub.sendTransaction(req);

        if (transactionHashMsg == null) {
            log.warn("transactionHashMsg is null");
            return null;
        }
        return transactionHashMsg.getTxHash();
    }


    @Override
    public ReceiptOuterClass.Receipt sendTransactionWithReceipt(TransactionOuterClass.Transaction transaction) {
        String txHash = this.sendTransaction(transaction);
        return this.getReceipt(txHash);
    }


    @Override
    public ReceiptOuterClass.Receipt getReceipt(String hash) {
        check(!Strings.isNullOrEmpty(hash), "Hash must not be null or empty");

        Broker.TransactionHashMsg transactionHashMsg = Broker.TransactionHashMsg.newBuilder()
                .setTxHash(hash)
                .build();

        // The newest transaction may not get the receipt. waiting for block mint
        RetryPolicy<Object> retryPolicy = new RetryPolicy<>()
                .handle(StatusRuntimeException.class)
                .withDelay(Duration.ofMillis(500L))
                .withMaxRetries(5);

        return (ReceiptOuterClass.Receipt) Failsafe.with(retryPolicy)
                .get((CheckedSupplier<Object>) () -> blockingStub.getReceipt(transactionHashMsg));
    }


    @Override
    public Broker.GetTransactionResponse getTransaction(String hash) {
        check(!Strings.isNullOrEmpty(hash), "Hash must not be null or empty");

        Broker.TransactionHashMsg transactionHashMsg = Broker.TransactionHashMsg.newBuilder()
                .setTxHash(hash)
                .build();

        return blockingStub.getTransaction(transactionHashMsg);
    }

    @Override
    public BlockOuterClass.Block getBlock(String value, Broker.GetBlockRequest.Type type) {
        check(!Strings.isNullOrEmpty(value), "Value must not be null or empty");
        check(!Objects.isNull(type), "Type must not be null");
        Broker.GetBlockRequest request = Broker.GetBlockRequest.newBuilder()
                .setValue(value)
                .setType(type)
                .build();
        return blockingStub.getBlock(request);
    }


    @Override
    public Broker.Response getChainStatus() {
        Broker.Request request = Broker.Request.newBuilder()
                .setType(Broker.Request.Type.CHAIN_STATUS)
                .build();
        return blockingStub.getInfo(request);
    }

    @Override
    public Broker.Response getValidators() {
        Broker.Request request = Broker.Request.newBuilder()
                .setType(Broker.Request.Type.VALIDATORS)
                .build();
        return blockingStub.getInfo(request);
    }

    @Override
    public String deployContract(byte[] contract) {
        check(contract != null, "Contract'bytes must not be null");

        // build transaction with INVOKE type.
        TransactionOuterClass.TransactionData td = TransactionOuterClass.TransactionData.newBuilder()
                .setType(TransactionOuterClass.TransactionData.Type.INVOKE)
                .setVmType(TransactionOuterClass.TransactionData.VMType.XVM)
                .setPayload(ByteString.copyFrom(contract))
                .build();

        TransactionOuterClass.Transaction tx = TransactionOuterClass.Transaction.newBuilder()
                .setFrom(ByteString.copyFrom(config.getEcKey().getAddress()))
                .setTo(ByteString.copyFrom(new byte[20])) // set to_address 0
                .setNonce(Utils.genNonce())
                .setTimestamp(Utils.genTimestamp())
                .setData(td)
                .build();
        TransactionOuterClass.Transaction signedTx = SignUtils.sign(tx, config.getEcKey());

        ReceiptOuterClass.Receipt receipt = this.sendTransactionWithReceipt(signedTx);

        return ByteUtil.toHexStringWithOx(receipt.getRet().toByteArray());
    }

    @Override
    public ReceiptOuterClass.Receipt invokeContract(TransactionOuterClass.TransactionData.VMType vmType, String contractAddress, String method, ArgOuterClass.Arg... args) {
        check(!Strings.isNullOrEmpty(contractAddress), "Contract address must not be null or empty");
        check(!Strings.isNullOrEmpty(method), "Method must not be null or empty");

        TransactionOuterClass.InvokePayload invokePayload = TransactionOuterClass.InvokePayload.newBuilder()
                .setMethod(method)
                .build();

        if (args != null) {
            for (ArgOuterClass.Arg arg : args) {
                invokePayload = invokePayload.toBuilder().addArgs(arg).build();
            }
        }

        TransactionOuterClass.TransactionData td = TransactionOuterClass.TransactionData.newBuilder()
                .setVmType(vmType)
                .setType(TransactionOuterClass.TransactionData.Type.INVOKE)
                .setPayload(invokePayload.toByteString())
                .build();

        TransactionOuterClass.Transaction tx = TransactionOuterClass.Transaction.newBuilder()
                .setFrom(ByteString.copyFrom(config.getEcKey().getAddress()))
                .setTo(ByteString.copyFrom(ByteUtil.hexStringToBytes(contractAddress)))
                .setData(td)
                .setTimestamp(Utils.genTimestamp())
                .setNonce(Utils.genNonce())
                .build();

        TransactionOuterClass.Transaction signedTx = SignUtils.sign(tx, config.getEcKey());
        return this.sendTransactionWithReceipt(signedTx);
    }

    @Override
    public ReceiptOuterClass.Receipt invokeBVMContract(String contractAddress, String method, ArgOuterClass.Arg... args) {
        return this.invokeContract(TransactionOuterClass.TransactionData.VMType.BVM,
                contractAddress, method, args);
    }

    @Override
    public ReceiptOuterClass.Receipt invokeXVMContract(String contractAddress, String method, ArgOuterClass.Arg... args) {
        return this.invokeContract(TransactionOuterClass.TransactionData.VMType.XVM,
                contractAddress, method, args);
    }

    @Override
    public Broker.GetBlocksResponse getBlocks(Long offset, Long length) {
        check(offset >= 0, "Offset must not be negative");
        check(length >= 0, "Length must not be negative");

        Broker.GetBlocksRequest request = Broker.GetBlocksRequest.newBuilder()
                .setLength(length)
                .setOffset(offset)
                .build();
        return blockingStub.getBlocks(request);
    }

    @Override
    public Broker.Response getNetworkMeta() {
        Broker.Request request = Broker.Request.newBuilder()
                .setType(Broker.Request.Type.NETWORK)
                .build();
        return blockingStub.getInfo(request);
    }

    @Override
    public Broker.Response getAccountBalance(String address) {
        check(!Strings.isNullOrEmpty(address), "Address must not be null or empty");
        Broker.Address request = Broker.Address.newBuilder()
                .setAddress(address)
                .build();
        return blockingStub.getAccountBalance(request);
    }

    @Override
    public Chain.ChainMeta getChainMeta() {
        Broker.Request request = Broker.Request.newBuilder().build();
        return blockingStub.getChainMeta(request);
    }

    /**
     * Pier sync merkle wrapper from bitxhub.
     */
    @Override
    public void syncMerkleWrapper(String id, StreamObserver<Broker.Response> streamObserver) {
        check(!Strings.isNullOrEmpty(id), "Id must not be null or empty");
        check(Objects.nonNull(streamObserver), "StreamObserver must not be null");
        Broker.SyncMerkleWrapperRequest request = Broker.SyncMerkleWrapperRequest.newBuilder()
                .setAppchainId(id)
                .build();
        asyncStub.syncMerkleWrapper(request, streamObserver);
    }

    @Override
    public void getMerkleWrapper(String pid, Long begin, Long end, StreamObserver<Broker.Response> streamObserver) {
        check(!Strings.isNullOrEmpty(pid), "Id must not be null or empty");
        check(begin > 0, "The number of begin must not be negative");
        check(end >= begin, "The number of end cannot be smaller than the number of begin");
        check(Objects.nonNull(streamObserver), "StreamObserver must not be null");

        Broker.GetMerkleWrapperRequest request = Broker.GetMerkleWrapperRequest.newBuilder()
                .setPid(pid)
                .setBegin(begin)
                .setEnd(end)
                .build();
        asyncStub.getMerkleWrapper(request, streamObserver);
    }


    private static void check(boolean test, String message) {
        if (!test) throw new IllegalArgumentException(message);
    }
}

