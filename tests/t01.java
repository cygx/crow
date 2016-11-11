import de.cygx.crow.*;
import java.io.*;
import static de.cygx.crow.Varint.*;

class t01 {
    public static void main(String[] args) {
        Test.run(t01.class);
    }

    static void roundtripVarint(long value) throws IOException {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        DataOutputStream dos = new DataOutputStream(bos);
        writeVarint(dos, value);

        ByteArrayInputStream bis =
            new ByteArrayInputStream(bos.toByteArray());
        DataInputStream dis = new DataInputStream(bis);
        long decodedValue = readVarint(dis);

        assert decodedValue == value:
            decodedValue + " != " + value;
    }

    static void _01_roundtrip_varints() throws IOException {
        roundtripVarint(0);
        roundtripVarint(1);
        roundtripVarint(0xFFFF);
    }
}
