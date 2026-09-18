package dev.kmpapp.designsystem

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Subscript
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kmpapp.core.designsystem.generated.resources.Res
import kmpapp.core.designsystem.generated.resources.retry_label
import kotlinx.collections.immutable.ImmutableList
import org.jetbrains.compose.resources.stringResource
import dev.kmpapp.common.UiState
import dev.kmpapp.designsystem.toolbar.XModalTopAppBar

@Composable
fun ItemPickerModal(
    title: String,
    itemsState: UiState<ImmutableList<String>>,
    selectedItemIndex: Int?,
    onCloseClick: () -> Unit,
    onRetryLoad: () -> Unit,
    onSubmitClick: (index: Int) -> Unit,
) {
    XDialog(
        onDismissRequest = onCloseClick,
    ) {
        Column(
            modifier =
                Modifier
                    .fillMaxWidth(),
        ) {
            XModalTopAppBar(
                title = { Text(text = title) },
                navigationIcon = {
                    IconButton(onClick = onCloseClick) {
                        Icon(imageVector = Icons.Default.Subscript, contentDescription = null)
                    }
                },
            )

            LazyColumn(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .padding(bottom = 16.dp)
                        .heightIn(max = 450.dp),
                state = rememberLazyListState(initialFirstVisibleItemIndex = selectedItemIndex ?: 0),
            ) {
                when (itemsState) {
                    is UiState.Loading ->
                        item("loading") {
                            Box(
                                modifier =
                                    Modifier
                                        .fillMaxWidth()
                                        .padding(8.dp),
                            ) {
                                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                            }
                        }

                    is UiState.Failed ->
                        item(key = "error") {
                            Placeholder(
                                modifier = Modifier.fillMaxWidth(),
                                title = { Text(text = itemsState.error.asString()) },
                                action = {
                                    XButton(onClick = onRetryLoad) {
                                        Text(text = stringResource(Res.string.retry_label))
                                    }
                                },
                            )
                        }

                    is UiState.Success -> {
                        itemsIndexed(itemsState.value) { index, item ->
                            ItemRow(
                                label = item,
                                isSelected = selectedItemIndex == index,
                                onClick = { onSubmitClick(index) },
                            )
                        }
                    }

                    is UiState.Uninitialized -> Unit
                }
            }
        }
    }
}

@Composable
private fun ItemRow(
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier =
            Modifier
                .then(if (isSelected) Modifier.background(Color.LightGray) else Modifier)
                .fillMaxWidth()
                .clickable(onClick = onClick)
                .padding(vertical = 12.dp, horizontal = 8.dp),
    ) {
        Text(
            text = label,
            maxLines = 1,
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold,
            overflow = TextOverflow.Ellipsis,
        )
    }
}
