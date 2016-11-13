package de.cygx.crow;
import java.io.IOException;

public class DecodingException extends IOException {
    public DecodingException() { super(); }
    public DecodingException(String msg) { super(msg); }
    public DecodingException(String msg, Throwable cause) { super(msg, cause); }
    public DecodingException(Throwable cause) { super(cause); }
}
