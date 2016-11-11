import de.cygx.crow.*;
import java.io.*;
import java.util.Arrays;
import static de.cygx.crow.Constants.*;

class t02 {
    public static void main(String[] args) {
        Test.run(t02.class);
    }

    static void _01_create_record_request_frame() throws IOException {
        RequestFrame frame =
            RequestFrame.requestRecords("foo", "bar", "baz").encode(true);

        assert frame.type() == RECORD_REQUEST:
            "type is " + frame.type();

        assert frame.size() == 3:
            "size is " + frame.size();

        assert frame.keepAlive() == true:
            "keepAlive is " + frame.keepAlive();

        assert frame.representation() == 0:
            "representation is " + frame.representation();
    }

    static void _02_decode_blob_request_frame() throws IOException {
        int[] ids = { 42, 1 << 31, 23 << 17 | 1 };
        RequestFrame frame = RequestFrame.requestBlobs(ids).encode(false);
        RequestFrame decodedFrame = frame.decode();
        int[] decodedIds = decodedFrame.blobIds();

        assert Arrays.equals(decodedIds, ids):
            Arrays.toString(decodedIds) + " != " + Arrays.toString(ids);
    }
}
