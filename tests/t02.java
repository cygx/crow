import de.cygx.crow.*;
import java.io.*;
import java.util.Arrays;
import static de.cygx.crow.Constants.*;

class t02 implements Test {
    public static void main(String[] args) {
        new t02().run();
    }

    void _A1_build_record_request_frame() throws IOException {
        RequestFrame frame = RequestFrame.requestRecords("foo", "bar", "baz")
            .keepAlive().build();

        is(frame.type, RequestFrame.Type.RECORD);
        is(frame.size, 3);
        is(frame.keepAlive, true);
        is(frame.coding, RequestFrame.Coding.RAW);
    }

    void _B1_decode_record_request_frame() throws IOException {
        String[] names = { "hello world", "Käsekuchen", "αβγ" };
        String[] decodedNames = RequestFrame.requestRecords(names)
            .build().encode(true).decode(false).names();

        iseq(decodedNames, names);
    }

    void _B2_decode_deflated_record_request_frame() throws IOException {
        String[] names = { "hello world", "Käsekuchen", "αβγ" };
        String[] decodedNames = RequestFrame.requestRecords(names)
            .deflate().build().encode(true).decode(false).names();

        iseq(decodedNames, names);
    }

    void _B3_decode_blob_request_frame() throws IOException {
        int[] ids = { 42, 1 << 31, 23 << 17 | 1 };
        int[] decodedIds = RequestFrame.requestBlobs(ids)
            .build().encode(true).decode(false).ids();

        iseq(decodedIds, ids);
    }
}
