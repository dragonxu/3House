package treehou.se.habit.ui.sitemaps.page;


import dagger.Subcomponent;
import treehou.se.habit.module.ActivityScope;
import treehou.se.habit.module.FragmentComponent;
import treehou.se.habit.module.FragmentComponentBuilder;

@ActivityScope
@Subcomponent(
        modules = PageModule.class
)
public interface PageComponent extends FragmentComponent<PageFragment> {

    @Subcomponent.Builder
    interface Builder extends FragmentComponentBuilder<PageModule, PageComponent> {

    }
}