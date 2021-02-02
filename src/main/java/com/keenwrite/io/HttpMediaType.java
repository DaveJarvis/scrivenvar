/* Copyright 2020-2021 White Magic Software, Ltd. -- All rights reserved. */
package com.keenwrite.io;

import javax.net.ssl.*;
import java.net.MalformedURLException;
import java.net.Socket;
import java.net.URI;
import java.net.URL;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.security.cert.X509Certificate;

import static com.keenwrite.events.StatusEvent.clue;
import static com.keenwrite.io.MediaType.UNDEFINED;
import static java.net.http.HttpClient.Redirect.NORMAL;
import static java.net.http.HttpRequest.BodyPublishers.noBody;
import static java.net.http.HttpResponse.BodyHandlers.ofString;
import static java.time.Duration.ofSeconds;

/**
 * Responsible for determining {@link MediaType} based on the content-type from
 * an HTTP request.
 */
public final class HttpMediaType {

  static {
    disableSSLVerification();
  }

  private static final HttpClient HTTP_CLIENT = HttpClient
    .newBuilder()
    .connectTimeout( ofSeconds( 10 ) )
    .followRedirects( NORMAL )
    .build();

  /**
   * Performs an HTTP HEAD request to determine the media type based on the
   * Content-Type header returned from the server.
   *
   * @param uri Determine the media type for this resource.
   * @return The data type for the resource or {@link MediaType#UNDEFINED} if
   * unmapped.
   * @throws MalformedURLException The {@link URI} could not be converted to
   *                               an instance of {@link URL}.
   */
  public static MediaType valueFrom( final URI uri )
    throws MalformedURLException {
    final var mediaType = new MediaType[]{UNDEFINED};

    try {
      clue( "Main.status.image.request.init" );
      final var request = HttpRequest
        .newBuilder()
        .method( "HEAD", noBody() )
        .uri( uri )
        .build();
      clue( "Main.status.image.request.fetch", uri.getHost() );
      final var response = HTTP_CLIENT.send( request, ofString() );
      final var headers = response.headers();
      final var map = headers.map();

      map.forEach( ( key, values ) -> {
        if( "content-type".equalsIgnoreCase( key ) ) {
          var header = values.get( 0 );
          // Trim off the character encoding.
          var i = header.indexOf( ';' );
          header = header.substring( 0, i == -1 ? header.length() : i );

          // Split the type and subtype.
          i = header.indexOf( '/' );
          i = i == -1 ? header.length() : i;
          final var type = header.substring( 0, i );
          final var subtype = header.substring( i + 1 );

          mediaType[ 0 ] = MediaType.valueFrom( type, subtype );
          clue( "Main.status.image.request.success", mediaType[ 0 ] );
        }
      } );

      clue();
    } catch( final Exception ex ) {
      clue( ex );
    }

    return mediaType[ 0 ];
  }

  // Method used for bypassing SSL verification
  private static void disableSSLVerification() {

    TrustManager[] trustAllCerts =
      new TrustManager[]{new X509ExtendedTrustManager() {
        @Override
        public void checkClientTrusted( X509Certificate[] chain,
                                        String authType,
                                        Socket socket ) {

        }

        @Override
        public void checkServerTrusted( X509Certificate[] chain,
                                        String authType,
                                        Socket socket ) {

        }

        @Override
        public void checkClientTrusted( X509Certificate[] chain,
                                        String authType,
                                        SSLEngine engine ) {

        }

        @Override
        public void checkServerTrusted( X509Certificate[] chain,
                                        String authType,
                                        SSLEngine engine ) {

        }

        @Override
        public java.security.cert.X509Certificate[] getAcceptedIssuers() {
          return null;
        }

        @Override
        public void checkClientTrusted(
          X509Certificate[] certs, String authType ) {
        }

        @Override
        public void checkServerTrusted(
          X509Certificate[] certs, String authType ) {
        }
      }};

    try {
      final var context = SSLContext.getInstance( "SSL" );
      context.init( null, trustAllCerts, new java.security.SecureRandom() );
      HttpsURLConnection.setDefaultSSLSocketFactory( context.getSocketFactory() );
      HttpsURLConnection.setDefaultHostnameVerifier( ( hostname, session ) -> true );
    } catch( final Exception ex ) {
      clue( ex );
    }
  }
}
