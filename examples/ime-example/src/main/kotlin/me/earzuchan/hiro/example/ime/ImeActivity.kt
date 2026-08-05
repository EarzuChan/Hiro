package me.earzuchan.hiro.example.ime

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.input.TextFieldLineLimits
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.foundation.text.input.setTextAndPlaceCursorAtEnd
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.FocusManager
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.PlatformImeOptions
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import me.earzuchan.hiro.compose.setHiroComposeContent

class ImeActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        enableEdgeToEdge()
        setHiroComposeContent {
            MaterialTheme(lightColorScheme(primary = Color(0xFF006C4C), secondary = Color(0xFF4E6358), tertiary = Color(0xFF3D6374))) {
                ImeExample()
            }
        }
    }
}

@OptIn(ExperimentalComposeUiApi::class, ExperimentalMaterial3Api::class)
@Composable
private fun ImeExample() {
    val keyboard = LocalSoftwareKeyboardController.current
    val focusManager = LocalFocusManager.current
    val modernState = rememberTextFieldState("新状态文本")
    var legacyValue by remember { mutableStateOf(TextFieldValue("旧状态文本")) }
    var multiline by remember { mutableStateOf("第一行\n第二行") }
    var password by remember { mutableStateOf("") }
    var lastAction by remember { mutableStateOf("尚未触发") }

    Column(
        Modifier.fillMaxSize()
            .imePadding()
            .verticalScroll(rememberScrollState())
            .safeDrawingPadding()
            .padding(horizontal = 20.dp, vertical = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Column(Modifier.fillMaxWidth().widthIn(max = 720.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("IME 实机检查台", style = MaterialTheme.typography.headlineSmall)
            Text("最近动作：$lastAction", color = MaterialTheme.colorScheme.secondary)

            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = { keyboard?.show() }, modifier = Modifier.weight(1f)) { Text("显示") }
                Button(onClick = { keyboard?.hide() }, modifier = Modifier.weight(1f)) { Text("隐藏") }
                Button(onClick = { focusManager.clearFocus() }, modifier = Modifier.weight(1f)) { Text("清除焦点") }
            }

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
                TextButton(onClick = { modernState.setTextAndPlaceCursorAtEnd("程序改值 ${modernState.text.length}") }) {
                    Text("程序改写新状态")
                }
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

            KeyboardTypeFields()
            ImeActionFields(focusManager = focusManager, onAction = { lastAction = it })
        }
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
            PlatformImeOptions("hiro.example.ascii=true"),
        )
    }
}

@Composable
private fun KeyboardTypeField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    keyboardType: KeyboardType,
    platformImeOptions: PlatformImeOptions? = null,
) {
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
        singleLine = true,
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
            singleLine = true,
        )
        OutlinedTextField(
            value = send,
            onValueChange = { send = it },
            modifier = Modifier.fillMaxWidth().focusRequester(sendFocus),
            label = { Text("Send") },
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
            keyboardActions = KeyboardActions(onSend = { doneFocus.requestFocus(); onAction("Send") }),
            singleLine = true,
        )
        OutlinedTextField(
            value = done,
            onValueChange = { done = it },
            modifier = Modifier.fillMaxWidth().focusRequester(doneFocus),
            label = { Text("Done") },
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus(); onAction("Done") }),
            singleLine = true,
        )
    }
}
