package cn.dmlab.bitxhub;

import cn.dmlab.utils.Utils;
import com.google.protobuf.ByteString;
import pb.ArgOuterClass;

import java.util.Objects;

/**
 * Convert Java types to gRPC Arg.
 */
public class Types {

    public static ArgOuterClass.Arg i32(Integer i) {
        return ArgOuterClass.Arg.newBuilder()
                .setType(ArgOuterClass.Arg.Type.I32)
                .setValue(ByteString.copyFrom(i.toString().getBytes())).build();
    }

    public static ArgOuterClass.Arg i64(Long i) {
        return ArgOuterClass.Arg.newBuilder()
                .setType(ArgOuterClass.Arg.Type.I64)
                .setValue(ByteString.copyFrom(i.toString().getBytes())).build();
    }

    public static ArgOuterClass.Arg u32(Integer i) {
        return ArgOuterClass.Arg.newBuilder()
                .setType(ArgOuterClass.Arg.Type.U32)
                .setValue(ByteString.copyFrom(i.toString().getBytes())).build();
    }

    public static ArgOuterClass.Arg u64(Long i) {
        return ArgOuterClass.Arg.newBuilder()
                .setType(ArgOuterClass.Arg.Type.U64)
                .setValue(ByteString.copyFrom(i.toString().getBytes())).build();
    }

    public static ArgOuterClass.Arg f32(Float f) {
        return ArgOuterClass.Arg.newBuilder()
                .setType(ArgOuterClass.Arg.Type.F32)
                .setValue(ByteString.copyFrom(f.toString().getBytes())).build();
    }

    public static ArgOuterClass.Arg f64(Double d) {
        return ArgOuterClass.Arg.newBuilder()
                .setType(ArgOuterClass.Arg.Type.F64)
                .setValue(ByteString.copyFrom(d.toString().getBytes())).build();
    }

    public static ArgOuterClass.Arg string(String str) {
        return ArgOuterClass.Arg.newBuilder()
                .setType(ArgOuterClass.Arg.Type.String)
                .setValue(ByteString.copyFrom(str.getBytes(Utils.DEFAULT_CHARSET))).build();
    }

    public static ArgOuterClass.Arg bytes(byte[] bytes) {
        return ArgOuterClass.Arg.newBuilder()
                .setType(ArgOuterClass.Arg.Type.Bytes)
                .setValue(ByteString.copyFrom(bytes)).build();
    }

    public static ArgOuterClass.Arg bool(Boolean b) {
        return ArgOuterClass.Arg.newBuilder()
                .setType(ArgOuterClass.Arg.Type.Bool)
                .setValue(ByteString.copyFrom(b.toString().getBytes())).build();
    }

    public static ArgOuterClass.Arg[] toArgArray(ArgOuterClass.Arg... args) {
        if (Objects.isNull(args) || args.length == 0) {
            return null;
        }
        return args;
    }

}
