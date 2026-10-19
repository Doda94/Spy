package app.spijun.ui.design

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp

/**
 * One-line text field: `surface` fill, radius 12, 1 dp `line` border that becomes a 2 dp `text`
 * border on focus and a 2 dp `danger` border when [error] is set. The placeholder vanishes once
 * something is typed, so [label] stays as the field's name for screen readers.
 */
@Composable
fun SpyTextField(
    value: String,
    onValue: (String) -> Unit,
    placeholder: String,
    label: String,
    modifier: Modifier = Modifier,
    error: Boolean = false,
    imeAction: ImeAction = ImeAction.Done,
    onImeAction: () -> Unit = {},
    leading: (@Composable () -> Unit)? = null,
) {
    val c = Spy.colors
    var focused by remember { mutableStateOf(false) }
    val border = when {
        error -> c.danger
        focused -> c.text
        else -> c.line
    }
    BasicTextField(
        value = value,
        onValueChange = onValue,
        singleLine = true,
        textStyle = Spy.type.bodyLarge.copy(color = c.text),
        cursorBrush = SolidColor(c.text),
        keyboardOptions = KeyboardOptions(imeAction = imeAction),
        keyboardActions = KeyboardActions(onAny = { onImeAction() }),
        modifier = modifier
            .fillMaxWidth()
            .onFocusChanged { focused = it.isFocused }
            .semantics { contentDescription = label },
        decorationBox = { inner ->
            androidx.compose.foundation.layout.Row(
                Modifier.fillMaxWidth().height(52.dp).clip(androidx.compose.foundation.shape.RoundedCornerShape(12.dp))
                    .background(c.surface)
                    .border(if (focused || error) 2.dp else 1.dp, border, androidx.compose.foundation.shape.RoundedCornerShape(12.dp))
                    .padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(10.dp),
            ) {
                leading?.invoke()
                Box(Modifier.weight(1f), contentAlignment = Alignment.CenterStart) {
                    if (value.isEmpty()) Text(placeholder, style = Spy.type.bodyLarge, color = c.textMuted)
                    inner()
                }
            }
        },
    )
}
