package edu.columbia.cs.psl.struct;

import edu.columbia.cs.psl.phosphor.instrumenter.RestructureRequestBytesCV;
import org.apache.http.*;
import org.apache.http.entity.*;
import org.apache.http.impl.entity.StrictContentLengthStrategy;
import org.apache.http.impl.io.*;
import org.apache.http.protocol.HTTP;
import org.apache.http.util.EntityUtils;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.Serializable;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.ByteBuffer;
import java.nio.channels.ByteChannel;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

/* Represents an HTTP request. Capable of converting the request it represents into a byte array using fine-grain public
 * accessor methods which make excellent auto-taint source methods. */
public class PhosphorHttpRequest implements Serializable {

    private static final long serialVersionUID = -2622573790320306202L;
    // Carriage return and line feed characters used to indicate the end of a line.
    private static final String CRLF = "\r\n";
    // The string name of the cookie header
    private static final String COOKIE_HEADER = "Cookie";
    // List of common HTTP header names
    private static final List<String> COMMON_HEADERS = Arrays.asList(COOKIE_HEADER, HttpHeaders.ACCEPT,
            HttpHeaders.ACCEPT_CHARSET, HttpHeaders.ACCEPT_ENCODING, HttpHeaders.ACCEPT_LANGUAGE, HttpHeaders.ACCEPT_RANGES,
            HttpHeaders.AGE, HttpHeaders.ALLOW, HttpHeaders.AUTHORIZATION, HttpHeaders.CACHE_CONTROL, HttpHeaders.CONNECTION,
            HttpHeaders.CONTENT_ENCODING, HttpHeaders.CONTENT_LANGUAGE, HttpHeaders.CONTENT_LOCATION,
            HttpHeaders.CONTENT_MD5, HttpHeaders.CONTENT_RANGE, HttpHeaders.CONTENT_TYPE, HttpHeaders.DATE, HttpHeaders.DAV,
            HttpHeaders.DEPTH, HttpHeaders.DESTINATION, HttpHeaders.ETAG, HttpHeaders.EXPECT, HttpHeaders.EXPIRES,
            HttpHeaders.FROM, HttpHeaders.HOST, HttpHeaders.IF, HttpHeaders.IF_MATCH, HttpHeaders.IF_MODIFIED_SINCE,
            HttpHeaders.IF_NONE_MATCH, HttpHeaders.IF_RANGE, HttpHeaders.IF_UNMODIFIED_SINCE, HttpHeaders.LAST_MODIFIED,
            HttpHeaders.LOCATION, HttpHeaders.LOCK_TOKEN, HttpHeaders.MAX_FORWARDS,
            HttpHeaders.OVERWRITE, HttpHeaders.PRAGMA, HttpHeaders.PROXY_AUTHENTICATE, HttpHeaders.PROXY_AUTHORIZATION,
            HttpHeaders.RANGE, HttpHeaders.REFERER, HttpHeaders.RETRY_AFTER, HttpHeaders.SERVER, HttpHeaders.STATUS_URI,
            HttpHeaders.TE, HttpHeaders.TIMEOUT, HttpHeaders.TRAILER, HttpHeaders.TRANSFER_ENCODING,
            HttpHeaders.UPGRADE, HttpHeaders.USER_AGENT, HttpHeaders.VARY, HttpHeaders.VIA, HttpHeaders.WARNING, HttpHeaders.WWW_AUTHENTICATE);

    // Part of the request line, indicates the method to be performed
    private final String method;
    // Part of the request line, indicates the resource the request applies to
    private final URI uri;
    // Part of the request line, indicates the version of HTTP used
    private final String protocolVersion;
    // Maps commonly used HTTP message header names to their values for this request
    private final HashMap<String, String> commonHeaders;
    // Maps non-common HTTP message header names to their values for this request
    private final HashMap<String, String> uncommonHeaders;
    // The String entity body of the request or null if no entity body is present
    private final String entityBody;

    /* Constructs a new PhosphorHttpRequest containing information from the specified HttpRequest. */
    public PhosphorHttpRequest(HttpRequest request, HttpEntity entity) throws URISyntaxException, IOException {
        this.method = request.getRequestLine().getMethod();
        this.uri = new URI(request.getRequestLine().getUri());
        this.protocolVersion = request.getProtocolVersion().toString();
        this.commonHeaders = new HashMap<>();
        this.uncommonHeaders = new HashMap<>();
        for(Header header : request.getAllHeaders()) {
            addHeader(header);
        }
        if(entity != null) {
            this.entityBody = EntityUtils.toString(entity);
            EntityUtils.consume(entity);
        } else {
            this.entityBody = null;
        }
    }

