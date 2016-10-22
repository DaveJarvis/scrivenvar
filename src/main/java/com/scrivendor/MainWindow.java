/*
 * Copyright (c) 2015 Karl Tauber <karl at jformdesigner dot com>
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
package com.scrivendor;

import static com.scrivendor.Constants.LOGO_32;
import com.scrivendor.definition.DefinitionPane;
import com.scrivendor.editor.MarkdownEditorPane;
import com.scrivendor.options.OptionsDialog;
import com.scrivendor.util.Action;
import com.scrivendor.util.ActionUtils;
import static de.jensd.fx.glyphs.fontawesome.FontAwesomeIcon.BOLD;
import static de.jensd.fx.glyphs.fontawesome.FontAwesomeIcon.CODE;
import static de.jensd.fx.glyphs.fontawesome.FontAwesomeIcon.FILE_ALT;
import static de.jensd.fx.glyphs.fontawesome.FontAwesomeIcon.FILE_CODE_ALT;
import static de.jensd.fx.glyphs.fontawesome.FontAwesomeIcon.FLOPPY_ALT;
import static de.jensd.fx.glyphs.fontawesome.FontAwesomeIcon.FOLDER_OPEN_ALT;
import static de.jensd.fx.glyphs.fontawesome.FontAwesomeIcon.HEADER;
import static de.jensd.fx.glyphs.fontawesome.FontAwesomeIcon.ITALIC;
import static de.jensd.fx.glyphs.fontawesome.FontAwesomeIcon.LINK;
import static de.jensd.fx.glyphs.fontawesome.FontAwesomeIcon.LIST_OL;
import static de.jensd.fx.glyphs.fontawesome.FontAwesomeIcon.LIST_UL;
import static de.jensd.fx.glyphs.fontawesome.FontAwesomeIcon.PICTURE_ALT;
import static de.jensd.fx.glyphs.fontawesome.FontAwesomeIcon.QUOTE_LEFT;
import static de.jensd.fx.glyphs.fontawesome.FontAwesomeIcon.REPEAT;
import static de.jensd.fx.glyphs.fontawesome.FontAwesomeIcon.STRIKETHROUGH;
import static de.jensd.fx.glyphs.fontawesome.FontAwesomeIcon.UNDO;
import java.io.IOException;
import java.text.MessageFormat;
import java.util.function.Function;
import javafx.beans.binding.Bindings;
import javafx.beans.binding.BooleanBinding;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.value.ObservableBooleanValue;
import javafx.event.Event;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuBar;
import javafx.scene.control.ToolBar;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import static javafx.scene.input.KeyCode.ESCAPE;
import javafx.scene.input.KeyEvent;
import static javafx.scene.input.KeyEvent.CHAR_UNDEFINED;
import static javafx.scene.input.KeyEvent.KEY_PRESSED;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.VBox;
import javafx.stage.Window;
import javafx.stage.WindowEvent;

/**
 * Main window containing a tab pane in the center for file editors.
 *
 * @author Karl Tauber
 */
public class MainWindow {

  private final Scene scene;
  private final FileEditorPane fileEditorPane;
  private final DefinitionPane definitionPane;
  
  private MenuBar menuBar;

  public MainWindow() {
    this.fileEditorPane = new FileEditorPane( this );
    this.definitionPane = new DefinitionPane();

    BorderPane borderPane = new BorderPane();
    borderPane.setPrefSize( 1024, 800 );
    borderPane.setTop( createMenuBar() );
    borderPane.setCenter( fileEditorPane.getNode() );
    borderPane.setLeft( definitionPane.getNode() );

    this.scene = new Scene( borderPane );
    this.scene.getStylesheets().add( Constants.STYLESHEET_PREVIEW );
    this.scene.windowProperty().addListener( (observable, oldWindow, newWindow) -> {
      newWindow.setOnCloseRequest( e -> {
        if( !this.fileEditorPane.closeAllEditors() ) {
          e.consume();
        }
      } );

      // workaround for a bug in JavaFX: unselect menubar if window looses focus
      newWindow.focusedProperty().addListener( (obs, oldFocused, newFocused) -> {
        if( !newFocused ) {
          // send an ESC key event to the menubar
          this.menuBar.fireEvent( new KeyEvent( KEY_PRESSED, CHAR_UNDEFINED, "", ESCAPE,
            false, false, false, false ) );
        }
      } );
    } );

  }

  public Scene getScene() {
    return scene;
  }

