/* Copyright 2020-2021 White Magic Software, Ltd. -- All rights reserved. */
package com.keenwrite.preferences;

import com.keenwrite.constants.Constants;
import com.keenwrite.sigils.Tokens;
import javafx.application.Platform;
import javafx.beans.property.*;
import javafx.collections.ObservableList;
import org.apache.commons.configuration2.XMLConfiguration;
import org.apache.commons.configuration2.builder.fluent.Configurations;
import org.apache.commons.configuration2.io.FileHandler;

import java.io.File;
import java.time.Year;
import java.time.ZonedDateTime;
import java.util.*;
import java.util.function.BiConsumer;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import java.util.function.Function;

import static com.keenwrite.Bootstrap.APP_TITLE_LOWERCASE;
import static com.keenwrite.Launcher.getVersion;
import static com.keenwrite.constants.Constants.*;
import static com.keenwrite.events.StatusEvent.clue;
import static com.keenwrite.preferences.WorkspaceKeys.*;
import static java.lang.String.valueOf;
import static java.lang.System.getProperty;
import static java.time.format.DateTimeFormatter.RFC_1123_DATE_TIME;
import static java.util.Map.entry;
import static javafx.application.Platform.runLater;
import static javafx.collections.FXCollections.observableArrayList;
import static javafx.collections.FXCollections.observableSet;

/**
 * Responsible for defining behaviours for separate projects. A workspace has
 * the ability to save and restore a session, including the window dimensions,
 * tab setup, files, and user preferences.
 * <p>
 * The configuration must support hierarchical (nested) configuration nodes
 * to persist the user interface state. Although possible with a flat
 * configuration file, it's not nearly as simple or elegant.
 * </p>
 * <p>
 * Neither JSON nor HOCON support schema validation and versioning, which makes
 * XML the more suitable configuration file format. Schema validation and
 * versioning provide future-proofing and ease of reading and upgrading previous
 * versions of the configuration file.
 * </p>
 * <p>
 * Persistent preferences may be set directly by the user or indirectly by
 * the act of using the application.
 * </p>
 * <p>
 * Note the following definitions:
 * </p>
 * <dl>
 *   <dt>File</dt>
 *   <dd>References a file name (no path), path, or directory.</dd>
 *   <dt>Path</dt>
 *   <dd>Fully qualified file name, which includes all parent directories.</dd>
 *   <dt>Dir</dt>
 *   <dd>Directory without a file name ({@link File#isDirectory()} is true)
 *   .</dd>
 * </dl>
 */
