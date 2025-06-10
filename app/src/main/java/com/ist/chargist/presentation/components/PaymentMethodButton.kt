package com.ist.chargist.presentation.components

import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.ist.chargist.ui.theme.ISTBlue

@Composable
fun PaymentMethodButton(label: String, isSelected: Boolean, onClick: () -> Unit) {
    val backgroundColor = if (isSelected) ISTBlue else Color(0XFFF0F0F0)
    val contentColor = if (isSelected) Color.White else Color.Black

    Button(
        onClick = onClick,
        colors = ButtonDefaults.buttonColors(
            containerColor = backgroundColor,
            contentColor = contentColor
        ),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.padding(horizontal = 10.dp)
    ) {
        Text(text = label)
    }
}