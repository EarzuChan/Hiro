package me.earzuchan.hiro.example.configuration

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
internal fun ConfigurationControlPanel(onConfigurationChange: (ConfigurationState) -> Unit) = Surface(color = MaterialTheme.colorScheme.background) {
    var state by remember { mutableStateOf(ConfigurationState()) }

    fun commit(next: ConfigurationState) {
        state = next
        onConfigurationChange(next)
    }

    Column(Modifier.verticalScroll(rememberScrollState()).padding(16.dp), Arrangement.spacedBy(10.dp)) {
        Text("Hiro 配置策略控制台", style = MaterialTheme.typography.titleLarge)
        Text("解析规则：固定值直接生效；跟随策略读取所选基线；变换策略在系统值上应用用户变换")
        Text("ViewConfiguration 的 Android 基线会以 Compose 缺省补齐，再应用用户补丁")
        Text("本面板每次调节都会用当前选择重建预览 HiroComposeView")

        HorizontalDivider()

        GenericPolicySection(
            title = "Density",
            selected = state.densityPolicy,
            current = state.densityPolicy.label(),
            choices = genericPolicyChoices(),
            onSelected = { commit(state.copy(densityPolicy = it)) },
        )
        Slider(
            value = state.densityValue,
            onValueChange = { commit(state.copy(densityValue = it)) },
            valueRange = 0.5f..3f,
            steps = 49,
        )
        Text("固定值或变换倍率：${state.densityValue.formatValue()}")

        GenericPolicySection(
            title = "FontScale",
            selected = state.fontScalePolicy,
            current = state.fontScalePolicy.label(),
            choices = genericPolicyChoices(),
            onSelected = { commit(state.copy(fontScalePolicy = it)) },
        )
        Slider(
            value = state.fontScaleValue,
            onValueChange = { commit(state.copy(fontScaleValue = it)) },
            valueRange = 0.5f..2f,
            steps = 29,
        )
        Text("固定值或变换倍率：${state.fontScaleValue.formatValue()}")

        GenericPolicySection(
            title = "LayoutDirection",
            selected = state.layoutDirectionPolicy,
            current = state.layoutDirectionPolicy.label(),
            choices = genericPolicyChoices(),
            onSelected = { commit(state.copy(layoutDirectionPolicy = it)) },
        )
        BooleanChoiceRow(
            title = "固定值",
            selected = state.layoutDirectionRtl,
            choices = listOf(false to "LTR", true to "RTL"),
            onSelected = { commit(state.copy(layoutDirectionRtl = it)) },
        )

        GenericPolicySection(
            title = "SystemTheme",
            selected = state.systemThemePolicy,
            current = state.systemThemePolicy.label(),
            choices = genericPolicyChoices(),
            onSelected = { commit(state.copy(systemThemePolicy = it)) },
        )
        BooleanChoiceRow(
            title = "固定值",
            selected = state.systemThemeDark,
            choices = listOf(false to "Light", true to "Dark"),
            onSelected = { commit(state.copy(systemThemeDark = it)) },
        )

        GenericPolicySection(
            title = "Locale",
            selected = state.localePolicy,
            current = state.localePolicy.label(),
            choices = genericPolicyChoices(),
            onSelected = { commit(state.copy(localePolicy = it)) },
        )
        BooleanChoiceRow(
            title = "固定值或变换目标",
            selected = state.localeChinese,
            choices = listOf(false to "en-US", true to "zh-CN"),
            onSelected = { commit(state.copy(localeChinese = it)) },
        )

        InsetsPolicySection(
            state = state,
            onPolicySelected = { commit(state.copy(insetsPolicy = it)) },
            onTopChanged = { commit(state.copy(insetTop = it)) },
            onBottomChanged = { commit(state.copy(insetBottom = it)) },
        )

        ViewConfigurationSection(
            state = state,
            onPolicySelected = { commit(state.copy(viewConfigurationPolicy = it)) },
            onTouchSlopChanged = { commit(state.copy(touchSlop = it)) },
        )
    }
}