    /* Adds the specified header to either the list of common header or the list of other header. */
    private void addHeader(Header header) {
        String name = header.getName();
        String value = header.getValue();
        if(name.equalsIgnoreCase(COOKIE_HEADER)) {
            commonHeaders.put(COOKIE_HEADER, value);
        } else if(!header.getName().equalsIgnoreCase(HttpHeaders.CONTENT_LENGTH)) {
            // Skip the content length header, it will be recalculated from the entity body
            for(String commonHeader : COMMON_HEADERS) {
                if(header.getName().equalsIgnoreCase(commonHeader)) {
                    commonHeaders.put(commonHeader, value);
                    return;
                }
            }
            // Header name did not match any of the common headers
            uncommonHeaders.put(name, value);
        }
    }

    /* Returns the HTTP method specifying the action to be performed by this request. */
    public String getMethod() {
        return method;
    }

    /* Returns this request's URI's scheme or null if undefined. */
    public String getScheme() {
        return uri.getScheme();
    }

    /* Returns this request's URI's fragment component or null if undefined. */
    public String getFragment() {
        return uri.getFragment();
    }

    /* Returns this request's URI's authority component or null if undefined. */
    public String getAuthority() {
        return uri.getAuthority();
    }

    /* Returns this request's URI's user-information or null if undefined. */
    public String getUserInfo() {
        return uri.getUserInfo();
    }

    /* Returns this request's URI's host or null if undefined. */
    public String getHost() {
        return uri.getHost();
    }

    /* Returns this request's URI's port or -1 if undefined. */
    public int getPort() {
        return uri.getPort();
    }

    /* Returns this request's URI's path or null if undefined. */
    public String getPath() {
        return uri.getPath();
    }

    /* Returns this request's URI's query or null if undefined. */
    public String getEncodedQuery() {
        return uri.getRawQuery();
    }

    /* Returns a string indicating the version of HTTP used for this request. */
    public String getProtocolVersion() {
        return protocolVersion;
    }

    /* Returns the map of uncommon headers for this request. */
    public HashMap<String, String> getUncommonHeaders() {
        return uncommonHeaders;
    }

    /* Returns the value of the accept header or null if undefined. */
    public String getAcceptHeader() {
        return commonHeaders.get(HttpHeaders.ACCEPT);
    }

    /* Returns the value of the accept-charset header or null if undefined. */
    public String getAcceptCharsetHeader() {
        return commonHeaders.get(HttpHeaders.ACCEPT_CHARSET);
    }

    /* Returns the value of the accept-encoding header or null if undefined. */
    public String getAcceptEncodingHeader() {
        return commonHeaders.get(HttpHeaders.ACCEPT_ENCODING);
    }

    /* Returns the value of the accept-language header or null if undefined. */
    public String getAcceptLanguageHeader() {
        return commonHeaders.get(HttpHeaders.ACCEPT_LANGUAGE);
    }

    /* Returns the value of the accept-ranges header or null if undefined. */
    public String getAcceptRangesHeader() {
        return commonHeaders.get(HttpHeaders.ACCEPT_RANGES);
    }

    /* Returns the value of the age header or null if undefined. */
    public String getAgeHeader() {
        return commonHeaders.get(HttpHeaders.AGE);
    }

    /* Returns the value of the allow header or null if undefined. */
    public String getAllowHeader() {
        return commonHeaders.get(HttpHeaders.ALLOW);
    }

    /* Returns the value of the authorization header or null if undefined. */
    public String getAuthorizationHeader() {
        return commonHeaders.get(HttpHeaders.AUTHORIZATION);
    }

    /* Returns the value of the cache-control header or null if undefined. */
    public String getCacheControlHeader() {
        return commonHeaders.get(HttpHeaders.CACHE_CONTROL);
    }

    /* Returns the value of the connection header or null if undefined. */
    public String getConnectionHeader() {
        return commonHeaders.get(HttpHeaders.CONNECTION);
    }

    /* Returns the value of the content-encoding header or null if undefined. */
    public String getContentEncodingHeader() {
        return commonHeaders.get(HttpHeaders.CONTENT_ENCODING);
    }

