package de.cygx.crow;
import java.io.IOException;

public class EncodingException extends IOException {
    public EncodingException() { super(); }
    public EncodingException(String msg) { super(msg); }
    public EncodingException(String msg, Throwable cause) { super(msg, cause); }
    public EncodingException(Throwable cause) { super(cause); }
}