@Composable
private fun GenericPolicySection(title: String, selected: GenericPolicyChoice, current: String, choices: List<Pair<GenericPolicyChoice, String>>, onSelected: (GenericPolicyChoice) -> Unit) {
    Text("$title · 当前：$current", style = MaterialTheme.typography.titleMedium)
    PolicyRadioRow(choices = choices, selected = selected, onSelected = onSelected)
}

@Composable
private fun InsetsPolicySection(state: ConfigurationState, onPolicySelected: (InsetsPolicyChoice) -> Unit, onTopChanged: (Int) -> Unit, onBottomChanged: (Int) -> Unit) {
    Text("WindowInsets · 当前：${state.insetsPolicy.label()}", style = MaterialTheme.typography.titleMedium)
    PolicyRadioRow(
        choices = listOf(
            InsetsPolicyChoice.FollowSystem to InsetsPolicyChoice.FollowSystem.label(),
            InsetsPolicyChoice.Fixed to InsetsPolicyChoice.Fixed.label(),
            InsetsPolicyChoice.TransformSystem to InsetsPolicyChoice.TransformSystem.label(),
        ),
        selected = state.insetsPolicy,
        onSelected = onPolicySelected,
    )
    Slider(value = state.insetTop.toFloat(), onValueChange = { onTopChanged(it.toInt()) }, valueRange = 0f..200f, steps = 39)
    Text("顶部初值或变换增量：${state.insetTop}px")
    Slider(value = state.insetBottom.toFloat(), onValueChange = { onBottomChanged(it.toInt()) }, valueRange = 0f..200f, steps = 39)
    Text("底部初值或变换增量：${state.insetBottom}px")
}

@Composable
private fun ViewConfigurationSection(state: ConfigurationState, onPolicySelected: (ViewConfigurationPolicyChoice) -> Unit, onTouchSlopChanged: (Float) -> Unit) {
    Text("ViewConfiguration · 当前：${state.viewConfigurationPolicy.label()}", style = MaterialTheme.typography.titleMedium)
    PolicyRadioRow(
        choices = listOf(
            ViewConfigurationPolicyChoice.FollowSystem to ViewConfigurationPolicyChoice.FollowSystem.label(),
            ViewConfigurationPolicyChoice.FollowCompose to ViewConfigurationPolicyChoice.FollowCompose.label(),
            ViewConfigurationPolicyChoice.Fixed to ViewConfigurationPolicyChoice.Fixed.label(),
            ViewConfigurationPolicyChoice.TransformSystem to ViewConfigurationPolicyChoice.TransformSystem.label(),
        ),
        selected = state.viewConfigurationPolicy,
        onSelected = onPolicySelected,
    )
    Slider(value = state.touchSlop, onValueChange = onTouchSlopChanged, valueRange = 0f..48f, steps = 47)
    Text("touchSlop 参数：${state.touchSlop.formatValue()}px；补丁策略按当前 Density 将 dp 转换为像素")
}

@Composable
private fun <T> PolicyRadioRow(choices: List<Pair<T, String>>, selected: T, onSelected: (T) -> Unit) = Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
    choices.forEach { (value, label) ->
        Row(Modifier.fillMaxWidth().clickable { onSelected(value) }, verticalAlignment = Alignment.CenterVertically) {
            RadioButton(selected = value == selected, onClick = { onSelected(value) })
            Text(label)
        }
    }
}

@Composable
private fun <T> BooleanChoiceRow(title: String, selected: T, choices: List<Pair<T, String>>, onSelected: (T) -> Unit) {
    Text(title)
    PolicyRadioRow(choices = choices, selected = selected, onSelected = onSelected)
}

private fun genericPolicyChoices() = listOf(
    GenericPolicyChoice.FollowSystem to GenericPolicyChoice.FollowSystem.label(),
    GenericPolicyChoice.Fixed to GenericPolicyChoice.Fixed.label(),
    GenericPolicyChoice.TransformSystem to GenericPolicyChoice.TransformSystem.label(),
)