    /* Returns the value of the content-language header or null if undefined. */
    public String getContentLanguageHeader() {
        return commonHeaders.get(HttpHeaders.CONTENT_LANGUAGE);
    }

    /* Returns the value of the content-location header or null if undefined. */
    public String getContentLocationHeader() {
        return commonHeaders.get(HttpHeaders.CONTENT_LOCATION);
    }

    /* Returns the value of the content-md5 header or null if undefined. */
    public String getContentMd5Header() {
        return commonHeaders.get(HttpHeaders.CONTENT_MD5);
    }

    /* Returns the value of the content-range header or null if undefined. */
    public String getContentRangeHeader() {
        return commonHeaders.get(HttpHeaders.CONTENT_RANGE);
    }

    /* Returns the value of the content-type header or null if undefined. */
    public String getContentTypeHeader() {
        return commonHeaders.get(HttpHeaders.CONTENT_TYPE);
    }

    /* Returns the value of the cookie header or null if undefined. */
    public String getCookieHeader() {
        return commonHeaders.get(COOKIE_HEADER);
    }

    /* Returns the value of the date header or null if undefined. */
    public String getDateHeader() {
        return commonHeaders.get(HttpHeaders.DATE);
    }

    /* Returns the value of the dav header or null if undefined. */
    public String getDavHeader() {
        return commonHeaders.get(HttpHeaders.DAV);
    }

    /* Returns the value of the depth header or null if undefined. */
    public String getDepthHeader() {
        return commonHeaders.get(HttpHeaders.DEPTH);
    }

    /* Returns the value of the destination header or null if undefined. */
    public String getDestinationHeader() {
        return commonHeaders.get(HttpHeaders.DESTINATION);
    }

    /* Returns the value of the ETag header or null if undefined. */
    public String getETagHeader() {
        return commonHeaders.get(HttpHeaders.ETAG);
    }

    /* Returns the value of the expect header or null if undefined. */
    public String getExpectHeader() {
        return commonHeaders.get(HttpHeaders.EXPECT);
    }

    /* Returns the value of the expires header or null if undefined. */
    public String getExpiresHeader() {
        return commonHeaders.get(HttpHeaders.EXPIRES);
    }

    /* Returns the value of the from header or null if undefined. */
    public String getFromHeader() {
        return commonHeaders.get(HttpHeaders.FROM);
    }

    /* Returns the value of the host header or null if undefined. */
    public String getHostHeader() {
        return commonHeaders.get(HttpHeaders.HOST);
    }

    /* Returns the value of the if header or null if undefined. */
    public String getIfHeader() {
        return commonHeaders.get(HttpHeaders.IF);
    }

    /* Returns the value of the if-match header or null if undefined. */
    public String getIfMatchHeader() {
        return commonHeaders.get(HttpHeaders.IF_MATCH);
    }

    /* Returns the value of the if-modified-since header or null if undefined. */
    public String getIfModifiedSinceHeader() {
        return commonHeaders.get(HttpHeaders.IF_MODIFIED_SINCE);
    }

    /* Returns the value of the if-none-match header or null if undefined. */
    public String getIfNoneMatchHeader() {
        return commonHeaders.get(HttpHeaders.IF_NONE_MATCH);
    }

    /* Returns the value of the if-range header or null if undefined. */
    public String getIfRangeHeader() {
        return commonHeaders.get(HttpHeaders.IF_RANGE);
    }

    /* Returns the value of the if-unmodified-since header or null if undefined. */
    public String getIfUnmodifiedSinceHeader() {
        return commonHeaders.get(HttpHeaders.IF_UNMODIFIED_SINCE);
    }

    /* Returns the value of the last-modified header or null if undefined. */
    public String getLastModifiedHeader() {
        return commonHeaders.get(HttpHeaders.LAST_MODIFIED);
    }

    /* Returns the value of the location header or null if undefined. */
    public String getLocationHeader() {
        return commonHeaders.get(HttpHeaders.LOCATION);
    }

    /* Returns the value of the lock-token header or null if undefined. */
    public String getLockTokenHeader() {
        return commonHeaders.get(HttpHeaders.LOCK_TOKEN);
    }

    /* Returns the value of the max-forwards header or null if undefined. */
    public String getMaxForwardsHeader() {
        return commonHeaders.get(HttpHeaders.MAX_FORWARDS);
    }

