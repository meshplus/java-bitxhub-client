package cn.dmlab.bitxhub;


import cn.dmlab.crypto.ecdsa.ECKeyP256;
import cn.dmlab.crypto.ecdsa.ECKeyS256;
import com.google.common.base.Strings;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@AllArgsConstructor
@NoArgsConstructor
@Data
@Builder
public class Config {
    private String host;
    private Integer port;

    private ECKeyS256 ecKey;

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
        return new Config(host, port, ecKey);
    }
}
