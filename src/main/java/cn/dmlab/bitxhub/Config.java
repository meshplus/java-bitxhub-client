package cn.dmlab.bitxhub;

import cn.dmlab.crypto.ecdsa.ECKeyS256;
import com.google.common.base.Strings;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.web3j.crypto.ECKeyPair;
import org.web3j.crypto.Keys;

import java.security.InvalidAlgorithmParameterException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;

@AllArgsConstructor
@NoArgsConstructor
@Data
@Builder
public class Config {
    private String host;
    private Integer port;

    private ECKeyS256 ecKey;
    private byte[] address;

    public void checkConfig() {
        if (Strings.isNullOrEmpty(host) || port == null) {
            throw new RuntimeException("address or port is empty");
        }
    }

    /**
     * Get default config
     */
    public static Config defaultConfig() {
        String host = "localhost";
        Integer port = 60011;
        ECKeyS256 ecKey = new ECKeyS256();
        byte[] address = ecKey.getAddress();
        return new Config(host, port, ecKey, address);
    }
}
