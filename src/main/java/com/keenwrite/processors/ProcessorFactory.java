/*
 * Copyright 2020 White Magic Software, Ltd.
 *
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 *  o Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 *
 *  o Redistributions in binary form must reproduce the above copyright
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
package com.keenwrite.processors;

import com.keenwrite.AbstractFileFactory;
import com.keenwrite.preview.HTMLPreviewPane;
import com.keenwrite.processors.markdown.MarkdownProcessor;

import java.nio.file.Path;
import java.util.Map;

/**
 * Responsible for creating processors capable of parsing, transforming,
 * interpolating, and rendering known file types.
 */
public class ProcessorFactory extends AbstractFileFactory {

  private final ProcessorContext mProcessorContext;
  private final Processor<String> mMarkdownProcessor;

  /**
   * Constructs a factory with the ability to create processors that can perform
   * text and caret processing to generate a final preview.
   *
   * @param processorContext Parameters needed to construct various processors.
   */
  private ProcessorFactory( final ProcessorContext processorContext ) {
    mProcessorContext = processorContext;
    mMarkdownProcessor = createMarkdownProcessor();
  }

  /**
   * Creates a processor chain suitable for parsing and rendering the file
   * opened at the given tab.
   *
   * @param context The tab containing a text editor, path, and caret position.
   * @return A processor that can render the given tab's text.
   */
  public static Processor<String> createProcessors(
      final ProcessorContext context ) {
    final var factory = new ProcessorFactory( context );

    return switch( context.getFileType() ) {
      case RMARKDOWN -> factory.createRProcessor();
      case SOURCE -> factory.createMarkdownDefinitionProcessor();
      case XML -> factory.createXMLProcessor();
      case RXML -> factory.createRXMLProcessor();
      default -> factory.createIdentityProcessor();
    };
  }

  /*
  public Processor<String> createProcessors(
      final FileEditorTab tab, final OutputFormat format ) {
    var chain = createProcessors( tab );
    chain.remove( HtmlPreviewProcessor.class );

    if( format.isHtml() ) {
      switch( format ) {
        case HTML_SVG:
          break;
        case HTML_TEX:
          break;
      }
    }

    return chain;
  }
  */

  private Processor<String> createHTMLPreviewProcessor() {
    return new HtmlPreviewProcessor( getPreviewPane() );
  }

  /**
   * Creates and links the processors at the end of the processing chain.
   *
   * @return A markdown, caret replacement, and preview pane processor chain.
   */
  private Processor<String> createMarkdownProcessor() {
    final var hpp = createHTMLPreviewProcessor();
    return new MarkdownProcessor( hpp, getPreviewPane().getPath() );
  }

  protected Processor<String> createIdentityProcessor() {
    final var hpp = createHTMLPreviewProcessor();
    return new IdentityProcessor( hpp );
  }

  protected Processor<String> createDefinitionProcessor(
      final Processor<String> p ) {
    return new DefinitionProcessor( p, getResolvedMap() );
  }

  protected Processor<String> createMarkdownDefinitionProcessor() {
    final var tpc = getCommonProcessor();
    return createDefinitionProcessor( tpc );
  }

  protected Processor<String> createXMLProcessor() {
    final var tpc = getCommonProcessor();
    final var xmlp = new XmlProcessor( tpc, getPath() );
    return createDefinitionProcessor( xmlp );
  }

  private Processor<String> createRProcessor() {
    final var tpc = getCommonProcessor();
    final var rp = new InlineRProcessor( tpc, getResolvedMap() );
    return new RVariableProcessor( rp, getResolvedMap() );
  }

  protected Processor<String> createRXMLProcessor() {
    final var tpc = getCommonProcessor();
    final var xmlp = new XmlProcessor( tpc, getPath() );
    final var rp = new InlineRProcessor( xmlp, getResolvedMap() );
    return new RVariableProcessor( rp, getResolvedMap() );
  }

  private HTMLPreviewPane getPreviewPane() {
    return getContext().getPreviewPane();
  }

  /**
   * Returns the variable map of interpolated definitions.
   *
   * @return A map to help dereference variables.
   */
  private Map<String, String> getResolvedMap() {
    return getContext().getResolvedMap();
  }

  private Path getPath() {
    return getContext().getPath();
  }

  /**
   * Returns a processor common to all processors: markdown, caret position
   * token replacer, and an HTML preview renderer.
   *
   * @return Processors at the end of the processing chain.
   */
  private Processor<String> getCommonProcessor() {
    return mMarkdownProcessor;
  }

  private ProcessorContext getContext() {
    return mProcessorContext;
  }
}
