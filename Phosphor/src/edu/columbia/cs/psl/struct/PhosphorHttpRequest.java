package edu.columbia.cs.psl.struct;

import org.apache.http.*;
import org.apache.http.entity.*;
import org.apache.http.impl.entity.StrictContentLengthStrategy;
import org.apache.http.impl.io.*;
import org.apache.http.protocol.HTTP;
import org.apache.http.util.EntityUtils;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.LinkedList;

/* Represents an HTTP request. Used as to convert HttpRequest objects into byte arrays. */
public class PhosphorHttpRequest implements Serializable {

    private static final long serialVersionUID = -2410222280911896035L;
    // Carriage return and line feed characters used to indicate the end of a line.
    private static final String CRLF = "\r\n";
    // The string name of a cookie header
    private static final String COOKIE_HEADER = "Cookie";

    // Part of the request line, indicates the method to be performed
    private final String method;
    // Part of the request line, indicates the resource the request applies to
    private final URI uri;
    // Part of the request line, indicates the version of HTTP used
    private final String protocolVersion;
    // List of HTTP message headers for the request
    private final LinkedList<Header> headers;
    // The String value of the cookie header or null if undefined
    private String cookieHeaderVal = null;
    // The String value of the content-encoding header or null if undefined
    private String contentEncodingHeaderVal = null;
    // The String value of the content-type header or null if undefined
    private String contentTypeHeaderVal = null;
    // The String entity body of the request or null if no entity body is present
    private final String entityBody;

    /* Constructs a new PhosphorHttpRequest containing information from the specified HttpRequest. */
    public PhosphorHttpRequest(HttpRequest request, HttpEntity entity) throws URISyntaxException, IOException {
        this.method = request.getRequestLine().getMethod();
        this.uri = new URI(request.getRequestLine().getUri());
        this.protocolVersion = request.getProtocolVersion().toString();
        this.headers = new LinkedList<>();
        for(Header header : request.getAllHeaders()) {
            if(header.getName().equalsIgnoreCase(COOKIE_HEADER)) {
                this.cookieHeaderVal = header.getValue();
            } else if(header.getName().equalsIgnoreCase(HttpHeaders.CONTENT_ENCODING)) {
                this.contentEncodingHeaderVal = header.getValue();
            } else if(header.getName().equalsIgnoreCase(HttpHeaders.CONTENT_TYPE)) {
                this.contentTypeHeaderVal = header.getValue();
            } else if(!header.getName().equalsIgnoreCase(HttpHeaders.CONTENT_LENGTH)) {
                this.headers.add(header);
            }
        }
        if(entity != null) {
            this.entityBody = EntityUtils.toString(entity);
            EntityUtils.consume(entity);
        } else {
            this.entityBody = null;
        }
    }

    /* Returns the HTTP method specifying the action to be performed by this request. */
    String getMethod() {
        return method;
    }

    /* Returns this request's URI's scheme or null if undefined. */
    private String getScheme() {
        return uri.getScheme();
    }

    /* Returns the scheme specified part of the uri or null if undefined. */
    private String getSchemeSpecificPart() {
        return uri.getRawSchemeSpecificPart();
    }

    /* Returns this request's URI's host or null if undefined. */
    private String getHost() {
        return uri.getHost();
    }

    /* Returns this request's URI's user-information or null if undefined. */
    private String getUserInfo() {
        return uri.getRawUserInfo();
    }

    /* Returns this request's URI's port or -1 if undefined. */
    private int getPort() {
        return uri.getPort();
    }

    /* Returns this request's URI's authority component or null if undefined. */
    private String getAuthority() {
        return uri.getRawAuthority();
    }

    /* Returns this request's URI's path or null if undefined. */
    private String getPath() {
        return uri.getRawPath();
    }

    /* Returns this request's URI's fragment component or null if undefined. */
    private String getFragment() {
        return uri.getRawFragment();
    }

    /* Returns this request's URI's query or null if undefined. */
    private String getQuery() {
        return uri.getQuery();
    }

    /* Returns a string indicating the version of HTTP used for this request. */
    private String getProtocolVersion() {
        return protocolVersion;
    }

    /* Returns the header list for this request. */
    private LinkedList<Header> getHeaders() {
        return headers;
    }

    /* Returns the value of the cookie header or null if undefined. */
    private String getCookie() {
        return cookieHeaderVal;
    }

    /* Returns the value of the content-type header or null if undefined. */
    private String getContentType() {
        return contentTypeHeaderVal;
    }

    /* Returns the value of the content-encoding header or null if undefined. */
    private String getContentEncoding() {
        return contentEncodingHeaderVal;
    }

    /* Returns the string value of this request's entity body or null if no entity body is present. */
    private String getEntityBody() {
        return entityBody;
    }

