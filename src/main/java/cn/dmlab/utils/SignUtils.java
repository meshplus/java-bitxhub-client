package cn.dmlab.utils;

import cn.dmlab.crypto.HashUtil;
import cn.dmlab.crypto.ecdsa.ECKeyP256;
import cn.dmlab.crypto.ecdsa.ECKeyS256;
import com.google.protobuf.ByteString;
import pb.TransactionOuterClass;

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
        byte[] signMessage = HashUtil.sha3(needToHash(unsignedTx).getBytes(Utils.DEFAULT_CHARSET));
        ECKeyS256.ECDSASignature sig = ecKeyS256.sign(signMessage);
        return unsignedTx.toBuilder()
                .setTransactionHash(ByteString.copyFrom(new byte[32])) //set tx hash
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
        byte[] signMessage = HashUtil.sha3(needToHash(unsignedTx).getBytes(Utils.DEFAULT_CHARSET));
        ECKeyP256.ECDSASignature sig = ecKey.sign(signMessage);
        return unsignedTx.toBuilder()
                .setTransactionHash(ByteString.copyFrom(new byte[32])) //set tx hash
                .setSignature(ByteString.copyFrom(sig.toByteArray(ecKey.getPubKey())))
                .build();
    }

    public static String needToHash(TransactionOuterClass.Transaction unsignedTx) {
        String signMessage = String.format("from=%s&to=%s&timestamp=%d&nonce=%d&data=%s",
                ByteUtil.toHexStringWithOx(unsignedTx.getFrom().toByteArray()),
                ByteUtil.toHexStringWithOx(unsignedTx.getTo().toByteArray()),
                unsignedTx.getTimestamp(),
                unsignedTx.getNonce(),
                ByteUtil.toHexString(unsignedTx.getData().toByteArray()));
        return signMessage;
    }

}
