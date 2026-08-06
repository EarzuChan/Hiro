package me.earzuchan.hiro.example.ime

import android.os.Bundle
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.widget.EditText
import android.widget.LinearLayout
import androidx.activity.ComponentActivity
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.input.TextFieldLineLimits
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.foundation.text.input.setTextAndPlaceCursorAtEnd
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.focus.FocusManager
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.input.*
import androidx.compose.ui.unit.dp
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import me.earzuchan.hiro.compose.HiroComposeView
import kotlin.math.roundToInt

class ImeActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val spacing = (12f * resources.displayMetrics.density).roundToInt()

        setContentView(LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(spacing, spacing, spacing, spacing)

            ViewCompat.setOnApplyWindowInsetsListener(this) { view, insets ->
                val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())

                view.setPadding(spacing, spacing + systemBars.top, spacing, spacing + systemBars.bottom)
                insets
            }

            // 三个啊三个
            addView(EditText(this@ImeActivity).apply {
                hint = "原生 EditText"
                isSingleLine = true
            }, LinearLayout.LayoutParams(MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT))

            addView(HiroComposeView(this@ImeActivity).apply {
                setContent { UiWrapper { ImeExample() } }
            }, LinearLayout.LayoutParams(MATCH_PARENT, 0, 1f).apply { topMargin = spacing })

            addView(HiroComposeView(this@ImeActivity).apply {
                rotation = 1.5f
                scaleX = 0.97f
                scaleY = 0.97f
                setContent { UiWrapper { TransformedSiblingEditor() } }
            }, LinearLayout.LayoutParams(MATCH_PARENT, (132f * resources.displayMetrics.density).roundToInt()).apply { topMargin = spacing })
        })
    }
}

@Composable
fun UiWrapper(content: @Composable () -> Unit) = MaterialTheme(if (isSystemInDarkTheme()) darkColorScheme() else lightColorScheme()) {
    Surface(content = content)
}

@Composable
private fun ImeExample() = Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp, vertical = 16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
    val keyboard = LocalSoftwareKeyboardController.current
    val focusManager = LocalFocusManager.current
    val modernState = rememberTextFieldState("新状态文本")
    var legacyValue by remember { mutableStateOf(TextFieldValue("旧状态文本")) }
    var multiline by remember { mutableStateOf("第一行\n第二行") }
    var password by remember { mutableStateOf("") }
    var guardedValue by remember { mutableStateOf(TextFieldValue("123")) }
    var rejectedEdits by remember { mutableStateOf(0) }
    var clipboardValue by remember { mutableStateOf(TextFieldValue("可复制与粘贴的文本")) }
    var lastAction by remember { mutableStateOf("尚未触发") }

    @Suppress("DEPRECATION")
    val clipboard = LocalClipboardManager.current

    Column(Modifier.fillMaxWidth().widthIn(max = 720.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("IME 实机检查台", style = MaterialTheme.typography.headlineSmall)

        Button({ focusManager.clearFocus() }) { Text("清除焦点") } // 仅清除本HiroComposeViewPort的焦点

        ExampleSection("状态与组合文本") {
            OutlinedTextField(
                state = modernState,
                modifier = Modifier.fillMaxWidth(),
                label = { Text("新 TextFieldState") },
                supportingText = { Text("长度 ${modernState.text.length}，选择区 ${modernState.selection}") },
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                onKeyboardAction = { lastAction = "新状态 Done" },
                lineLimits = TextFieldLineLimits.SingleLine,
            )

            TextButton({ modernState.setTextAndPlaceCursorAtEnd("程序改值 ${modernState.text.length}") }) { Text("程序改写新状态") }

            OutlinedTextField(
                value = legacyValue,
                onValueChange = { legacyValue = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("旧 TextFieldValue") },
                supportingText = { Text("选择区 ${legacyValue.selection}，组合区 ${legacyValue.composition ?: "无"}") },
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = { lastAction = "旧状态 Done" }),
                singleLine = true,
            )
        }

        ExampleSection("布局与敏感输入") {
            OutlinedTextField(
                value = multiline,
                onValueChange = { multiline = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("多行文本") },
                minLines = 3,
                maxLines = 5,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Default),
            )

            OutlinedTextField(
                value = password,
                onValueChange = { password = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("密码") },
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = { keyboard?.hide(); lastAction = "密码 Done" }),
                singleLine = true,
            )
        }

        ExampleSection("受控编辑与剪贴板") {
            OutlinedTextField(
                value = guardedValue,
                onValueChange = { next ->
                    if (next.text.all(Char::isDigit)) guardedValue = next else rejectedEdits++
                },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("仅接受数字") },
                supportingText = { Text("已拒绝 $rejectedEdits 次") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true,
            )

            OutlinedTextField(
                value = clipboardValue,
                onValueChange = { clipboardValue = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("剪贴板文本") },
                singleLine = true,
            )

            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button({ clipboard.setText(AnnotatedString(clipboardValue.text)) }, Modifier.weight(1f)) { Text("复制") }

                Button({
                    clipboard.setText(AnnotatedString(clipboardValue.text))
                    clipboardValue = TextFieldValue()
                }, Modifier.weight(1f)) { Text("剪切") }

                Button({ clipboard.getText()?.let { clipboardValue = TextFieldValue(it.text) } }, Modifier.weight(1f)) { Text("粘贴") }
            }
        }

        KeyboardTypeFields()

        Text("最近动作：$lastAction")
        ImeActionFields(focusManager = focusManager, onAction = { lastAction = it })
    }
}