  private Node createMenuBar() {
    BooleanBinding activeFileEditorIsNull = fileEditorPane.activeFileEditorProperty().isNull();

    // File actions
    Action fileNewAction = new Action( Messages.get( "Main.menu.file.new" ), "Shortcut+N", FILE_ALT, e -> fileNew() );
    Action fileOpenAction = new Action( Messages.get( "Main.menu.file.open" ), "Shortcut+O", FOLDER_OPEN_ALT, e -> fileOpen() );
    Action fileCloseAction = new Action( Messages.get( "Main.menu.file.close" ), "Shortcut+W", null, e -> fileClose(), activeFileEditorIsNull );
    Action fileCloseAllAction = new Action( Messages.get( "Main.menu.file.close_all" ), null, null, e -> fileCloseAll(), activeFileEditorIsNull );
    Action fileSaveAction = new Action( Messages.get( "Main.menu.file.save" ), "Shortcut+S", FLOPPY_ALT, e -> fileSave(),
      createActiveBooleanProperty( FileEditor::modifiedProperty ).not() );
    Action fileSaveAllAction = new Action( Messages.get( "Main.menu.file.save_all" ), "Shortcut+Shift+S", null, e -> fileSaveAll(),
      Bindings.not( fileEditorPane.anyFileEditorModifiedProperty() ) );
    Action fileExitAction = new Action( Messages.get( "Main.menu.file.exit" ), null, null, e -> fileExit() );

    // Edit actions
    Action editUndoAction = new Action( Messages.get( "Main.menu.edit.undo" ), "Shortcut+Z", UNDO,
      e -> getActiveEditor().undo(),
      createActiveBooleanProperty( FileEditor::canUndoProperty ).not() );
    Action editRedoAction = new Action( Messages.get( "Main.menu.edit.redo" ), "Shortcut+Y", REPEAT,
      e -> getActiveEditor().redo(),
      createActiveBooleanProperty( FileEditor::canRedoProperty ).not() );

    // Insert actions
    Action insertBoldAction = new Action( Messages.get( "Main.menu.insert.bold" ), "Shortcut+B", BOLD,
      e -> getActiveEditor().surroundSelection( "**", "**" ),
      activeFileEditorIsNull );
    Action insertItalicAction = new Action( Messages.get( "Main.menu.insert.italic" ), "Shortcut+I", ITALIC,
      e -> getActiveEditor().surroundSelection( "*", "*" ),
      activeFileEditorIsNull );
    Action insertStrikethroughAction = new Action( Messages.get( "Main.menu.insert.strikethrough" ), "Shortcut+T", STRIKETHROUGH,
      e -> getActiveEditor().surroundSelection( "~~", "~~" ),
      activeFileEditorIsNull );
    Action insertBlockquoteAction = new Action( Messages.get( "Main.menu.insert.blockquote" ), "Ctrl+Q", QUOTE_LEFT, // not Shortcut+Q because of conflict on Mac
      e -> getActiveEditor().surroundSelection( "\n\n> ", "" ),
      activeFileEditorIsNull );
    Action insertCodeAction = new Action( Messages.get( "Main.menu.insert.code" ), "Shortcut+K", CODE,
      e -> getActiveEditor().surroundSelection( "`", "`" ),
      activeFileEditorIsNull );
    Action insertFencedCodeBlockAction = new Action( Messages.get( "Main.menu.insert.fenced_code_block" ), "Shortcut+Shift+K", FILE_CODE_ALT,
      e -> getActiveEditor().surroundSelection( "\n\n```\n", "\n```\n\n", Messages.get( "Main.menu.insert.fenced_code_block.prompt" ) ),
      activeFileEditorIsNull );

    Action insertLinkAction = new Action( Messages.get( "Main.menu.insert.link" ), "Shortcut+L", LINK,
      e -> getActiveEditor().insertLink(),
      activeFileEditorIsNull );
    Action insertImageAction = new Action( Messages.get( "Main.menu.insert.image" ), "Shortcut+G", PICTURE_ALT,
      e -> getActiveEditor().insertImage(),
      activeFileEditorIsNull );

    final Action[] headers = new Action[ 6 ];

    for( int i = 1; i <= 6; i++ ) {
      final String hashes = new String( new char[ i ] ).replace( "\0", "#" );
      final String markup = String.format( "\n\n%s ", hashes );
      final String text = Messages.get( "Main.menu.insert.header_" + i );
      final String accelerator = "Shortcut+" + i;
      final String prompt = Messages.get( "Main.menu.insert.header_" + i + ".prompt" );

      headers[ i - 1 ] = new Action( text, accelerator, HEADER,
        e -> getActiveEditor().surroundSelection( markup, "", prompt ),
        activeFileEditorIsNull );
    }

    Action insertUnorderedListAction = new Action( Messages.get( "Main.menu.insert.unordered_list" ), "Shortcut+U", LIST_UL,
      e -> getActiveEditor().surroundSelection( "\n\n* ", "" ),
      activeFileEditorIsNull );
    Action insertOrderedListAction = new Action( Messages.get( "Main.menu.insert.ordered_list" ), "Shortcut+Shift+O", LIST_OL,
      e -> getActiveEditor().surroundSelection( "\n\n1. ", "" ),
      activeFileEditorIsNull );
    Action insertHorizontalRuleAction = new Action( Messages.get( "Main.menu.insert.horizontal_rule" ), "Shortcut+H", null,
      e -> getActiveEditor().surroundSelection( "\n\n---\n\n", "" ),
      activeFileEditorIsNull );

    // Tools actions
    Action toolsOptionsAction = new Action( Messages.get( "Main.menu.tools.options" ), "Shortcut+,", null, e -> toolsOptions() );

    // Help actions
    Action helpAboutAction = new Action( Messages.get( "Main.menu.help.about" ), null, null, e -> helpAbout() );

    //---- MenuBar ----
    Menu fileMenu = ActionUtils.createMenu( Messages.get( "Main.menu.file" ),
      fileNewAction,
      fileOpenAction,
      null,
      fileCloseAction,
      fileCloseAllAction,
      null,
      fileSaveAction,
      fileSaveAllAction,
      null,
      fileExitAction );

    Menu editMenu = ActionUtils.createMenu( Messages.get( "Main.menu.edit" ),
      editUndoAction,
      editRedoAction );

    Menu insertMenu = ActionUtils.createMenu( Messages.get( "Main.menu.insert" ),
      insertBoldAction,
      insertItalicAction,
      insertStrikethroughAction,
      insertBlockquoteAction,
      insertCodeAction,
      insertFencedCodeBlockAction,
      null,
      insertLinkAction,
      insertImageAction,
      null,
      headers[ 0 ],
      headers[ 1 ],
      headers[ 2 ],
      headers[ 3 ],
      headers[ 4 ],
      headers[ 5 ],
      null,
      insertUnorderedListAction,
      insertOrderedListAction,
      insertHorizontalRuleAction );

    Menu toolsMenu = ActionUtils.createMenu( Messages.get( "Main.menu.tools" ),
      toolsOptionsAction );

    Menu helpMenu = ActionUtils.createMenu( Messages.get( "Main.menu.help" ),
      helpAboutAction );

    menuBar = new MenuBar( fileMenu, editMenu, insertMenu, toolsMenu, helpMenu );

    //---- ToolBar ----
    ToolBar toolBar = ActionUtils.createToolBar(
      fileNewAction,
      fileOpenAction,
      fileSaveAction,
      null,
      editUndoAction,
      editRedoAction,
      null,
      insertBoldAction,
      insertItalicAction,
      insertBlockquoteAction,
      insertCodeAction,
      insertFencedCodeBlockAction,
      null,
      insertLinkAction,
      insertImageAction,
      null,
      headers[ 0 ],
      null,
      insertUnorderedListAction,
      insertOrderedListAction );

    return new VBox( menuBar, toolBar );
  }