public final class Workspace {
  private final Map<Key, Property<?>> VALUES = Map.ofEntries(
    entry( KEY_META_VERSION, asStringProperty( getVersion() ) ),
    entry( KEY_META_NAME, asStringProperty( "default" ) ),

    entry( KEY_DOC_TITLE, asStringProperty( "title" ) ),
    entry( KEY_DOC_AUTHOR, asStringProperty( getProperty( "user.name" ) ) ),
    entry( KEY_DOC_BYLINE, asStringProperty( getProperty( "user.name" ) ) ),
    entry( KEY_DOC_ADDRESS, asStringProperty( "" ) ),
    entry( KEY_DOC_PHONE, asStringProperty( "" ) ),
    entry( KEY_DOC_EMAIL, asStringProperty( "" ) ),
    entry( KEY_DOC_KEYWORDS, asStringProperty( "science, nature" ) ),
    entry( KEY_DOC_COPYRIGHT, asStringProperty( getYear() ) ),
    entry( KEY_DOC_DATE, asStringProperty( getDate() ) ),

    entry( KEY_EDITOR_AUTOSAVE, asIntegerProperty( 30 ) ),

    entry( KEY_R_SCRIPT, asStringProperty( "" ) ),
    entry( KEY_R_DIR, asFileProperty( USER_DIRECTORY ) ),
    entry( KEY_R_DELIM_BEGAN, asStringProperty( R_DELIM_BEGAN_DEFAULT ) ),
    entry( KEY_R_DELIM_ENDED, asStringProperty( R_DELIM_ENDED_DEFAULT ) ),

    entry( KEY_IMAGES_DIR, asFileProperty( USER_DIRECTORY ) ),
    entry( KEY_IMAGES_ORDER, asStringProperty( PERSIST_IMAGES_DEFAULT ) ),
    entry( KEY_IMAGES_RESIZE, asBooleanProperty( true ) ),
    entry( KEY_IMAGES_SERVER, asStringProperty( DIAGRAM_SERVER_NAME ) ),

    entry( KEY_DEF_PATH, asFileProperty( DEFINITION_DEFAULT ) ),
    entry( KEY_DEF_DELIM_BEGAN, asStringProperty( DEF_DELIM_BEGAN_DEFAULT ) ),
    entry( KEY_DEF_DELIM_ENDED, asStringProperty( DEF_DELIM_ENDED_DEFAULT ) ),

    entry( KEY_UI_RECENT_DIR, asFileProperty( USER_DIRECTORY ) ),
    entry( KEY_UI_RECENT_DOCUMENT, asFileProperty( DOCUMENT_DEFAULT ) ),
    entry( KEY_UI_RECENT_DEFINITION, asFileProperty( DEFINITION_DEFAULT ) ),

    //@formatter:off
    entry( KEY_UI_FONT_EDITOR_NAME, asStringProperty( FONT_NAME_EDITOR_DEFAULT ) ),
    entry( KEY_UI_FONT_EDITOR_SIZE, asDoubleProperty( FONT_SIZE_EDITOR_DEFAULT ) ),
    entry( KEY_UI_FONT_PREVIEW_NAME, asStringProperty( FONT_NAME_PREVIEW_DEFAULT ) ),
    entry( KEY_UI_FONT_PREVIEW_SIZE, asDoubleProperty( FONT_SIZE_PREVIEW_DEFAULT ) ),
    entry( KEY_UI_FONT_PREVIEW_MONO_NAME, asStringProperty( FONT_NAME_PREVIEW_MONO_NAME_DEFAULT ) ),
    entry( KEY_UI_FONT_PREVIEW_MONO_SIZE, asDoubleProperty( FONT_SIZE_PREVIEW_MONO_SIZE_DEFAULT ) ),

    entry( KEY_UI_WINDOW_X, asDoubleProperty( WINDOW_X_DEFAULT ) ),
    entry( KEY_UI_WINDOW_Y, asDoubleProperty( WINDOW_Y_DEFAULT ) ),
    entry( KEY_UI_WINDOW_W, asDoubleProperty( WINDOW_W_DEFAULT ) ),
    entry( KEY_UI_WINDOW_H, asDoubleProperty( WINDOW_H_DEFAULT ) ),
    entry( KEY_UI_WINDOW_MAX, asBooleanProperty() ),
    entry( KEY_UI_WINDOW_FULL, asBooleanProperty() ),

    entry( KEY_UI_SKIN_SELECTION, asSkinProperty( SKIN_DEFAULT ) ),
    entry( KEY_UI_SKIN_CUSTOM, asFileProperty( SKIN_CUSTOM_DEFAULT ) ),

    entry( KEY_UI_PREVIEW_STYLESHEET, asFileProperty( PREVIEW_CUSTOM_DEFAULT ) ),

    entry( KEY_LANGUAGE_LOCALE, asLocaleProperty( LOCALE_DEFAULT ) ),

    entry( KEY_TYPESET_CONTEXT_CLEAN, asBooleanProperty( true ) ),
    entry( KEY_TYPESET_CONTEXT_THEMES_PATH, asFileProperty( USER_DIRECTORY ) ),
    entry( KEY_TYPESET_CONTEXT_THEME_SELECTION, asStringProperty( "boschet" ) ),
    entry( KEY_TYPESET_TYPOGRAPHY_QUOTES, asBooleanProperty( true ) )
    //@formatter:on
  );

