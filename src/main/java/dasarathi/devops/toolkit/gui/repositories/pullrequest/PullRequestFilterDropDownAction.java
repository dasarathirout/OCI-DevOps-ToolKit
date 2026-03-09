package dasarathi.devops.toolkit.gui.repositories.pullrequest;

import com.intellij.openapi.actionSystem.ActionUpdateThread;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.actionSystem.DefaultActionGroup;
import com.intellij.openapi.actionSystem.Separator;
import com.intellij.openapi.actionSystem.ex.ComboBoxAction;
import com.intellij.openapi.project.DumbAware;
import com.intellij.openapi.project.DumbAwareAction;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Supplier;
import javax.swing.JComponent;
import org.jetbrains.annotations.NotNull;

public final class PullRequestFilterDropDownAction extends ComboBoxAction implements DumbAware {
  private final String label;
  private final String defaultSelectionText;
  private final Supplier<List<String>> optionProvider;
  private final Consumer<String> onSelectionChanged;
  private final Map<String, AnAction> optionActions = new HashMap<>();
  private final AnAction resetAction;
  private final Separator separator = Separator.create();
  private String currentText;

  public PullRequestFilterDropDownAction(
      @NotNull String label,
      @NotNull String defaultSelectionText,
      @NotNull Supplier<List<String>> optionProvider,
      @NotNull Consumer<String> onSelectionChanged) {
    this.label = Objects.requireNonNull(label, "label");
    this.defaultSelectionText =
        Objects.requireNonNull(defaultSelectionText, "defaultSelectionText");
    this.currentText = defaultSelectionText;
    this.optionProvider = Objects.requireNonNull(optionProvider, "optionProvider");
    this.onSelectionChanged = Objects.requireNonNull(onSelectionChanged, "onSelectionChanged");
    getTemplatePresentation().setDescription("FilterBy: " + label);
    this.resetAction =
        new DumbAwareAction("Reset " + label) {
          @Override
          public void actionPerformed(@NotNull AnActionEvent e) {
            updateSelection(PullRequestFilterDropDownAction.this.defaultSelectionText);
          }
        };
  }

  @Override
  protected @NotNull DefaultActionGroup createPopupActionGroup(
      @NotNull JComponent button, @NotNull DataContext dataContext) {
    DefaultActionGroup group = new DefaultActionGroup();
    List<String> options = optionProvider.get();
    Set<String> optionSet = options == null ? Set.of() : new LinkedHashSet<>(options);
    optionActions.keySet().removeIf(existing -> !optionSet.contains(existing));

    for (String option : optionSet) {
      group.add(optionActions.computeIfAbsent(option, this::createFilterItem));
    }
    group.add(separator);
    group.add(resetAction);
    return group;
  }

  @Override
  public @NotNull ActionUpdateThread getActionUpdateThread() {
    return ActionUpdateThread.EDT;
  }

  private AnAction createFilterItem(String name) {
    return new DumbAwareAction(name) {
      @Override
      public void actionPerformed(@NotNull AnActionEvent e) {
        updateSelection(name);
      }
    };
  }

  private void updateSelection(String newText) {
    this.currentText = newText;
    onSelectionChanged.accept(newText);
  }

  @Override
  public void update(@NotNull AnActionEvent e) {
    e.getPresentation().setText(label + " ▾ " + currentText);
  }
}