    /* Returns the value of the overwrite header or null if undefined. */
    public String getOverwriteHeader() {
        return commonHeaders.get(HttpHeaders.OVERWRITE);
    }

    /* Returns the value of the pragma header or null if undefined. */
    public String getPragmaHeader() {
        return commonHeaders.get(HttpHeaders.PRAGMA);
    }

    /* Returns the value of the proxy-authenticate header or null if undefined. */
    public String getProxyAuthenticateHeader() {
        return commonHeaders.get(HttpHeaders.PROXY_AUTHENTICATE);
    }

    /* Returns the value of the proxy-authorization header or null if undefined. */
    public String getProxyAuthorizationHeader() {
        return commonHeaders.get(HttpHeaders.PROXY_AUTHORIZATION);
    }

    /* Returns the value of the range header or null if undefined. */
    public String getRangeHeader() {
        return commonHeaders.get(HttpHeaders.RANGE);
    }

    /* Returns the value of the referer header or null if undefined. */
    public String getRefererHeader() {
        return commonHeaders.get(HttpHeaders.REFERER);
    }

    /* Returns the value of the retry-after header or null if undefined. */
    public String getRetryAfterHeader() {
        return commonHeaders.get(HttpHeaders.RETRY_AFTER);
    }

    /* Returns the value of the server header or null if undefined. */
    public String getServerHeader() {
        return commonHeaders.get(HttpHeaders.SERVER);
    }

    /* Returns the value of the status-uri header or null if undefined. */
    public String getStatusUriHeader() {
        return commonHeaders.get(HttpHeaders.STATUS_URI);
    }

    /* Returns the value of the te header or null if undefined. */
    public String getTeHeader() {
        return commonHeaders.get(HttpHeaders.TE);
    }

    /* Returns the value of the timeout header or null if undefined. */
    public String getTimeoutHeader() {
        return commonHeaders.get(HttpHeaders.TIMEOUT);
    }

    /* Returns the value of the trailer header or null if undefined. */
    public String getTrailerHeader() {
        return commonHeaders.get(HttpHeaders.TRAILER);
    }

    /* Returns the value of the transfer-encoding header or null if undefined. */
    public String getTransferEncodingHeader() {
        return commonHeaders.get(HttpHeaders.TRANSFER_ENCODING);
    }

    /* Returns the value of the upgrade header or null if undefined. */
    public String getUpgradeHeader() {
        return commonHeaders.get(HttpHeaders.UPGRADE);
    }

    /* Returns the value of the user-agent header or null if undefined. */
    public String getUserAgentHeader() {
        return commonHeaders.get(HttpHeaders.USER_AGENT);
    }

    /* Returns the value of the vary header or null if undefined. */
    public String getVaryHeader() {
        return commonHeaders.get(HttpHeaders.VARY);
    }

    /* Returns the value of the via header or null if undefined. */
    public String getViaHeader() {
        return commonHeaders.get(HttpHeaders.VIA);
    }

    /* Returns the value of the warning header or null if undefined. */
    public String getWarningHeader() {
        return commonHeaders.get(HttpHeaders.WARNING);
    }

    /* Returns the value of the www-authenticate header or null if undefined. */
    public String getWWWAuthenticateHeader() {
        return commonHeaders.get(HttpHeaders.WWW_AUTHENTICATE);
    }

    /* Returns the string value of this request's entity body or null if no entity body is present. */
    public String getEntityBody() {
        return entityBody;
    }

    /* Adds this request's URI information to the specified StringBuilder. */
    private void addURIInfo(StringBuilder builder) throws Exception {
        String scheme = getScheme();
        String userInfo = getUserInfo();
        String host = getHost();
        String fragment = getFragment();
        int port = getPort();
        String authority = getAuthority();
        String path = getPath();
        URI reconstructedURI = (host == null) ? new URI(scheme, authority, path, null, fragment) : new URI(scheme, userInfo, host, port, path, null, fragment);
        addEncodedQueryString(reconstructedURI, getEncodedQuery());
        builder.append(reconstructedURI.toASCIIString());
    }

    /* Adds an encoded query string to the specified URI. */
    private static void addEncodedQueryString(URI uri, String queryString) throws IllegalAccessException {
        Field queryField = getField(uri, "query", String.class);
        Field stringField = getField(uri, "string", String.class);
        if(queryField == null || stringField == null) {
            throw new RuntimeException("Could not find field for URI instance");
        } else {
            queryField.set(uri, queryString);
            // Clear cached string
            stringField.set(uri, null);
        }
    }