    /* Adds this request's URI information to the specified StringBuilder. */
    private void addURIInfo(StringBuilder builder) {
        String fragment = getFragment();
        if(getScheme() != null) {
            builder.append(getScheme()).append(':');
        }
        if(uri.isOpaque()) {
            builder.append(getSchemeSpecificPart());
        } else {
            String host = getHost();
            int port = getPort();
            String authority = getAuthority();
            String path = getPath();
            String query = getQuery();
            if(host != null) {
                builder.append("//");
                if(getUserInfo() != null) {
                    builder.append(getUserInfo()).append('@');
                }
                boolean needBrackets = ((host.indexOf(':') >= 0) && !host.startsWith("[") && !host.endsWith("]"));
                if(needBrackets) {
                    builder.append('[');
                }
                builder.append(host);
                if(needBrackets) {
                    builder.append(']');
                }
                if(port != -1) {
                    builder.append(':').append(port);
                }
            } else if(authority != null) {
                builder.append("//").append(authority);
            }
            if(path != null) {
                builder.append(path);
            }
            if(query != null) {
                builder.append('?').append(query);
            }
        }
        if(fragment != null) {
            builder.append('#').append(fragment);
        }
    }

    /* Adds this request's non-entity header information to the specified StringBuilder. */
    private void addHeaderInfo(StringBuilder builder) {
        String cookieVal = getCookie();
        if(cookieVal != null) {
            builder.append(COOKIE_HEADER).append(": ").append(cookieVal).append(CRLF);
        }
        for(Header header : getHeaders()) {
            builder.append(header.getName()).append(": ").append(header.getValue()).append(CRLF);
        }
    }

    /* Adds this request's entity headers and entity body to the specified StringBuilder. */
    private void addEntityInfo(StringBuilder builder) {
        String type = getContentType();
        String encoding = getContentEncoding();
        String body = getEntityBody();
        if(type != null) {
            builder.append(HTTP.CONTENT_TYPE).append(": ").append(type).append(CRLF);
        }
        if(encoding != null) {
            builder.append(HTTP.CONTENT_ENCODING).append(": ").append(encoding).append(CRLF);
        }
        if(body != null) {
            builder.append(HTTP.CONTENT_LEN).append(": ").append(body.length()).append(CRLF);
            builder.append(CRLF).append(body);
        } else {
            builder.append(HTTP.CONTENT_LEN).append(": ").append("0").append(CRLF).append(CRLF);
        }
    }

    /* Returns a text representation of this request. */
    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append(getMethod()).append(' ');
        addURIInfo(builder);
        builder.append(' ').append(getProtocolVersion()).append(CRLF);
        addHeaderInfo(builder);
        addEntityInfo(builder);
        return builder.toString();
    }

    /* Structures the bytes in the specified array into a PhosphorHttpRequest and then converts back to a byte array
     * which is returned. */
    @SuppressWarnings("unused")
    public static byte[] structureIntoRequest(int[] lastValidUpdate, byte[] bytes, int startingPosition, int lastValid) {
        byte[] copy = bytes.clone();
        int size = bytes.length;
        try {
            byte[] trimmedBytes = new byte[lastValid-startingPosition];
            System.arraycopy(bytes, startingPosition, trimmedBytes, 0, trimmedBytes.length);
            PhosphorHttpRequest request = requestFromBytes(trimmedBytes);
            String requestString = request.toString();
            byte[] processedBytes = requestString.getBytes();
            // Ensure that the size of the byte array returned is at least as long as the specified input array and that
            // the content is offset by starting position
            lastValidUpdate[0] = startingPosition + processedBytes.length;
            return positionBytes(bytes, processedBytes, startingPosition, size);
        } catch(Exception e) {
            e.printStackTrace();
            lastValidUpdate[0] = lastValid;
            return copy;
        }
    }

    private static byte[] positionBytes(byte[] oldBytes, byte[] newBytes, int startingPosition, int minSize) {
        byte[] result = new byte[Math.max(startingPosition + newBytes.length, minSize)];
        System.arraycopy(newBytes, 0, result, startingPosition, newBytes.length);
        System.arraycopy(oldBytes, 0, result, 0, startingPosition);
        return result;
    }

    /* Parses the specified bytes into a PhosphorHttpRequest. Returns the parsed request. */
    private static PhosphorHttpRequest requestFromBytes(byte[] bytes) throws Exception {
        SessionInputBufferImpl sessionBuffer = new SessionInputBufferImpl(new HttpTransportMetricsImpl(), bytes.length);
        sessionBuffer.bind(new ByteArrayInputStream(bytes));
        DefaultHttpRequestParser parser = new DefaultHttpRequestParser(sessionBuffer);
        HttpRequest request = parser.parse();
        // Read remaining entity body content from the buffer
        long contentLength = StrictContentLengthStrategy.INSTANCE.determineLength(request);
        InputStream contentStream;
        if(contentLength == ContentLengthStrategy.CHUNKED) {
            contentStream = new ChunkedInputStream(sessionBuffer);
            contentLength = -1;
        } else if(contentLength == ContentLengthStrategy.IDENTITY) {
            contentStream = new IdentityInputStream(sessionBuffer);
            contentLength = -1;
        } else {
            contentStream = new ContentLengthInputStream(sessionBuffer, contentLength);
        }
        ContentType contentType;
        try {
          contentType = ContentType.parse(request.getFirstHeader(HTTP.CONTENT_TYPE).getValue());
        } catch(Exception e) {
            contentType = null;
        }
        HttpEntity entity = new InputStreamEntity(contentStream, contentLength, contentType);
        return new PhosphorHttpRequest(request, entity);
    }
}
