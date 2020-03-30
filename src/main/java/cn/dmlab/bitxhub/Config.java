package cn.dmlab.bitxhub;


import cn.dmlab.crypto.ecdsa.ECKeyP256;
import com.google.common.base.Strings;
import lombok.*;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Builder
public class Config {
    private String host;
    private Integer port;

    @Setter
    private ECKeyP256 ecKey;

    public void checkConfig() {
        if (ecKey == null) {
            throw new RuntimeException("ecKeyPair key is empty");
        }

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
        ECKeyP256 ecKey = new ECKeyP256();
        return new Config(host, port, ecKey);
    }
}