    /* Adds this request's header information to the specified StringBuilder. */
    private void addHeaderInfo(StringBuilder builder) {
        // Add common headers
        if(getCookieHeader() != null) {
            builder.append(COOKIE_HEADER).append(": ").append(getCookieHeader()).append(CRLF);
        }
        if(getAcceptHeader() != null) {
            builder.append(HttpHeaders.ACCEPT).append(": ").append(getAcceptHeader()).append(CRLF);
        }
        if(getAcceptCharsetHeader() != null) {
            builder.append(HttpHeaders.ACCEPT_CHARSET).append(": ").append(getAcceptCharsetHeader()).append(CRLF);
        }
        if(getAcceptEncodingHeader() != null) {
            builder.append(HttpHeaders.ACCEPT_ENCODING).append(": ").append(getAcceptEncodingHeader()).append(CRLF);
        }
        if(getAcceptLanguageHeader() != null) {
            builder.append(HttpHeaders.ACCEPT_LANGUAGE).append(": ").append(getAcceptLanguageHeader()).append(CRLF);
        }
        if(getAcceptRangesHeader() != null) {
            builder.append(HttpHeaders.ACCEPT_RANGES).append(": ").append(getAcceptRangesHeader()).append(CRLF);
        }
        if(getAgeHeader() != null) {
            builder.append(HttpHeaders.AGE).append(": ").append(getAgeHeader()).append(CRLF);
        }
        if(getAllowHeader() != null) {
            builder.append(HttpHeaders.ALLOW).append(": ").append(getAllowHeader()).append(CRLF);
        }
        if(getAuthorizationHeader() != null) {
            builder.append(HttpHeaders.AUTHORIZATION).append(": ").append(getAuthorizationHeader()).append(CRLF);
        }
        if(getCacheControlHeader() != null) {
            builder.append(HttpHeaders.CACHE_CONTROL).append(": ").append(getCacheControlHeader()).append(CRLF);
        }
        if(getConnectionHeader() != null) {
            builder.append(HttpHeaders.CONNECTION).append(": ").append(getConnectionHeader()).append(CRLF);
        }
        if(getContentEncodingHeader() != null) {
            builder.append(HttpHeaders.CONTENT_ENCODING).append(": ").append(getContentEncodingHeader()).append(CRLF);
        }
        if(getContentLanguageHeader() != null) {
            builder.append(HttpHeaders.CONTENT_LANGUAGE).append(": ").append(getContentLanguageHeader()).append(CRLF);
        }
        if(getContentLocationHeader() != null) {
            builder.append(HttpHeaders.CONTENT_LOCATION).append(": ").append(getContentLocationHeader()).append(CRLF);
        }
        if(getContentMd5Header() != null) {
            builder.append(HttpHeaders.CONTENT_MD5).append(": ").append(getContentMd5Header()).append(CRLF);
        }
        if(getContentRangeHeader() != null) {
            builder.append(HttpHeaders.CONTENT_RANGE).append(": ").append(getContentRangeHeader()).append(CRLF);
        }
        if(getContentTypeHeader() != null) {
            builder.append(HttpHeaders.CONTENT_TYPE).append(": ").append(getContentTypeHeader()).append(CRLF);
        }
        if(getDateHeader() != null) {
            builder.append(HttpHeaders.DATE).append(": ").append(getDateHeader()).append(CRLF);
        }
        if(getDavHeader() != null) {
            builder.append(HttpHeaders.DAV).append(": ").append(getDavHeader()).append(CRLF);
        }
        if(getDepthHeader() != null) {
            builder.append(HttpHeaders.DEPTH).append(": ").append(getDepthHeader()).append(CRLF);
        }
        if(getDestinationHeader() != null) {
            builder.append(HttpHeaders.DESTINATION).append(": ").append(getDestinationHeader()).append(CRLF);
        }
        if(getETagHeader() != null) {
            builder.append(HttpHeaders.ETAG).append(": ").append(getETagHeader()).append(CRLF);
        }
        if(getExpectHeader() != null) {
            builder.append(HttpHeaders.EXPECT).append(": ").append(getExpectHeader()).append(CRLF);
        }
        if(getExpiresHeader() != null) {
            builder.append(HttpHeaders.EXPIRES).append(": ").append(getExpiresHeader()).append(CRLF);
        }
        if(getFromHeader() != null) {
            builder.append(HttpHeaders.FROM).append(": ").append(getFromHeader()).append(CRLF);
        }
        if(getHostHeader() != null) {
            builder.append(HttpHeaders.HOST).append(": ").append(getHostHeader()).append(CRLF);
        }
        if(getIfHeader() != null) {
            builder.append(HttpHeaders.IF).append(": ").append(getIfHeader()).append(CRLF);
        }
        if(getIfMatchHeader() != null) {
            builder.append(HttpHeaders.IF_MATCH).append(": ").append(getIfMatchHeader()).append(CRLF);
        }
        if(getIfModifiedSinceHeader() != null) {
            builder.append(HttpHeaders.IF_MODIFIED_SINCE).append(": ").append(getIfModifiedSinceHeader()).append(CRLF);
        }
        if(getIfNoneMatchHeader() != null) {
            builder.append(HttpHeaders.IF_NONE_MATCH).append(": ").append(getIfNoneMatchHeader()).append(CRLF);
        }
        if(getIfRangeHeader() != null) {
            builder.append(HttpHeaders.IF_RANGE).append(": ").append(getIfRangeHeader()).append(CRLF);
        }
        if(getIfUnmodifiedSinceHeader() != null) {
            builder.append(HttpHeaders.IF_UNMODIFIED_SINCE).append(": ").append(getIfUnmodifiedSinceHeader()).append(CRLF);
        }
        if(getLastModifiedHeader() != null) {
            builder.append(HttpHeaders.LAST_MODIFIED).append(": ").append(getLastModifiedHeader()).append(CRLF);
        }
        if(getLocationHeader() != null) {
            builder.append(HttpHeaders.LOCATION).append(": ").append(getLocationHeader()).append(CRLF);
        }
        if(getLockTokenHeader() != null) {
            builder.append(HttpHeaders.LOCK_TOKEN).append(": ").append(getLockTokenHeader()).append(CRLF);
        }
        if(getMaxForwardsHeader() != null) {
            builder.append(HttpHeaders.MAX_FORWARDS).append(": ").append(getMaxForwardsHeader()).append(CRLF);
        }
        if(getOverwriteHeader() != null) {
            builder.append(HttpHeaders.OVERWRITE).append(": ").append(getOverwriteHeader()).append(CRLF);
        }
        if(getPragmaHeader() != null) {
            builder.append(HttpHeaders.PRAGMA).append(": ").append(getPragmaHeader()).append(CRLF);
        }
        if(getProxyAuthenticateHeader() != null) {
            builder.append(HttpHeaders.PROXY_AUTHENTICATE).append(": ").append(getProxyAuthenticateHeader()).append(CRLF);
        }
        if(getProxyAuthorizationHeader() != null) {
            builder.append(HttpHeaders.PROXY_AUTHORIZATION).append(": ").append(getProxyAuthorizationHeader()).append(CRLF);
        }
        if(getRangeHeader() != null) {
            builder.append(HttpHeaders.RANGE).append(": ").append(getRangeHeader()).append(CRLF);
        }
        if(getRefererHeader() != null) {
            builder.append(HttpHeaders.REFERER).append(": ").append(getRefererHeader()).append(CRLF);
        }
        if(getRetryAfterHeader() != null) {
            builder.append(HttpHeaders.RETRY_AFTER).append(": ").append(getRetryAfterHeader()).append(CRLF);
        }
        if(getServerHeader() != null) {
            builder.append(HttpHeaders.SERVER).append(": ").append(getServerHeader()).append(CRLF);
        }
        if(getStatusUriHeader() != null) {
            builder.append(HttpHeaders.STATUS_URI).append(": ").append(getStatusUriHeader()).append(CRLF);
        }
        if(getTeHeader() != null) {
            builder.append(HttpHeaders.TE).append(": ").append(getTeHeader()).append(CRLF);
        }
        if(getTimeoutHeader() != null) {
            builder.append(HttpHeaders.TIMEOUT).append(": ").append(getTimeoutHeader()).append(CRLF);
        }
        if(getTrailerHeader() != null) {
            builder.append(HttpHeaders.TRAILER).append(": ").append(getTrailerHeader()).append(CRLF);
        }
        if(getTransferEncodingHeader() != null) {
            builder.append(HttpHeaders.TRANSFER_ENCODING).append(": ").append(getTransferEncodingHeader()).append(CRLF);
        }
        if(getUpgradeHeader() != null) {
            builder.append(HttpHeaders.UPGRADE).append(": ").append(getUpgradeHeader()).append(CRLF);
        }
        if(getUserAgentHeader() != null) {
            builder.append(HttpHeaders.USER_AGENT).append(": ").append(getUserAgentHeader()).append(CRLF);
        }
        if(getVaryHeader() != null) {
            builder.append(HttpHeaders.VARY).append(": ").append(getVaryHeader()).append(CRLF);
        }
        if(getViaHeader() != null) {
            builder.append(HttpHeaders.VIA).append(": ").append(getViaHeader()).append(CRLF);
        }
        if(getWarningHeader() != null) {
            builder.append(HttpHeaders.WARNING).append(": ").append(getWarningHeader()).append(CRLF);
        }
        if(getWWWAuthenticateHeader() != null) {
            builder.append(HttpHeaders.WWW_AUTHENTICATE).append(": ").append(getWWWAuthenticateHeader()).append(CRLF);
        }
        // Add the uncommon headers
        HashMap<String, String> uncommonHeaders = getUncommonHeaders();
        for(String name : uncommonHeaders.keySet()) {
            if(uncommonHeaders.get(name) != null) {
                builder.append(name).append(": ").append(uncommonHeaders.get(name)).append(CRLF);
            }
        }
    }

