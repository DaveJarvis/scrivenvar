/* Copyright 2020-2021 White Magic Software, Ltd. -- All rights reserved. */
package com.keenwrite.preview;

import com.keenwrite.preview.images.Lanczos3Filter;
import com.keenwrite.preview.images.ResampleOp;
import org.xhtmlrenderer.swing.ImageReplacedElement;

import java.awt.*;
import java.awt.image.BufferedImage;

/**
 * Responsible for scaling an image using a Lanczos-3 filter, typically for
 * down-sampling.
 */
public final class SmoothImageReplacedElement extends ImageReplacedElement {
  private final static Lanczos3Filter FILTER = new Lanczos3Filter();

  /**
   * Creates a high-quality rescaled version of the given image. The
   * aspect ratio is preserved if either width or height is less than 1.
   *
   * @param source An instance of {@link BufferedImage} to rescale.
   * @param width  Rescale the given image to this width (px).
   * @param height Rescale the given image to this height (px).
   */
  public SmoothImageReplacedElement(
    final Image source, final int width, final int height ) {
    super._image = rescale( source, width, height );
  }

  private BufferedImage rescale(
    final Image source, final int w, final int h ) {
    final var bi = (BufferedImage) source;
    final var dim = rescaleDimensions( bi, w, h );

    final var resampleOp = new ResampleOp( FILTER, dim.width, dim.height );
    return resampleOp.filter( bi, null );
  }

  private Dimension rescaleDimensions(
    final BufferedImage bi, final int width, final int height ) {
    final var w = bi.getWidth();
    final var h = bi.getHeight();

    int newW = width;
    int newH = height;

    if( newW <= 0 ) {
      newW = (int) (w * ((double) newH / h));
    }

    if( newH <= 0 ) {
      newH = (int) (h * ((double) newW / w));
    }

    return new Dimension( newW, newH );
  }
}