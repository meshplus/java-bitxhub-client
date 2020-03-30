package cn.dmlab.crypto.ecdsa;

import cn.dmlab.crypto.HashUtil;

public class ECUtil {

    /**
     * verify ECDSA signature.
     *
     * @param sourceData source data
     * @param signature  signature
     * @param publicKey  public key
     * @return is legal
     */
    public static boolean verify(byte[] sourceData, byte[] signature, byte[] publicKey) {
        byte[] hash = HashUtil.sha3(sourceData);
        byte[] r = new byte[32];
        byte[] s = new byte[32];
        System.arraycopy(signature, 0, r, 0, 32);
        System.arraycopy(signature, 32, s, 0, 32);
        ECKeyS256.ECDSASignature ecdsaSignature = ECKeyS256.ECDSASignature.fromComponents(r, s, signature[signature.length - 1]);
        return ECKeyS256.verify(hash, ecdsaSignature, publicKey);
    }

    /**
     * verify ECDSA signature.
     *
     * @param sourceData source data
     * @param signature  signature
     * @param ecKeyS256  {@link ECKeyS256}
     * @return is legal
     */
    public static boolean verify(byte[] sourceData, byte[] signature, ECKeyS256 ecKeyS256) {
        byte[] hash = HashUtil.sha3(sourceData);
        byte[] r = new byte[32];
        byte[] s = new byte[32];
        System.arraycopy(signature, 0, r, 0, 32);
        System.arraycopy(signature, 32, s, 0, 32);
        ECKeyS256.ECDSASignature ecdsaSignature = ECKeyS256.ECDSASignature.fromComponents(r, s, signature[signature.length - 1]);
        return ecKeyS256.verify(hash, ecdsaSignature);
    }

    /**
     * verify ECDSA signature.
     *
     * @param sourceData source data
     * @param signature  signature
     * @param ecKeyP256  {@link ECKeyP256}
     * @return is legal
     */
    public static boolean verify(byte[] sourceData, byte[] signature, ECKeyP256 ecKeyP256) {
        byte[] hash = HashUtil.sha3(sourceData);
        byte[] r = new byte[32];
        byte[] s = new byte[32];
        System.arraycopy(signature, 0, r, 0, 32);
        System.arraycopy(signature, 32, s, 0, 32);
        ECKeyP256.ECDSASignature ecdsaSignature = ECKeyP256.ECDSASignature.fromComponents(r, s, signature[64]);
        return ecKeyP256.verify(hash, ecdsaSignature);
    }
}