  private StringProperty asStringProperty( final String defaultValue ) {
    return new SimpleStringProperty( defaultValue );
  }

  @SuppressWarnings( "SameParameterValue" )
  private IntegerProperty asIntegerProperty( final int defaultValue ) {
    return new SimpleIntegerProperty( defaultValue );
  }

  private DoubleProperty asDoubleProperty( final double defaultValue ) {
    return new SimpleDoubleProperty( defaultValue );
  }

  private BooleanProperty asBooleanProperty() {
    return new SimpleBooleanProperty();
  }

  @SuppressWarnings( "SameParameterValue" )
  private BooleanProperty asBooleanProperty( final boolean defaultValue ) {
    return new SimpleBooleanProperty( defaultValue );
  }

  private FileProperty asFileProperty( final File defaultValue ) {
    return new FileProperty( defaultValue );
  }

  @SuppressWarnings( "SameParameterValue" )
  private SkinProperty asSkinProperty( final String defaultValue ) {
    return new SkinProperty( defaultValue );
  }

  @SuppressWarnings( "SameParameterValue" )
  private LocaleProperty asLocaleProperty( final Locale defaultValue ) {
    return new LocaleProperty( defaultValue );
  }

  /**
   * Helps instantiate {@link Property} instances for XML configuration items.
   */
  private static final Map<Class<?>, Function<String, Object>> UNMARSHALL =
    Map.of(
      LocaleProperty.class, LocaleProperty::parseLocale,
      SimpleBooleanProperty.class, Boolean::parseBoolean,
      SimpleIntegerProperty.class, Integer::parseInt,
      SimpleDoubleProperty.class, Double::parseDouble,
      SimpleFloatProperty.class, Float::parseFloat,
      FileProperty.class, File::new
    );

  private static final Map<Class<?>, Function<String, Object>> MARSHALL =
    Map.of(
      LocaleProperty.class, LocaleProperty::toLanguageTag
    );

  private final Map<Key, SetProperty<?>> SETS = Map.ofEntries(
    entry(
      KEY_UI_FILES_PATH,
      new SimpleSetProperty<>( observableSet( new HashSet<>() ) )
    )
  );

  /**
   * Creates a new {@link Workspace} that will attempt to load a configuration
   * file. If the configuration file cannot be loaded, the workspace settings
   * will return default values. This allows unit tests to provide an instance
   * of {@link Workspace} when necessary without encountering failures.
   */
  public Workspace() {
    load( FILE_PREFERENCES );
  }

  /**
   * Creates a new {@link Workspace} that will attempt to load the given
   * configuration file.
   *
   * @param filename The file to load.
   */
  public Workspace( final String filename ) {
    load( filename );
  }

  /**
   * Creates an instance of {@link ObservableList} that is based on a
   * modifiable observable array list for the given items.
   *
   * @param items The items to wrap in an observable list.
   * @param <E>   The type of items to add to the list.
   * @return An observable property that can have its contents modified.
   */
  public static <E> ObservableList<E> listProperty( final Set<E> items ) {
    return new SimpleListProperty<>( observableArrayList( items ) );
  }

  /**
   * Returns a value that represents a setting in the application that the user
   * may configure, either directly or indirectly.
   *
   * @param key The reference to the users' preference stored in deference
   *            of app reëntrance.
   * @return An observable property to be persisted.
   */
  @SuppressWarnings( "unchecked" )
  public <T, U extends Property<T>> U valuesProperty( final Key key ) {
    assert key != null;
    // The type that goes into the map must come out.
    return (U) VALUES.get( key );
  }

  /**
   * Returns a list of values that represent a setting in the application that
   * the user may configure, either directly or indirectly. The property
   * returned is backed by a mutable {@link Set}.
   *
   * @param key The {@link Key} associated with a preference value.
   * @return An observable property to be persisted.
   */
  @SuppressWarnings( "unchecked" )
  public <T> SetProperty<T> setsProperty( final Key key ) {
    assert key != null;
    // The type that goes into the map must come out.
    return (SetProperty<T>) SETS.get( key );
  }

