package io.ballerina.plugins.idea.ui.settings;

import com.intellij.ide.highlighter.HighlighterFactory;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.ReadAction;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.EditorFactory;
import com.intellij.openapi.editor.EditorSettings;
import com.intellij.openapi.editor.colors.EditorColorsManager;
import com.intellij.openapi.editor.ex.EditorEx;
import com.intellij.openapi.editor.highlighter.EditorHighlighter;
import com.intellij.openapi.fileChooser.FileChooserDescriptorFactory;
import com.intellij.openapi.fileTypes.FileType;
import com.intellij.openapi.fileTypes.FileTypeManager;
import com.intellij.openapi.fileTypes.UnknownFileType;
import com.intellij.openapi.options.ConfigurationException;
import com.intellij.openapi.ui.ComboBox;
import com.intellij.openapi.ui.TextBrowseFolderListener;
import com.intellij.openapi.ui.TextFieldWithBrowseButton;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.ui.CollectionComboBoxModel;
import com.intellij.ui.EnumComboBoxModel;
import com.intellij.ui.ListCellRendererWrapper;
import com.intellij.ui.TitledSeparator;
import com.intellij.ui.components.JBCheckBox;
import com.intellij.util.containers.ContainerUtil;
import io.ballerina.plugins.idea.ui.preview.BallerinaHtmlPanelProvider;
import io.ballerina.plugins.idea.ui.split.SplitFileEditor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.List;
import java.util.Objects;
import javax.swing.*;

public class MarkdownSettingsForm implements MarkdownCssSettings.Holder, MarkdownPreviewSettings.Holder, Disposable {
  private static final String JAVA_FX_HTML_PANEL_PROVIDER = "JavaFxHtmlPanelProvider";
  private JPanel myMainPanel;
  private JBCheckBox myCssFromURIEnabled;
  private TextFieldWithBrowseButton myCssURI;
  private JBCheckBox myApplyCustomCssText;
  private JPanel myEditorPanel;
  private JPanel myCssTitledSeparator;
  private ComboBox myPreviewProvider;
  private ComboBox myDefaultSplitLayout;
  private JBCheckBox myUseGrayscaleRenderingForJBCheckBox;
  private JPanel myPreviewTitledSeparator;
  private JBCheckBox myAutoScrollCheckBox;
  private JPanel myMultipleProvidersPreviewPanel;

  @Nullable
  private EditorEx myEditor;
  @NotNull
  private final ActionListener myCssURIListener;
  @NotNull
  private final ActionListener myCustomCssTextListener;

  private Object myLastItem;
  private EnumComboBoxModel<SplitFileEditor.SplitEditorLayout> mySplitLayoutModel;
  private CollectionComboBoxModel<BallerinaHtmlPanelProvider.ProviderInfo> myPreviewPanelModel;