    /* Adds this request's content-length header and entity body to the specified StringBuilder. */
    private void addEntityInfo(StringBuilder builder) {
        String body = getEntityBody();
        if(body != null) {
            builder.append(HttpHeaders.CONTENT_LENGTH).append(": ").append(body.length()).append(CRLF);
            builder.append(CRLF).append(body);
        } else {
            builder.append(HttpHeaders.CONTENT_LENGTH).append(": ").append("0").append(CRLF).append(CRLF);
        }
    }

    /* Returns a text representation of this request. */
    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append(getMethod()).append(' ');
        try {
            addURIInfo(builder);
        } catch (Exception e) {
             builder.append(uri.toASCIIString());
        }
        builder.append(' ').append(getProtocolVersion()).append(CRLF);
        addHeaderInfo(builder);
        addEntityInfo(builder);
        return builder.toString();
    }

    /* Reads bytes from the specified object's socket, structures the read bytes into a PhosphorHttpRequest, converts that
     * request back into bytes, and return a ByteBuffer wrapping those bytes. */
    public static ByteBuffer structureRequestBytes(Object obj) {
        try {
            String className = obj.getClass().getName().replace(".", "/");
            if(className.equals(RestructureRequestBytesCV.TOMCAT_8_BUFFER_CLASS)) {
                Field socketField = getField(obj, "socket", ByteChannel.class);
                Field bufField = getField(obj, "buf", byte[].class);
                if(socketField == null || bufField == null) {
                    return null;
                }
                Object socket = socketField.get(obj);
                int bufLength = ((byte[])bufField.get(obj)).length;
                PhosphorHttpRequest request = requestFromSocket(socket, bufLength);
                return ByteBuffer.wrap(request.toString().getBytes());
            } else if(className.equals(RestructureRequestBytesCV.TOMCAT_9_BUFFER_CLASS)) {
                Field wrapperField = getField(obj, "wrapper", Object.class);
                Field bufField = getField(obj, "byteBuffer", ByteBuffer.class);
                if(wrapperField == null || bufField == null) {
                    return null;
                }
                Object wrapper = wrapperField.get(obj);
                int bufLength = ((ByteBuffer)bufField.get(obj)).capacity();
                PhosphorHttpRequest request = requestFromSocket(wrapper, bufLength);
                return ByteBuffer.wrap(request.toString().getBytes());
            } else {
                return null;
            }
        } catch(Exception e) {
            return null;
        }
    }

    /* Returns the field with the specified name and type for the specified object's class or null if the
     * field was not found. */
    private static Field getField(Object obj, String fieldName, Class<?> fieldClass) {
        for (Class<?> clazz = obj.getClass(); clazz != null; clazz = clazz.getSuperclass()) {
            // Iterate over superclasses to check for the field
            try {
                Field field = clazz.getDeclaredField(fieldName);
                if(field.getName().equals(fieldName) && fieldClass.isAssignableFrom(field.getType())) {
                    field.setAccessible(true);
                    return field;
                }
            } catch (Exception e) {
                //
            }
        }
        return null;
    }

    /* Reads bytes from the specified socket or socket wrapper into the specified ByteBuffer. */
    private static int readBytes(Object obj, ByteBuffer dest) throws Exception {
        if(obj instanceof ByteChannel) {
            return ((ByteChannel)obj).read(dest);
        } else {
            Method readMethod = obj.getClass().getMethod("read", Boolean.TYPE, ByteBuffer.class);
            return (int)readMethod.invoke(obj, false, dest);
        }
    }

    /* Reads bytes from the specified socket or socket wrapper. Returns a PhosphorHttpRequest parsed from those bytes. */
    private static PhosphorHttpRequest requestFromSocket(Object obj, int initialBufferLength) throws Exception {
        // Read initial bytes into a buffer
        byte[] bytes = new byte[initialBufferLength];
        ByteBuffer buf = ByteBuffer.wrap(bytes);
        readBytes(obj, buf);
        // Parse initial read's request line and headers
        SessionInputBufferImpl sessionBuffer = new SessionInputBufferImpl(new HttpTransportMetricsImpl(), bytes.length);
        sessionBuffer.bind(new ByteArrayInputStream(bytes));
        HttpRequest request = new DefaultHttpRequestParser(sessionBuffer).parse();
        // Read remaining entity body content from the buffer
        // Determine the content type
        ContentType contentType;
        try {
            contentType = ContentType.parse(request.getFirstHeader(HTTP.CONTENT_TYPE).getValue());
        } catch(Exception e) {
            contentType = null;
        }
        // Determine the content length
        long contentLength;
        try {
            contentLength = Long.parseLong(request.getFirstHeader(HTTP.CONTENT_LEN).getValue());
        } catch(Exception e) {
            contentLength = StrictContentLengthStrategy.INSTANCE.determineLength(request);
        }
        HttpEntity entity = parseEntity(obj, sessionBuffer, contentType, contentLength);
        return new PhosphorHttpRequest(request, entity);
    }

    /* Reads remaining entity body content from the specified buffer. Reads any additional bytes needed for the entity
     * from the specified object. Returns an entity constructed from that content or null if the buffer did not contain
     * entity body content. */
    private static HttpEntity parseEntity(Object obj, SessionInputBufferImpl sessionBuffer, ContentType contentType, long contentLength) throws Exception {
        HttpEntity entity = null;
        if(contentLength == ContentLengthStrategy.CHUNKED) {
            entity = new InputStreamEntity(new ChunkedInputStream(sessionBuffer), -1, contentType);
        } else if(contentLength == ContentLengthStrategy.IDENTITY) {
            entity = new InputStreamEntity(new IdentityInputStream(sessionBuffer), -1, contentType);
        } else if(contentLength > 0) {
            byte[] content = new byte[(int)contentLength];
            int offset = sessionBuffer.read(content);
            if(offset < contentLength) {
                // Read remaining content from the socket
                ByteBuffer buffer = ByteBuffer.wrap(content, offset, (int)contentLength - offset);
                readBytes(obj, buffer);
            }
            entity = new InputStreamEntity(new ByteArrayInputStream(content), contentLength, contentType);
        }
        return entity;
    }

    /* Reads from the specified object's phosphor-added buffer into the specified destination buffer. Reads additional bytes from the
     * specified object's socket if its phosphor-added buffer is exhausted. Returns the number of bytes read. */
    @SuppressWarnings("unused")
    public static int read(ByteBuffer dest, Object obj) {
        try {
            Field bufField = getField(obj, RestructureRequestBytesCV.BYTE_BUFF_FIELD_NAME, ByteBuffer.class);
            if(bufField == null) {
                return 0;
            }
            ByteBuffer source = (ByteBuffer)bufField.get(obj);
            if(source == null || !source.hasRemaining()) {
                source = structureRequestBytes(obj);
                bufField.set(obj, source);
            }
            if(source == null) {
                return 0;
            }
            int nRead = Math.min(source.remaining(), dest.remaining());
            if(nRead > 0) {
                dest.put(source.array(), source.arrayOffset() + source.position(), nRead);
                source.position(source.position() + nRead);
            }
            return nRead;
        } catch(Exception e) {
            return 0;
        }
    }
}
