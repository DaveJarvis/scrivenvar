/*
 * Copyright (c) 2016 Karl Tauber <karl at jformdesigner dot com>
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 *  * Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 *
 *  * Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in the
 *    documentation and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
 * A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
 * HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
 * DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY
 * THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package com.scrivendor;

import static com.scrivendor.Constants.BUNDLE_NAME;
import java.text.MessageFormat;
import java.util.ResourceBundle;
import java.util.Stack;

/**
 * Recursively resolves message properties. Property values can refer to other
 * properties using a <code>${var}</code> syntax.
 *
 * @author Karl Tauber, Dave Jarvis
 */
public class Messages {

  private static final ResourceBundle RESOURCE_BUNDLE = ResourceBundle.getBundle( BUNDLE_NAME );

  private Messages() {
  }

  /**
   * Return the value of a resource bundle value after having resolved any
   * references to other bundle variables.
   *
   * @param props The bundle containing resolvable properties.
   * @param s The value for a key to resolve.
   *
   * @return The value of the key with all references recursively dereferenced.
   */
  private static String resolve( ResourceBundle props, String s ) {
    final int len = s.length();
    final Stack<StringBuilder> stack = new Stack<>();

    StringBuilder sb = new StringBuilder( 256 );
    boolean open = false;

    for( int i = 0; i < len; i++ ) {
      final char c = s.charAt( i );

      switch( c ) {
        case '$': {
          if( i + 1 < len && s.charAt( i + 1 ) == '{' ) {
            stack.push( sb );
            sb = new StringBuilder( 256 );
            i++;
            open = true;
          }

          break;
        }

        case '}': {
          if( open ) {
            open = false;
            final String name = sb.toString();

            sb = stack.pop();
            sb.append( props.getString( name ) );
            break;
          }
        }

        default: {
          sb.append( c );
          break;
        }
      }
    }

    if( open ) {
      throw new IllegalArgumentException( "missing '}'" );
    }

    return sb.toString();
  }

  /**
   * Returns the value for a key from the message bundle.
   *
   * @param key Retrieve the value for this key.
   *
   * @return The value for the key.
   */
  public static String get( String key ) {
    return resolve( RESOURCE_BUNDLE, RESOURCE_BUNDLE.getString( key ) );
  }

  /**
   * Returns the value for a key from the message bundle with the arguments
   * replacing <code>{#}</code> place holders.
   *
   * @param key Retrieve the value for this key.
   * @param args The values to substitute for place holders.
   *
   * @return The value for the key.
   */
  public static String get( String key, Object... args ) {
    return MessageFormat.format( get( key ), args );
  }
}
