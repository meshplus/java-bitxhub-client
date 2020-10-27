package cn.dmlab.bitxhub;

import lombok.Data;

@Data
public class TransactOpts {
    private String from;
    private long normalNonce;
    private long iBTPNonce;
}
