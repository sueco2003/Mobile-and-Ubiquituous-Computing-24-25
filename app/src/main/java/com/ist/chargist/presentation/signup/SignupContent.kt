package com.ist.chargist.presentation.signup

import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ist.chargist.R
import com.ist.chargist.presentation.components.CustomLottieAnimation
import com.ist.chargist.ui.theme.AssetChipColor
import com.ist.chargist.ui.theme.ChargISTTheme
import com.ist.chargist.ui.theme.ISTBlue
import com.ist.chargist.ui.theme.LoginContainerColor
import com.ist.chargist.ui.theme.TextColor
import com.ist.chargist.utils.UiState

@Composable
fun SignupContent(
    signUpUserUiState: UiState,
    onButtonClicked: (email: String, password: String) -> Unit,
    goToLogin: () -> Unit
) {
    val focusManager = LocalFocusManager.current
    val scrollState = rememberScrollState()
    val interactionSource = remember { MutableInteractionSource() }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = Modifier
            .fillMaxWidth()
            .fillMaxHeight()
            .clip(RoundedCornerShape(topStart = 30.dp, topEnd = 30.dp))
            .padding(26.dp)
            .clickable(
                interactionSource = interactionSource,
                indication = null
            ) { focusManager.clearFocus() }
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.verticalScroll(state = scrollState)
        ) {
            CustomLottieAnimation(
                modifier = Modifier.size(200.dp),
                lottie = R.raw.register
            )
            Spacer(modifier = Modifier.padding(10.dp))
            Text(
                text = stringResource(R.string.signup_title),
                fontWeight = FontWeight.SemiBold,
                fontSize = 28.sp,
                color = TextColor
            )
            Spacer(modifier = Modifier.padding(10.dp))
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                SignUpFormContent(
                    onButtonClicked,
                    signUpUserUiState,
                    goToLogin
                )
            }
        }
    }
}

@Composable
private fun SignUpFormContent(
    onButtonClicked: (email: String, password: String) -> Unit,
    signUpUserUiState: UiState,
    goToLogin: () -> Unit
) {
    val context = LocalContext.current

    var emailValue by remember { mutableStateOf("") }
    var passwordValue by remember { mutableStateOf("") }
    var passwordConfirmValue by remember { mutableStateOf("") }
    var passwordVisibility by remember { mutableStateOf(false) }
    var loading by remember { mutableStateOf(false) }

    OutlinedTextField(
        value = emailValue,
        onValueChange = {
            emailValue = it
        },
        colors = OutlinedTextFieldDefaults.colors(
            unfocusedContainerColor = AssetChipColor,
            focusedContainerColor = AssetChipColor
        ),
        leadingIcon = {
            Icon(
                imageVector = Icons.Default.Person,
                contentDescription = stringResource(R.string.login_icon_person_description)
            )
        },
        label = {
            Text(
                text = stringResource(R.string.signup_email_label),
            )
        },
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp)
    )
    Spacer(modifier = Modifier.padding(5.dp))
    OutlinedTextField(
        value = passwordValue,
        onValueChange = {
            passwordValue = it
        },
        colors = OutlinedTextFieldDefaults.colors(
            unfocusedContainerColor = AssetChipColor,
            focusedContainerColor = AssetChipColor

        ),
        leadingIcon = {
            Icon(
                imageVector = Icons.Default.Key,
                contentDescription = stringResource(R.string.login_password_icon_description)
            )
        },
        trailingIcon = {
            IconButton(onClick = {
                passwordVisibility = !passwordVisibility
            }) {
                Icon(
                    if (passwordVisibility) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                    contentDescription = stringResource(R.string.signup_password_label_content_description),
                )
            }
        },
        visualTransformation =
            if (passwordVisibility) VisualTransformation.None
            else PasswordVisualTransformation(),
        label = {
            Text(
                text = stringResource(R.string.signup_password_label),
            )
        },
        singleLine = true,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp)
    )
    Spacer(modifier = Modifier.padding(5.dp))
    OutlinedTextField(
        value = passwordConfirmValue,
        onValueChange = {
            passwordConfirmValue = it
        },
        colors = OutlinedTextFieldDefaults.colors(
            unfocusedContainerColor = AssetChipColor,
            focusedContainerColor = AssetChipColor
        ),
        leadingIcon = {
            Icon(
                imageVector = Icons.Default.Key,
                contentDescription = stringResource(R.string.login_password_icon_description)
            )
        },
        trailingIcon = {
            IconButton(onClick = {
                passwordVisibility = !passwordVisibility
            }) {
                Icon(
                    if (passwordVisibility) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                    contentDescription = stringResource(id = R.string.signup_password_label_content_description),
                )
            }
        },
        visualTransformation = if (passwordVisibility) VisualTransformation.None
        else PasswordVisualTransformation(),
        label = {
            Text(
                text = stringResource(R.string.signup_password_confirm_label),
            )
        },
        singleLine = true,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp)
    )
    Spacer(modifier = Modifier.padding(10.dp))
    Button(
        onClick = {
            if (passwordValue != passwordConfirmValue) {
                Toast.makeText(
                    context,
                    context.getString(R.string.signup_error_passwords_dont_match),
                    Toast.LENGTH_LONG
                ).show()

            } else {
                onButtonClicked(emailValue, passwordValue)
            }
        },
        enabled = !loading,
        modifier = Modifier
            .fillMaxWidth()
            .height(50.dp),
        colors = ButtonDefaults.buttonColors()
            .copy(containerColor = ISTBlue, contentColor = Color.White),
        shape = RoundedCornerShape(8.dp)
    ) {
        when (signUpUserUiState) {
            is UiState.Loading -> {
                loading = true
                CircularProgressIndicator(color = Color.White)
            }

            else -> {
                loading = false
                Text(
                    text = stringResource(R.string.signup_btn_text),
                    fontSize = 20.sp
                )
            }
        }
    }
    Spacer(modifier = Modifier.padding(15.dp))
    Row {
        Text(
            text = stringResource(R.string.signup_btn_back_label),
            fontSize = MaterialTheme.typography.bodyMedium.fontSize,
            fontWeight = FontWeight.Bold,
            color = TextColor,
            modifier = Modifier.clickable {
                goToLogin()
            }
        )
    }
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
fun SignupContentPreview() {
    ChargISTTheme {
        SignupContent(
            signUpUserUiState = UiState.Idle,
            onButtonClicked = { _, _ -> }) {
        }
    }
}

@Preview(uiMode = android.content.res.Configuration.UI_MODE_NIGHT_YES)
@Composable
fun SignupContentDarkPreview() {
    ChargISTTheme {
        SignupContent(
            signUpUserUiState = UiState.Idle,
            onButtonClicked = { _, _ -> }) {
        }
    }
}