package org.helioviewer.jhv.io;

import java.io.ByteArrayOutputStream;

//import io.netty.handler.codec.http.HttpHeaders;
import org.asynchttpclient.AsyncHandler;
import org.asynchttpclient.AsyncHandler.State;
import org.asynchttpclient.AsyncHttpClient;
import org.asynchttpclient.AsyncHttpClientConfig;
import org.asynchttpclient.DefaultAsyncHttpClient;
import org.asynchttpclient.DefaultAsyncHttpClientConfig;
import org.asynchttpclient.HttpResponseBodyPart;
import org.asynchttpclient.HttpResponseHeaders;
import org.asynchttpclient.HttpResponseStatus;
import org.asynchttpclient.ListenableFuture;

public class AsyncHTTP {

    private static final AsyncHttpClientConfig config = new DefaultAsyncHttpClientConfig.Builder().setFollowRedirect(true).build();
    private static final AsyncHttpClient client = new DefaultAsyncHttpClient(config);

    public static void get(String url) throws Exception {
        ListenableFuture<String> f = client.prepareGet(url).execute(new AsyncHandler<String>() {
            private final ByteArrayOutputStream stream = new ByteArrayOutputStream();

            @Override
            public State onStatusReceived(HttpResponseStatus status) throws Exception {
                int statusCode = status.getStatusCode();
                // The Status have been read
                // If you don't want to read the headers, body or stop processing the response
                if (statusCode >= 500) {
                    return State.ABORT;
                }
                return State.CONTINUE;
            }

            @Override
            public State onHeadersReceived(HttpResponseHeaders h) throws Exception {
                // HttpHeaders headers = h.getHeaders();
                // The headers have been read
                // If you don't want to read the body, or stop processing the response
                // return State.ABORT;
                return State.CONTINUE;
            }

            @Override
            public State onBodyPartReceived(HttpResponseBodyPart bodyPart) throws Exception {
                stream.write(bodyPart.getBodyPartBytes());
                return State.CONTINUE;
            }

            @Override
            public String onCompleted() throws Exception {
                // Will be invoked once the response has been fully read or a ResponseComplete exception
                // has been thrown.
                // NOTE: should probably use Content-Encoding from headers
                return stream.toString("UTF-8");
            }

            @Override
            public void onThrowable(Throwable t) {
            }
        });

        String bodyResponse = f.get();
        System.out.println(">>> " + bodyResponse);
    }

}