  private MarkdownEditorPane getActiveEditor() {
    return fileEditorPane.getActiveFileEditor().getEditor();
  }

  /**
   * Creates a boolean property that is bound to another boolean value of the
   * active editor.
   */
  private BooleanProperty createActiveBooleanProperty( Function<FileEditor, ObservableBooleanValue> func ) {
    BooleanProperty b = new SimpleBooleanProperty();
    FileEditor fileEditor = fileEditorPane.getActiveFileEditor();
    if( fileEditor != null ) {
      b.bind( func.apply( fileEditor ) );
    }
    fileEditorPane.activeFileEditorProperty().addListener( (observable, oldFileEditor, newFileEditor) -> {
      b.unbind();
      if( newFileEditor != null ) {
        b.bind( func.apply( newFileEditor ) );
      } else {
        b.set( false );
      }
    } );
    return b;
  }

  Alert createAlert( AlertType alertType, String title,
    String contentTextFormat, Object... contentTextArgs ) {
    Alert alert = new Alert( alertType );
    alert.setTitle( title );
    alert.setHeaderText( null );
    alert.setContentText( MessageFormat.format( contentTextFormat, contentTextArgs ) );
    alert.initOwner( getScene().getWindow() );
    return alert;
  }

  //---- File actions -------------------------------------------------------
  private void fileNew() {
    fileEditorPane.newEditor();
  }

  private void fileOpen() {
    fileEditorPane.openEditor();
  }

  private void fileClose() {
    fileEditorPane.closeEditor( fileEditorPane.getActiveFileEditor(), true );
  }

  private void fileCloseAll() {
    fileEditorPane.closeAllEditors();
  }

  private void fileSave() {
    fileEditorPane.saveEditor( fileEditorPane.getActiveFileEditor() );
  }

  private void fileSaveAll() {
    fileEditorPane.saveAllEditors();
  }

  private void fileExit() {
    Window window = scene.getWindow();
    Event.fireEvent( window, new WindowEvent( window, WindowEvent.WINDOW_CLOSE_REQUEST ) );
  }

  //---- Tools actions ------------------------------------------------------
  private void toolsOptions() {
    OptionsDialog dialog = new OptionsDialog( getScene().getWindow() );
    dialog.showAndWait();
  }

  //---- Help actions -------------------------------------------------------
  private void helpAbout() {
    Alert alert = new Alert( AlertType.INFORMATION );
    alert.setTitle( Messages.get( "Dialog.about.title" ) );
    alert.setHeaderText( Messages.get( "Dialog.about.header" ) );
    alert.setContentText( Messages.get( "Dialog.about.content" ) );
    alert.setGraphic( new ImageView( new Image( LOGO_32 ) ) );
    alert.initOwner( getScene().getWindow() );

    alert.showAndWait();
  }
}