  /**
   * Returns the {@link Boolean} preference value associated with the given
   * {@link Key}. The caller must be sure that the given {@link Key} is
   * associated with a value that matches the return type.
   *
   * @param key The {@link Key} associated with a preference value.
   * @return The value associated with the given {@link Key}.
   */
  public boolean toBoolean( final Key key ) {
    assert key != null;
    return (Boolean) valuesProperty( key ).getValue();
  }

  /**
   * Returns the {@link Integer} preference value associated with the given
   * {@link Key}. The caller must be sure that the given {@link Key} is
   * associated with a value that matches the return type.
   *
   * @param key The {@link Key} associated with a preference value.
   * @return The value associated with the given {@link Key}.
   */
  public int toInteger( final Key key ) {
    assert key != null;
    return (Integer) valuesProperty( key ).getValue();
  }

  /**
   * Returns the {@link Double} preference value associated with the given
   * {@link Key}. The caller must be sure that the given {@link Key} is
   * associated with a value that matches the return type.
   *
   * @param key The {@link Key} associated with a preference value.
   * @return The value associated with the given {@link Key}.
   */
  public double toDouble( final Key key ) {
    assert key != null;
    return (Double) valuesProperty( key ).getValue();
  }

  public File toFile( final Key key ) {
    assert key != null;
    return fileProperty( key ).get();
  }

  public String toString( final Key key ) {
    assert key != null;
    return stringProperty( key ).get();
  }

  public Tokens toTokens( final Key began, final Key ended ) {
    assert began != null;
    assert ended != null;
    return new Tokens( stringProperty( began ), stringProperty( ended ) );
  }

  @SuppressWarnings( "SameParameterValue" )
  public IntegerProperty integerProperty( final Key key ) {
    assert key != null;
    return valuesProperty( key );
  }

  @SuppressWarnings( "SameParameterValue" )
  public DoubleProperty doubleProperty( final Key key ) {
    assert key != null;
    return valuesProperty( key );
  }

  /**
   * Returns the {@link File} {@link Property} associated with the given
   * {@link Key} from the internal list of preference values. The caller
   * must be sure that the given {@link Key} is associated with a {@link File}
   * {@link Property}.
   *
   * @param key The {@link Key} associated with a preference value.
   * @return The value associated with the given {@link Key}.
   */
  public ObjectProperty<File> fileProperty( final Key key ) {
    assert key != null;
    return valuesProperty( key );
  }

  public ObjectProperty<String> skinProperty( final Key key ) {
    assert key != null;
    return valuesProperty( key );
  }

  public LocaleProperty localeProperty( final Key key ) {
    assert key != null;
    return valuesProperty( key );
  }

  /**
   * Returns the language locale setting for the
   * {@link WorkspaceKeys#KEY_LANGUAGE_LOCALE} key.
   *
   * @return The user's current locale setting.
   */
  public Locale getLocale() {
    return localeProperty( KEY_LANGUAGE_LOCALE ).toLocale();
  }

  public StringProperty stringProperty( final Key key ) {
    assert key != null;
    return valuesProperty( key );
  }

  public BooleanProperty booleanProperty( final Key key ) {
    assert key != null;
    return valuesProperty( key );
  }

  public void loadValueKeys( final Consumer<Key> consumer ) {
    VALUES.keySet().forEach( consumer );
  }

  public void loadSetKeys( final Consumer<Key> consumer ) {
    SETS.keySet().forEach( consumer );
  }

  /**
   * Calls the given consumer for all single-value keys. For lists, see
   * {@link #saveSets(BiConsumer)}.
   *
   * @param consumer Called to accept each preference key value.
   */
  public void saveValues( final BiConsumer<Key, Property<?>> consumer ) {
    VALUES.forEach( consumer );
  }

