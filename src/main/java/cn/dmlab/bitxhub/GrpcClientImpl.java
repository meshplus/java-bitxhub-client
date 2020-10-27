package cn.dmlab.bitxhub;

import cn.dmlab.crypto.ecdsa.ECKeyS256;
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

import java.math.BigInteger;
import java.time.Duration;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

import org.web3j.crypto.Keys;
import pb.*;


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
        blockingStub = ChainBrokerGrpc.newBlockingStub(channel);
        asyncStub = ChainBrokerGrpc.newStub(channel);
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
    public void setECKey(ECKeyS256 ecKey) {
        check(!Objects.isNull(ecKey), "Ecdsa key  must not be null");
        this.config.setEcKey(ecKey);
    }


    @Override
    public String sendTransaction(TransactionOuterClass.Transaction transaction, TransactOpts opts) {
        check(!Objects.isNull(transaction.getFrom()), "From address must not be null");
        check(!Objects.isNull(transaction.getTo()), "To address must not be null");
        check(!Objects.isNull(transaction.getSignature()), "Signature must not be null");

        if (opts == null) {
            opts = new TransactOpts();
            opts.setFrom(Keys.toChecksumAddress(ByteUtil.toHex(transaction.getFrom().toByteArray())));
        }

        long nonce;
        if (opts.getNormalNonce() != 0 && opts.getIBTPNonce() != 0) {
            log.error("can't set ibtp nonce and normal nonce at the same time");
            return null;
        }
        if (opts.getNormalNonce() == 0 && opts.getIBTPNonce() == 0) {
            nonce = this.getPendingNonceByAccount(opts.getFrom());
        } else {
            if (opts.getIBTPNonce() != 0) {
                nonce = opts.getIBTPNonce();
            } else {
                nonce = opts.getNormalNonce();
            }
        }
        transaction = transaction.toBuilder().setNonce(nonce).build();
        TransactionOuterClass.Transaction signedTx = SignUtils.sign(transaction, config.getEcKey());
        Broker.TransactionHashMsg transactionHashMsg = blockingStub.sendTransaction(signedTx);

        if (transactionHashMsg == null) {
            log.warn("transactionHashMsg is null");
            return null;
        }
        return transactionHashMsg.getTxHash();
    }

    @Override
    public long getPendingNonceByAccount(String account) {
        Broker.Response pendingNonceByAccount = blockingStub.getPendingNonceByAccount(Broker.Address.newBuilder().setAddress(account).build());
        BigInteger nonce = new BigInteger(pendingNonceByAccount.getData().toStringUtf8());
        return nonce.longValue();
    }


    @Override
    public ReceiptOuterClass.Receipt sendTransactionWithReceipt(TransactionOuterClass.Transaction transaction, TransactOpts opts) {
        String txHash = this.sendTransaction(transaction, opts);
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
        check(contract != null, "Contract bytes must not be null");
        check(config.getEcKey() != null, "Ecdsa key must not be null");
        // build transaction with INVOKE type.
        TransactionOuterClass.TransactionData td = TransactionOuterClass.TransactionData.newBuilder()
                .setType(TransactionOuterClass.TransactionData.Type.INVOKE)
                .setVmType(TransactionOuterClass.TransactionData.VMType.XVM)
                .setPayload(ByteString.copyFrom(contract))
                .build();

        TransactionOuterClass.Transaction tx = TransactionOuterClass.Transaction.newBuilder()
                .setFrom(ByteString.copyFrom(config.getAddress()))
                .setTo(ByteString.copyFrom(new byte[20])) // set to_address 0
                .setNonce(Utils.genNonce())
                .setTimestamp(Utils.genTimestamp())
                .setPayload(td.toByteString())
                .build();

        ReceiptOuterClass.Receipt receipt = this.sendTransactionWithReceipt(tx, null);

        return ByteUtil.toHexStringWithOx(receipt.getRet().toByteArray());
    }

    @Override
    public ReceiptOuterClass.Receipt invokeContract(TransactionOuterClass.TransactionData.VMType vmType, String contractAddress, String method, ArgOuterClass.Arg... args) {
        check(!Strings.isNullOrEmpty(contractAddress), "Contract address must not be null or empty");
        check(!Strings.isNullOrEmpty(method), "Method must not be null or empty");
        check(config.getEcKey() != null, "Ecdsa key must not be null");

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
                .setFrom(ByteString.copyFrom(config.getAddress()))
                .setTo(ByteString.copyFrom(ByteUtil.hexStringToBytes(contractAddress)))
                .setPayload(td.toByteString())
                .setTimestamp(Utils.genTimestamp())
                .build();

        return this.sendTransactionWithReceipt(tx, null);
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
    public Broker.GetBlocksResponse getBlocks(Long start, Long end) {
        check(start >= 0, "Start must not be negative");
        check(end >= start, "End must not be negative");

        Broker.GetBlocksRequest request = Broker.GetBlocksRequest.newBuilder()
                .setStart(start)
                .setEnd(end)
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

    @Override
    public void getInterchainTxWrappers(String pid, Long begin, Long end, StreamObserver<Broker.InterchainTxWrappers> streamObserver) {
        check(!Strings.isNullOrEmpty(pid), "Id must not be null or empty");
        check(Objects.nonNull(streamObserver), "StreamObserver must not be null");
        check(begin >= 0, "begin must not be negative");
        check(end >= begin, "End must not be negative");
        Broker.GetInterchainTxWrappersRequest request = Broker.GetInterchainTxWrappersRequest.newBuilder()
                .setPid(pid)
                .setBegin(begin)
                .setEnd(end)
                .build();
        asyncStub.getInterchainTxWrappers(request, streamObserver);
    }

    @Override
    public void getBlockHeaders(Long begin, Long end, StreamObserver<BlockOuterClass.BlockHeader> streamObserver) {
        check(Objects.nonNull(streamObserver), "StreamObserver must not be null");
        check(begin >= 0, "begin must not be negative");
        check(end >= begin, "End must not be negative");

        Broker.GetBlockHeaderRequest request = Broker.GetBlockHeaderRequest.newBuilder()
                .setBegin(begin)
                .setEnd(end)
                .build();
        asyncStub.getBlockHeader(request, streamObserver);
    }

    private static void check(boolean test, String message) {
        if (!test) throw new IllegalArgumentException(message);
    }
}

