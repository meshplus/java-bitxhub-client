package cn.dmlab.bitxhub;

import cn.dmlab.crypto.ecdsa.ECKeyP256;
import io.grpc.stub.StreamObserver;
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
    void subscribe(Broker.SubscriptionRequest.Type type, StreamObserver<Broker.Response> observer);

    /**
     * Reset ecdsa key.
     *
     * @param ecKey ecdsa key
     */
    void setECKey(ECKeyP256 ecKey);

    /**
     * Send a signed transaction to BitXHub. If the signature is illegal,
     * the transaction hash will be obtained but the transaction receipt is illegal.
     *
     * @param transaction Unsigned transaction
     * @return tx hash
     */
    String sendTransaction(TransactionOuterClass.Transaction transaction);

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
    ReceiptOuterClass.Receipt sendTransactionWithReceipt(TransactionOuterClass.Transaction transaction);


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
     * @param offset block height offset
     * @param length range of blocks
     * @return blocks info
     */
    Broker.GetBlocksResponse getBlocks(Long offset, Long length);

    /**
     * Get the status of the blockchain from BitXHub, normal or abnormal.
     *
     * @return block status
     */
    Broker.Response getChainStatus();


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
     * Sync merkle wrapper from BitXHub, A merkle wrapper is a structure containing a merkle tree.
     * Through this structure, you can quickly obtain cross-chain transactions in a block.
     *
     * @param pid            app-chain id.
     * @param streamObserver contain methods of the onNext(), onComplete(), OnError().
     * @return merkle wrappers
     */
    void syncMerkleWrapper(String pid, StreamObserver<Broker.Response> streamObserver);

    /**
     * Get the range of merkle wrapper from BitXHub.
     *
     * @param pid            app-chain id
     * @param begin          begin of the block height
     * @param end            end of the block height
     * @param streamObserver contain methods of the onNext(), onComplete(), OnError()
     */
    void getMerkleWrapper(String pid, Long begin, Long end, StreamObserver<Broker.Response> streamObserver);

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
    ReceiptOuterClass.Receipt invokeContract(TransactionOuterClass.TransactionData.VMType vmType, String contractAddress, String method, ArgOuterClass.Arg... args);

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