  /**
   * Calls the given consumer for all multi-value keys. For single items, see
   * {@link #saveValues(BiConsumer)}. Callers are responsible for iterating
   * over the list of items retrieved through this method.
   *
   * @param consumer Called to accept each preference key list.
   */
  public void saveSets( final BiConsumer<Key, SetProperty<?>> consumer ) {
    SETS.forEach( consumer );
  }

  /**
   * Delegates to {@link #listen(Key, ReadOnlyProperty, BooleanSupplier)},
   * providing a value of {@code true} for the {@link BooleanSupplier} to
   * indicate the property changes always take effect.
   *
   * @param key      The value to bind to the internal key property.
   * @param property The external property value that sets the internal value.
   */
  public <T> void listen( final Key key, final ReadOnlyProperty<T> property ) {
    listen( key, property, () -> true );
  }

  /**
   * Binds a read-only property to a value in the preferences. This allows
   * user interface properties to change and the preferences will be
   * synchronized automatically.
   * <p>
   * This calls {@link Platform#runLater(Runnable)} to ensure that all pending
   * application window states are finished before assessing whether property
   * changes should be applied. Without this, exiting the application while the
   * window is maximized would persist the window's maximum dimensions,
   * preventing restoration to its prior, non-maximum size.
   * </p>
   *
   * @param key      The value to bind to the internal key property.
   * @param property The external property value that sets the internal value.
   * @param enabled  Indicates whether property changes should be applied.
   */
  public <T> void listen(
    final Key key,
    final ReadOnlyProperty<T> property,
    final BooleanSupplier enabled ) {
    property.addListener(
      ( c, o, n ) -> runLater( () -> {
        if( enabled.getAsBoolean() ) {
          valuesProperty( key ).setValue( n );
        }
      } )
    );
  }

  /**
   * Saves the current workspace.
   */
  public void save() {
    try {
      final var config = new XMLConfiguration();

      // The root config key can only be set for an empty configuration file.
      config.setRootElementName( APP_TITLE_LOWERCASE );
      valuesProperty( KEY_META_VERSION ).setValue( getVersion() );

      saveValues( ( key, property ) ->
                    config.setProperty( key.toString(), marshall( property ) )
      );

      saveSets( ( key, set ) -> {
        final var keyName = key.toString();
        set.forEach( ( value ) -> config.addProperty( keyName, value ) );
      } );
      new FileHandler( config ).save( FILE_PREFERENCES );
    } catch( final Exception ex ) {
      clue( ex );
    }
  }

  /**
   * Attempts to load the {@link Constants#FILE_PREFERENCES} configuration file.
   * If not found, this will fall back to an empty configuration file, leaving
   * the application to fill in default values.
   *
   * @param filename The file containing user preferences to load.
   */
  private void load( final String filename ) {
    try {
      final var config = new Configurations().xml( filename );

      loadValueKeys( ( key ) -> {
        final var configValue = config.getProperty( key.toString() );

        // Allow other properties to load, even if any are missing.
        if( configValue != null ) {
          final var propertyValue = valuesProperty( key );
          propertyValue.setValue( unmarshall( propertyValue, configValue ) );
        }
      } );

      loadSetKeys( ( key ) -> {
        final var configSet =
          new LinkedHashSet<>( config.getList( key.toString() ) );
        final var propertySet = setsProperty( key );
        propertySet.setValue( observableSet( configSet ) );
      } );
    } catch( final Exception ex ) {
      clue( ex );
    }
  }

  private Object unmarshall(
    final Property<?> property, final Object configValue ) {
    final var setting = configValue.toString();

    return UNMARSHALL
      .getOrDefault( property.getClass(), ( value ) -> value )
      .apply( setting );
  }

  private Object marshall( final Property<?> property ) {
    return property.getValue() == null
      ? null
      : MARSHALL
      .getOrDefault( property.getClass(), ( __ ) -> property.getValue() )
      .apply( property.getValue().toString() );
  }

  private String getYear() {
    return valueOf( Year.now().getValue() );
  }

  private String getDate() {
    return ZonedDateTime.now().format( RFC_1123_DATE_TIME );
  }
}
