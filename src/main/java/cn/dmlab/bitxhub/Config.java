package cn.dmlab.bitxhub;

import cn.dmlab.crypto.ecdsa.ECKeyS256;
import com.google.common.base.Strings;
import io.grpc.netty.GrpcSslContexts;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLException;
import javax.net.ssl.SSLSession;
import javax.net.ssl.TrustManagerFactory;
import java.io.File;

@AllArgsConstructor
@NoArgsConstructor
@Data
@Builder
public class Config {
    private String host;
    private Integer port;

    private ECKeyS256 ecKey;
    private SslContext sslContext;

    public void checkConfig() {
        if (Strings.isNullOrEmpty(host) || port == null) {
            throw new RuntimeException("address or port is empty");
        }
    }

    public byte[] getAddress() {
        return ecKey.getAddress();
    }

    /**
     * Get default config
     */
    public static Config defaultConfig() {
        String host = "localhost";
        Integer port = 60011;
        ECKeyS256 ecKey = new ECKeyS256();
        return new Config(host, port, ecKey, null);
    }

    /**
     * Get default config
     */
    public static Config defaultConfigWithTLS() {
        // remember set yours hosts for tls such as [127.0.0.1 BitXHub]
        String host = "BitXHub";
        Integer port = 60011;
        ECKeyS256 ecKey = new ECKeyS256();
        byte[] address = ecKey.getAddress();

        // privKey need convert to pkcs8
        return new Config(host, port, ecKey,
                buildSslContext(Config.class.getClassLoader().getResource("agency.cert").getPath(),
                        Config.class.getClassLoader().getResource("gateway.cert").getPath(),
                        Config.class.getClassLoader().getResource("gateway.priv").getPath()));
    }

    /**
     * buildSslContext
     * @param trustCertCollectionFilePath
     * @param clientCertChainFilePath
     * @param clientPrivateKeyFilePath
     * @return
     * @throws SSLException
     */
    private static SslContext buildSslContext(String trustCertCollectionFilePath,
                                              String clientCertChainFilePath,
                                              String clientPrivateKeyFilePath) {
        SslContextBuilder builder = GrpcSslContexts.forClient();
        if (trustCertCollectionFilePath != null) {
            builder.trustManager(new File(trustCertCollectionFilePath));
        }
        if (clientCertChainFilePath != null && clientPrivateKeyFilePath != null) {
            builder.keyManager(new File(clientCertChainFilePath), new File(clientPrivateKeyFilePath));
        }
        SslContext sslContext = null;
        try {
            sslContext = builder.build();
        } catch (SSLException e) {
            throw new RuntimeException(e.getMessage(), e.getCause());
        }
        return sslContext;
    }
}
