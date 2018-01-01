package treehou.se.habit.ui.servers.create.myopenhab


import dagger.Subcomponent
import treehou.se.habit.module.ActivityScope
import treehou.se.habit.module.FragmentComponent
import treehou.se.habit.module.FragmentComponentBuilder
import treehou.se.habit.ui.settings.SettingsFragment

@ActivityScope
@Subcomponent(modules = arrayOf(CreateMyOpenhabModule::class))
interface CreateMyOpenhabComponent : FragmentComponent<CreateMyOpenhabFragment> {

    @Subcomponent.Builder
    interface Builder : FragmentComponentBuilder<CreateMyOpenhabModule, CreateMyOpenhabComponent>
}