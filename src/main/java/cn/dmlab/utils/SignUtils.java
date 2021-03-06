package cn.dmlab.utils;

import cn.dmlab.crypto.HashUtil;
import cn.dmlab.crypto.ecdsa.ECKeyP256;
import cn.dmlab.crypto.ecdsa.ECKeyS256;
import com.google.protobuf.ByteString;
import org.web3j.crypto.ECKeyPair;
import org.web3j.crypto.Sign;
import pb.*;

public class SignUtils {
    /**
     * Sign tx and return signed tx
     *
     * @param unsignedTx unsigned tx
     * @param ecKeyS256  ecdsa key pair
     * @return sign tx
     */
    public static TransactionOuterClass.Transaction sign(TransactionOuterClass.Transaction unsignedTx
            , ECKeyS256 ecKeyS256) {
        byte[] txHash = needToHash(unsignedTx);
        byte[] signMessage = HashUtil.sha3(txHash);
        ECKeyS256.ECDSASignature sig = ecKeyS256.sign(signMessage);
        return unsignedTx.toBuilder()
                .setTransactionHash(ByteString.copyFrom(txHash)) //set tx hash
                .setSignature(ByteString.copyFrom(sig.toByteArray()))
                .build();
    }

    /**
     * Sign tx and return signed tx
     *
     * @param unsignedTx unsigned tx
     * @param ecKey      ecdsa key pair
     * @return sign tx
     */
    public static TransactionOuterClass.Transaction sign(TransactionOuterClass.Transaction unsignedTx
            , ECKeyP256 ecKey) {
        byte[] signMessage = HashUtil.sha3(needToHash(unsignedTx));
        ECKeyP256.ECDSASignature sig = ecKey.sign(signMessage);
        return unsignedTx.toBuilder()
                .setTransactionHash(ByteString.copyFrom(needToHash(unsignedTx))) //set tx hash
                .setSignature(ByteString.copyFrom(sig.toByteArray(ecKey.getPubKey())))
                .build();
    }

    public static byte[] needToHash(TransactionOuterClass.Transaction unsignedTx) {
        TransactionOuterClass.Transaction tx = TransactionOuterClass.Transaction.newBuilder()
                .setFrom(unsignedTx.getFrom())
                .setTo(unsignedTx.getTo())
                .setTimestamp(unsignedTx.getTimestamp())
                .setPayload(unsignedTx.getPayload())
                .setNonce(unsignedTx.getNonce())
                .setAmount(unsignedTx.getAmount()).build();
        if (unsignedTx.getIBTP() != Ibtp.IBTP.getDefaultInstance()) {
            tx.toBuilder().setIBTP(unsignedTx.getIBTP()).build();
        }
        return tx.toByteArray();
    }

    public static TransactionOuterClass.Transaction sign(TransactionOuterClass.Transaction unsignedTx, ECKeyPair ecKey) {
        byte[] signMessage = HashUtil.sha3(needToHash(unsignedTx));
        Sign.SignatureData sig = Sign.signMessage(signMessage, ecKey, false);
        return unsignedTx.toBuilder()
                .setTransactionHash(ByteString.copyFrom(needToHash(unsignedTx))) //set tx hash
                .setSignature(ByteString.copyFrom(toByteArray(sig)))
                .build();

    }

    public static byte[] toByteArray(Sign.SignatureData sig) {
        byte v = sig.getV()[0];
        final byte fixedV = v >= 27
                ? (byte) (v - 27)
                : v;

        return ByteUtil.merge(
                sig.getR(),
                sig.getS(),
                new byte[]{fixedV});
    }
}