@Composable
private fun TransformedSiblingEditor() = Column(Modifier.fillMaxSize().padding(horizontal = 16.dp, vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
    var value by remember { mutableStateOf(TextFieldValue("变换与裁剪锚点")) }

    Text("兄弟 HiroComposeView", style = MaterialTheme.typography.titleSmall)
    Box(Modifier.fillMaxWidth().height(82.dp).clipToBounds()) {
        OutlinedTextField(
            value = value,
            onValueChange = { value = it },
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Android View 变换") },
            singleLine = true,
        )
    }
}

@Composable
private fun ExampleSection(title: String, content: @Composable ColumnScope.() -> Unit) {
    HorizontalDivider(Modifier.padding(top = 8.dp))
    Text(title, style = MaterialTheme.typography.titleMedium)
    Column(verticalArrangement = Arrangement.spacedBy(8.dp), content = content)
}

@Composable
private fun KeyboardTypeFields() {
    var number by remember { mutableStateOf("") }
    var decimal by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var uri by remember { mutableStateOf("") }
    var ascii by remember { mutableStateOf("") }

    ExampleSection("键盘类型") {
        KeyboardTypeField("数字", number, { number = it }, KeyboardType.Number)
        KeyboardTypeField("小数", decimal, { decimal = it }, KeyboardType.Decimal)
        KeyboardTypeField("电话", phone, { phone = it }, KeyboardType.Phone)
        KeyboardTypeField("邮箱", email, { email = it }, KeyboardType.Email)
        KeyboardTypeField("网址", uri, { uri = it }, KeyboardType.Uri)
        KeyboardTypeField(
            "仅 ASCII",
            ascii,
            { ascii = it },
            KeyboardType.Ascii,
            PlatformImeOptions("啊一个ASCII"),
        )
    }
}

@Composable
private fun KeyboardTypeField(label: String, value: String, onValueChange: (String) -> Unit, keyboardType: KeyboardType, platformImeOptions: PlatformImeOptions? = null) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = Modifier.fillMaxWidth(),
        label = { Text(label) },
        keyboardOptions = KeyboardOptions(
            keyboardType = keyboardType,
            imeAction = ImeAction.Next,
            platformImeOptions = platformImeOptions,
        ),
        singleLine = true
    )
}

@Composable
private fun ImeActionFields(focusManager: FocusManager, onAction: (String) -> Unit) {
    var next by remember { mutableStateOf("") }
    var search by remember { mutableStateOf("") }
    var send by remember { mutableStateOf("") }
    var done by remember { mutableStateOf("") }
    val searchFocus = remember { FocusRequester() }
    val sendFocus = remember { FocusRequester() }
    val doneFocus = remember { FocusRequester() }

    ExampleSection("IME 动作") {
        OutlinedTextField(
            value = next,
            onValueChange = { next = it },
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Next") },
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
            keyboardActions = KeyboardActions(onNext = { searchFocus.requestFocus(); onAction("Next") }),
            singleLine = true,
        )

        OutlinedTextField(
            value = search,
            onValueChange = { search = it },
            modifier = Modifier.fillMaxWidth().focusRequester(searchFocus),
            label = { Text("Search") },
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            keyboardActions = KeyboardActions(onSearch = { sendFocus.requestFocus(); onAction("Search") }),
            singleLine = true
        )

        OutlinedTextField(
            value = send,
            onValueChange = { send = it },
            modifier = Modifier.fillMaxWidth().focusRequester(sendFocus),
            label = { Text("Send") },
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
            keyboardActions = KeyboardActions(onSend = { doneFocus.requestFocus(); onAction("Send") }),
            singleLine = true
        )

        OutlinedTextField(
            value = done,
            onValueChange = { done = it },
            modifier = Modifier.fillMaxWidth().focusRequester(doneFocus),
            label = { Text("Done") },
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus(); onAction("Done") }),
            singleLine = true
        )
    }
}