  public MarkdownSettingsForm() {
    myCssURIListener = new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
        myCssURI.setEnabled(myCssFromURIEnabled.isSelected());
      }
    };

    myCustomCssTextListener = new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
        adjustCSSRulesAvailability();
      }
    };

    adjustCSSRulesAvailability();

    myCssFromURIEnabled.addActionListener(myCssURIListener);
    myApplyCustomCssText.addActionListener(myCustomCssTextListener);
    myCssURI.addBrowseFolderListener(new TextBrowseFolderListener(FileChooserDescriptorFactory.createSingleFileDescriptor("css")) {
      @NotNull
      @Override
      protected String chosenFileToResultingText(@NotNull VirtualFile chosenFile) {
        return chosenFile.getUrl();
      }
    });

    myMultipleProvidersPreviewPanel.setVisible(isMultipleProviders());
    updateUseGrayscaleEnabled();

    myDefaultSplitLayout.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
        adjustAutoScroll();
      }
    });

    adjustAutoScroll();
  }

  private void adjustAutoScroll() {
    myAutoScrollCheckBox.setEnabled(myDefaultSplitLayout.getSelectedItem() == SplitFileEditor.SplitEditorLayout.SPLIT);
  }

  private void adjustCSSRulesAvailability() {
    if (myEditor != null) {
      boolean enabled = myApplyCustomCssText.isSelected();
      myEditor.getDocument().setReadOnly(!enabled);
      myEditor.getContentComponent().setEnabled(enabled);
      myEditor.setCaretEnabled(enabled);
    }
  }

  public JComponent getComponent() {
    return myMainPanel;
  }

  private void createUIComponents() {
    myEditorPanel = new JPanel(new BorderLayout());

    myEditor = createEditor();
    myEditorPanel.add(myEditor.getComponent(), BorderLayout.CENTER);

    myCssTitledSeparator = new TitledSeparator("Title");

    createPreviewUIComponents();
  }

  private static boolean isMultipleProviders() {
    return BallerinaHtmlPanelProvider.getProviders().length > 1;
  }

  public void validate() throws ConfigurationException {
    if (!myCssFromURIEnabled.isSelected()) return;

    try {
      new URL(myCssURI.getText()).toURI();
    }
    catch (URISyntaxException | MalformedURLException e) {
      throw new ConfigurationException("URI '" + myCssURI.getText() + "' parsing reports the error: " + e.getMessage());
    }
  }

  @NotNull
  private static EditorEx createEditor() {
    EditorFactory editorFactory = EditorFactory.getInstance();
    Document editorDocument = editorFactory.createDocument("");
    EditorEx editor = (EditorEx)editorFactory.createEditor(editorDocument);
    fillEditorSettings(editor.getSettings());
    setHighlighting(editor);
    return editor;
  }

  private static void setHighlighting(EditorEx editor) {
    final FileType cssFileType = FileTypeManager.getInstance().getFileTypeByExtension("css");
    if (cssFileType == UnknownFileType.INSTANCE) {
      return;
    }
    final EditorHighlighter editorHighlighter =
      HighlighterFactory.createHighlighter(cssFileType, EditorColorsManager.getInstance().getGlobalScheme(), null);
    editor.setHighlighter(editorHighlighter);
  }

  private static void fillEditorSettings(final EditorSettings editorSettings) {
    editorSettings.setWhitespacesShown(false);
    editorSettings.setLineMarkerAreaShown(false);
    editorSettings.setIndentGuidesShown(false);
    editorSettings.setLineNumbersShown(true);
    editorSettings.setFoldingOutlineShown(false);
    editorSettings.setAdditionalColumnsCount(1);
    editorSettings.setAdditionalLinesCount(1);
    editorSettings.setUseSoftWraps(false);
  }

  @Override
  public void setMarkdownCssSettings(@NotNull MarkdownCssSettings settings) {
    myCssFromURIEnabled.setSelected(settings.isUriEnabled());
    myCssURI.setText(settings.getStylesheetUri());
    myApplyCustomCssText.setSelected(settings.isTextEnabled());
    resetEditor(settings.getStylesheetText());

    //noinspection ConstantConditions
    myCssURIListener.actionPerformed(null);
    myCustomCssTextListener.actionPerformed(null);
  }

  void resetEditor(@NotNull String cssText) {
    if (myEditor != null && !myEditor.isDisposed()) {
      ApplicationManager.getApplication().runWriteAction(() -> {
        boolean writable = myEditor.getDocument().isWritable();
        myEditor.getDocument().setReadOnly(false);
        myEditor.getDocument().setText(cssText);
        myEditor.getDocument().setReadOnly(!writable);
      });
    }
  }

  @NotNull
  @Override
  public MarkdownCssSettings getMarkdownCssSettings() {
    return new MarkdownCssSettings(myCssFromURIEnabled.isSelected(),
                                   myCssURI.getText(),
                                   myApplyCustomCssText.isSelected(),
                                   myEditor != null && !myEditor.isDisposed() ?
                                   ReadAction.compute(() -> myEditor.getDocument().getText()) : "");
  }

  @Override
  public void dispose() {
    if (myEditor != null && !myEditor.isDisposed()) {
      EditorFactory.getInstance().releaseEditor(myEditor);
    }
    myEditor = null;
  }

  private void createPreviewUIComponents() {
    myPreviewTitledSeparator = new TitledSeparator("Preview");
    mySplitLayoutModel = new EnumComboBoxModel<>(SplitFileEditor.SplitEditorLayout.class);
    myDefaultSplitLayout = new ComboBox<>(mySplitLayoutModel);
    myDefaultSplitLayout.setRenderer(new ListCellRendererWrapper<SplitFileEditor.SplitEditorLayout>() {
      @Override
      public void customize(JList list, SplitFileEditor.SplitEditorLayout value, int index, boolean selected, boolean hasFocus) {
        setText(value.getPresentationText());
      }
    });

    createMultipleProvidersSettings();
  }

  private void createMultipleProvidersSettings() {
    //noinspection unchecked
    final List<BallerinaHtmlPanelProvider.ProviderInfo> providerInfos =
      ContainerUtil.mapNotNull(BallerinaHtmlPanelProvider.getProviders(),
                               provider -> {
                                 if (provider.isAvailable() == BallerinaHtmlPanelProvider.AvailabilityInfo.UNAVAILABLE) {
                                   return null;
                                 }
                                 return provider.getProviderInfo();
                               });
    myPreviewPanelModel = new CollectionComboBoxModel<>(providerInfos, providerInfos.get(0));
    myPreviewProvider = new ComboBox<>(myPreviewPanelModel);

    myLastItem = myPreviewProvider.getSelectedItem();
    myPreviewProvider.addItemListener(new ItemListener() {
      @Override
      public void itemStateChanged(ItemEvent e) {
        final Object item = e.getItem();
        if (e.getStateChange() != ItemEvent.SELECTED || !(item instanceof BallerinaHtmlPanelProvider.ProviderInfo)) {
          return;
        }

        final BallerinaHtmlPanelProvider provider = BallerinaHtmlPanelProvider
                .createFromInfo((BallerinaHtmlPanelProvider.ProviderInfo)item);
        final BallerinaHtmlPanelProvider.AvailabilityInfo availability = provider.isAvailable();

        if (!availability.checkAvailability(myMainPanel)) {
          myPreviewProvider.setSelectedItem(myLastItem);
        }
        else {
          myLastItem = item;
          updateUseGrayscaleEnabled();
        }
      }
    });
  }

  private void updateUseGrayscaleEnabled() {
    final BallerinaHtmlPanelProvider.ProviderInfo selected = getSelectedProvider();
    myUseGrayscaleRenderingForJBCheckBox.setEnabled(isProviderOf(selected, JAVA_FX_HTML_PANEL_PROVIDER));
  }

  private static boolean isProviderOf(@NotNull BallerinaHtmlPanelProvider.ProviderInfo selected, @NotNull String provider) {
    return selected.getClassName().contains(provider);
  }

  @NotNull
  private static BallerinaHtmlPanelProvider getProvider(@SuppressWarnings("SameParameterValue") @NotNull String providerClass) {
    for (BallerinaHtmlPanelProvider provider : BallerinaHtmlPanelProvider.getProviders()) {
      if (isProviderOf(provider.getProviderInfo(), providerClass)) return provider;
    }

    throw new RuntimeException("Cannot find " + providerClass);
  }

  @NotNull
  private BallerinaHtmlPanelProvider.ProviderInfo getSelectedProvider() {
    if (isMultipleProviders()) {
      return Objects.requireNonNull(myPreviewPanelModel.getSelected());
    }
    else {
      return getProvider(JAVA_FX_HTML_PANEL_PROVIDER).getProviderInfo();
    }
  }

  @Override
  public void setMarkdownPreviewSettings(@NotNull MarkdownPreviewSettings settings) {
    if (isMultipleProviders() && myPreviewPanelModel.contains(settings.getHtmlPanelProviderInfo())) {
      myPreviewPanelModel.setSelectedItem(settings.getHtmlPanelProviderInfo());
    }

    mySplitLayoutModel.setSelectedItem(settings.getSplitEditorLayout());
    myUseGrayscaleRenderingForJBCheckBox.setSelected(settings.isUseGrayscaleRendering());
    myAutoScrollCheckBox.setSelected(settings.isAutoScrollPreview());

    updateUseGrayscaleEnabled();
  }

  @NotNull
  @Override
  public MarkdownPreviewSettings getMarkdownPreviewSettings() {
    BallerinaHtmlPanelProvider.ProviderInfo provider = getSelectedProvider();

    Objects.requireNonNull(provider);
    return new MarkdownPreviewSettings(mySplitLayoutModel.getSelectedItem(),
                                       provider,
                                       myUseGrayscaleRenderingForJBCheckBox.isSelected(),
                                       myAutoScrollCheckBox.isSelected());
  }
}
