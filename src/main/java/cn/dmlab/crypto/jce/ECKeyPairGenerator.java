package cn.dmlab.crypto.jce;

import java.security.*;
import java.security.spec.ECGenParameterSpec;

public final class ECKeyPairGenerator {

    public static final String ALGORITHM = "EC";

    private static final String algorithmAssertionMsg =
            "Assumed JRE supports EC key pair generation";

    private static final String keySpecAssertionMsg =
            "Assumed correct key spec statically";

    private ECKeyPairGenerator() {
    }


    public static KeyPairGenerator getInstance(final Provider provider, final SecureRandom random, final String curveName) {
        try {
            ECGenParameterSpec curve
                    = new ECGenParameterSpec(curveName);
            final KeyPairGenerator gen = KeyPairGenerator.getInstance(ALGORITHM, provider);
            gen.initialize(curve, random);
            return gen;
        } catch (NoSuchAlgorithmException ex) {
            throw new AssertionError(algorithmAssertionMsg);
        } catch (InvalidAlgorithmParameterException ex) {
            throw new AssertionError(keySpecAssertionMsg);
        }
    }
}
