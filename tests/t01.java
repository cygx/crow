import de.cygx.crow.*;
import java.io.*;
import static de.cygx.crow.Constants.*;
import static de.cygx.crow.Varint.*;

class t01 {
    public static void main(String[] args) {
        Test.run(t01.class);
    }

    static void roundtripVarint(long value) {
        try {
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            DataOutputStream dos = new DataOutputStream(bos);
            writeVarint(dos, value);

            ByteArrayInputStream bis =
                new ByteArrayInputStream(bos.toByteArray());
            DataInputStream dis = new DataInputStream(bis);
            long decodedValue = readVarint(dis);

            assert decodedValue == value : decodedValue + " != " + value;
        }
        catch(IOException e) { throw new RuntimeException(e); }
    }

    static void _01_varint() {
        roundtripVarint(0);
        roundtripVarint(1);
        roundtripVarint(0xFFFF);
    }

    static void _02_requestFrame() {
        RequestFrame frame =
            RequestFrame.requestRecords("foo", "bar", "baz").encode(true);

        assert frame.type() == RECORD_REQUEST   : "type is " + frame.type();
        assert frame.size() == 3                : "size is " + frame.size();
        assert frame.keepAlive() == true        : "keepAlive is "
                                                + frame.keepAlive();
        assert frame.representation() == 0      : "representation is "
                                                + frame.representation();
    }
}
