package cn.dmlab.crypto;

import com.google.common.hash.Hashing;
import org.web3j.crypto.Hash;

import static java.util.Arrays.copyOfRange;

public class HashUtil {

    /**
     * MessageDigest use SHA256Digest.
     *
     * @param data data for hash
     * @return result bytes
     */
    public static byte[] sha3(byte[] data) {
        return Hashing.sha256().hashBytes(data).asBytes();
    }


    /**
     * Calculates RIGTMOST160(SHA3(input)). This is used in address calculations.
     * *
     *
     * @param input - data
     * @return - 20 right bytes of the hash keccak of the data
     */
    public static byte[] sha3omit12(byte[] input) {
        byte[] hash = Hash.sha3(input);
        return copyOfRange(hash, 12, hash.length);
    }


}
