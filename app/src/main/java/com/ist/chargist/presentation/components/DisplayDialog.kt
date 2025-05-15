package com.ist.chargist.presentation.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.ist.chargist.R
import com.ist.chargist.ui.theme.DialogBackground
import com.ist.chargist.ui.theme.ISTBlue
import com.ist.chargist.ui.theme.ImageDialog
import com.ist.chargist.ui.theme.TextColor

@Composable
fun AddChargerDialog(
    navigateBack: () -> Unit,
) {
    var showDialog by remember { mutableStateOf(false) }

    Dialog(onDismissRequest = {
        showDialog = false
        navigateBack()
    }) {
        Surface(
            color = DialogBackground,
            modifier = Modifier
                .fillMaxWidth()
                .border(width = 1.dp, color = Color.White, shape = RoundedCornerShape(14.dp))
                .clip(RoundedCornerShape(14.dp))
        ) {
            Column(
                modifier = Modifier
                    .padding(16.dp),
                horizontalAlignment = Alignment.Start
            ) {
                Image(
                    painter = painterResource(ImageDialog),
                    contentDescription = null,
                )
                Spacer(modifier = Modifier.height(14.dp))
                Text(
                    text = stringResource(R.string.new_collection_dialog_title),
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextColor
                )
                Text(
                    text = stringResource(R.string.new_location_dialog_subtitle),
                    fontSize = 14.sp,
                    color = TextColor
                )
                Spacer(modifier = Modifier.height(16.dp))
                Button(
                    onClick = {
                        showDialog = false
                        navigateBack()
                    },
                    modifier = Modifier
                        .fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors()
                        .copy(containerColor = ISTBlue),
                ) {
                    Text(
                        text = "Close",
                        color = Color.White
                    )
                }
            }
        }
    }
}