package cn.dmlab.bitxhub;

import cn.dmlab.crypto.ecdsa.ECKeyS256;
import io.grpc.stub.StreamObserver;
import org.web3j.crypto.ECKeyPair;
import pb.*;


public interface GrpcClient {
    /**
     * Close all connections between BitXHub and the client.
     *
     * @throws InterruptedException
     */
    void stop() throws InterruptedException;

    /**
     * Subscribe to event notifications from BitXHub.
     *
     * @param type     subscribe type
     * @param observer contain methods of the onNext(), onComplete(), OnError().
     */
    void subscribe(pb.Broker.SubscriptionRequest.Type type, StreamObserver<Broker.Response> observer);

    /**
     * Reset ecdsa key.
     *
     * @param ecKey ecdsa key
     */
    void setECKey(ECKeyS256 ecKey);

    /**
     * Send a unsign transaction to BitXHub. If the signature is illegal,
     * the transaction hash will be obtained but the transaction receipt is illegal.
     *
     * @param transaction Unsigned transaction
     * @return tx hash
     */
    String sendTransaction(Transaction.BxhTransaction transaction, TransactOpts opts);


    /**
     * Send a signed transaction to BitXHub. If the signature is illegal,
     * the transaction hash will be obtained but the transaction receipt is illegal.
     *
     * @param transaction Unsigned transaction
     * @return tx hash
     */
    String sendSignedTransaction(Transaction.BxhTransaction transaction);


    /**
     *  returns the latest nonce of an account in the pending status,
     * 	and it should be the nonce for next transaction
     * @param account
     * @return
     */
    long getPendingNonceByAccount(String account);
    /**
     * Get the receipt by transaction hash,
     * the status of the receipt is a sign of whether the transaction is successful.
     *
     * @param hash tx hash
     * @return tx receipt
     */
    ReceiptOuterClass.Receipt getReceipt(String hash);

    /**
     * Get transaction from BitXHub by transaction hash.
     *
     * @param hash tx hash
     * @return transaction
     */
    Broker.GetTransactionResponse getTransaction(String hash);

    /**
     * Send tx to chain and get the receipt.
     *
     * @param transaction Unsigned transaction
     * @return tx receipt
     */
    ReceiptOuterClass.Receipt sendTransactionWithReceipt(Transaction.BxhTransaction transaction, TransactOpts opts);

    /**
     * Generate contract transaction with necessary parameters.
     * @param vmType          VM type
     * @param contractAddress contract address
     * @param method          contract method
     * @param args            method args
     * @return transaction
     */
    Transaction.BxhTransaction generateContractTx(Transaction.TransactionData.VMType vmType, String contractAddress, String method, ArgOuterClass.Arg... args);

    /**
     * Send view tx to chain and get the receipt.
     *
     * @param transaction Unsigned transaction
     * @return tx receipt
     */
    ReceiptOuterClass.Receipt sendView(Transaction.BxhTransaction transaction);
    /**
     * Obtain block information from BitXHub.
     * The block header contains the basic information of the block,
     * and the block body contains all the transactions packaged.
     *
     * @param value value
     * @param type  height or hash
     * @return
     */
    BlockOuterClass.Block getBlock(String value, Broker.GetBlockRequest.Type type);

    /**
     * Get blocks of the specified block height range.
     *
     * @param start start signal
     * @param end   end signal
     * @return blocks info
     */
    Broker.GetBlocksResponse getBlocks(Long start, Long end);

    /**
     * Get the status of the blockchain from BitXHub, normal or abnormal.
     *
     * @return block status
     */
    Broker.Response getChainStatus();

    /**
     * Get the validators from BitXHub.
     *
     * @return validators
     */
    Broker.Response getValidators();


    /**
     * Get the current network situation of BitXHub.
     *
     * @return network meta
     */
    Broker.Response getNetworkMeta();

    /**
     * Get account balance from BitXHub by address
     *
     * @param address account address
     * @return balance
     */
    Broker.Response getAccountBalance(String address);

    /**
     * Get the current blockchain situation of BitXHub.
     *
     * @return chain meta
     */
    Chain.ChainMeta getChainMeta();

    /**
     * Through the InterchainTxWrapper structure, you can quickly obtain cross-chain transactions in a block.
     *
     * @param pid            app-chain id.
     * @param begin          begin signal
     * @param end            end signal
     * @param streamObserver contain methods of the onNext(), onComplete(), OnError().
     * @return merkle wrappers
     */
    void getInterchainTxWrappers(String pid, Long begin, Long end, StreamObserver<Broker.InterchainTxWrappers> streamObserver);

    /**
     * Get the missing block header from BitXHub
     *
     * @param begin          begin signal
     * @param end            end signal
     * @param streamObserver contain methods of the onNext(), onComplete(), OnError().
     */
    void getBlockHeaders(Long begin, Long end, StreamObserver<BlockOuterClass.BlockHeader> streamObserver);

    /**
     * Call this interface to deploy a WASM contract to BitXHub
     *
     * @param contract contract bytes array
     * @return contract address
     */
    String deployContract(byte[] contract);

    /**
     * Call the specific method in the contract, and then get the transaction receipt
     *
     * @param vmType          VM type
     * @param contractAddress contract address
     * @param method          contract method
     * @param args            method args
     * @return transaction receipt
     */
    ReceiptOuterClass.Receipt invokeContract(Transaction.TransactionData.VMType vmType, String contractAddress, String method, ArgOuterClass.Arg... args);

    /**
     * Invoke the BVM contract, BVM is BitXHub's blot contract.
     *
     * @param contractAddress contract address
     * @param method          contract method
     * @param args            method args
     * @return transaction receipt
     */
    ReceiptOuterClass.Receipt invokeBVMContract(String contractAddress, String method, ArgOuterClass.Arg... args);

    /**
     * Invoke the XVM contract, XVM is WebAssembly contract.
     *
     * @param contractAddress contract address
     * @param method          contract method
     * @param args            method args
     * @return transaction receipt
     */
    ReceiptOuterClass.Receipt invokeXVMContract(String contractAddress, String method, ArgOuterClass.Arg... args);

}
