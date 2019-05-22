package edu.columbia.cs.psl.struct;

import org.apache.http.HttpEntity;
import org.apache.http.HttpEntityEnclosingRequest;
import org.apache.http.HttpRequest;
import org.apache.http.impl.io.*;
import org.apache.http.io.HttpTransportMetrics;
import org.apache.http.io.SessionOutputBuffer;
import org.apache.http.message.BasicLineFormatter;
import org.apache.http.protocol.HTTP;
import org.apache.http.util.CharArrayBuffer;

import java.io.ByteArrayInputStream;
import java.io.OutputStream;
import java.util.Arrays;

/* Used to intercept the bytes of a request being written. */
public class ByteArrayGrabbingOutputBuffer implements SessionOutputBuffer {

    private static final byte[] CRLF = new byte[] {HTTP.CR, HTTP.LF};
    // The maximum length of the byte array
    private static final int MAX_CAPACITY = Integer.MAX_VALUE - 8;
    // Stores the bytes written to this buffer
    private byte[] bytes;
    // The number of bytes written to this buffer
    private int size;

    /* Constructs a new empty buffer instance with the specified initial capacity. */
    public ByteArrayGrabbingOutputBuffer(int initialCapacity) {
        this.bytes = new byte[initialCapacity];
        this.size = 0;
    }

    /* Returns a copy of the bytes written to this buffer. */
    public byte[] getBytes(boolean trim) {
        return trim ? Arrays.copyOfRange(bytes, 0, size) : bytes.clone();
    }

    /* Increases the size of the byte array so that it can hold at least the specified minimum number of elements. */
    public void ensureCapacity(int minCapacity) {
        int newCapacity = bytes.length + (bytes.length >> 1); // Increase capacity by 50%
        newCapacity = (newCapacity - minCapacity < 0) ? minCapacity : newCapacity;
        newCapacity = (newCapacity - MAX_CAPACITY > 0) ? MAX_CAPACITY : newCapacity;
        byte[] temp = bytes;
        bytes =  new byte[newCapacity];
        System.arraycopy(temp, 0, bytes, 0, size);
    }

    @Override
    public void write(byte[] b, int off, int len) {
        if(b != null && len > 0) {
            if(size + len > bytes.length) {
                ensureCapacity(size+len);
            }
            for(int i = off; i < off+len; i++) {
                bytes[size++] = b[i];
            }
        }
    }

    @Override
    public void write(byte[] b) {
        if(b != null) {
            write(b, 0, b.length);
        }
    }

    @Override
    public void write(int b) {
        if(size >= bytes.length) {
            ensureCapacity(size+1);
        }
        bytes[size++] =(byte)b;
    }

    @Override
    public void writeLine(String s) {
        if(s != null) {
            for(int i = 0; i < s.length(); i++) {
                write(s.charAt(i));
            }
            write(CRLF);
        }
    }

    @Override
    public void writeLine(CharArrayBuffer buffer) {
        if(buffer != null) {
            for(int i = 0; i < buffer.length(); i++) {
                write(buffer.charAt(i));
            }
            write(CRLF);
        }
    }

    @Override
    public void flush() {

    }

    @Override
    public HttpTransportMetrics getMetrics() {
        return null;
    }

    /* Structures the bytes in the specified array into an HttpUriRequest and then convert back to a byte array and return
     * that array. */
    @SuppressWarnings("unused")
    public static byte[] structureIntoRequest(byte[] bytes) {
        byte[] copy = bytes.clone();
        int size = bytes.length;
        try {
            HttpRequest request = requestFromBytes(bytes);
            return requestToBytes(request, size);
        } catch(Exception e) {
            e.printStackTrace();
            return copy;
        }
    }

    private static HttpRequest requestFromBytes(byte[] bytes) throws Exception {
        SessionInputBufferImpl sessionBuffer = new SessionInputBufferImpl(new HttpTransportMetricsImpl(), bytes.length);
        sessionBuffer.bind(new ByteArrayInputStream(bytes));
        DefaultHttpRequestParser parser = new DefaultHttpRequestParser(sessionBuffer);
        return parser.parse();
    }

    private static byte[] requestToBytes(HttpRequest request, int size) throws Exception {
        // Intercept the request's bytes as they are written
        ByteArrayGrabbingOutputBuffer sessionBuffer = new ByteArrayGrabbingOutputBuffer(size);
        DefaultHttpRequestWriter writer = new DefaultHttpRequestWriter(sessionBuffer, new BasicLineFormatter());
        writer.write(request);
        if(request instanceof HttpEntityEnclosingRequest) {
            HttpEntity entity = ((HttpEntityEnclosingRequest)request).getEntity();
            if(entity != null) {
                try(OutputStream stream = new ContentLengthOutputStream(sessionBuffer, entity.getContentLength())) {
                    entity.writeTo(stream);
                }
            }
        }
        return sessionBuffer.getBytes(false);
    }
}
