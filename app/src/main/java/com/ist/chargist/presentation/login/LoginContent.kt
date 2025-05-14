package com.ist.chargist.presentation.login


import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
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
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hocel.assetmanager.presentation.components.GoogleButton
import com.ist.chargist.R
import com.ist.chargist.presentation.components.CustomLottieAnimation
import com.ist.chargist.presentation.components.HorizontalDividerWithText
import com.ist.chargist.ui.theme.AssetChipColor
import com.ist.chargist.ui.theme.ISTBlue
import com.ist.chargist.ui.theme.TextColor
import com.ist.chargist.utils.UiState

@Composable
fun LoginContent(
    signInUserUiState: UiState,
    googleSignInUserUiState: UiState,
    onButtonClicked: (email: String, password: String) -> Unit,
    onGoogleSignClicked: () -> Unit,
    goToSignup: () -> Unit,
    viewModel: LoginViewModel
) {
    val context = LocalContext.current
    val focusManager = LocalFocusManager.current
    val scrollState = rememberScrollState()
    var googleLoadingState by remember { mutableStateOf(false) }
    val interactionSource = remember { MutableInteractionSource() }

    // Derive loading state from existing UI states
    val isLoading = signInUserUiState is UiState.Loading ||
            googleSignInUserUiState is UiState.Loading

    LaunchedEffect(googleSignInUserUiState) {
        googleLoadingState = googleSignInUserUiState is UiState.Loading
    }

    Column(
        horizontalAlignment = Alignment.Start,
        verticalArrangement = Arrangement.Center,
        modifier = Modifier
            .fillMaxSize()
            .clip(RoundedCornerShape(topStart = 30.dp, topEnd = 30.dp))
            .padding(26.dp)
            .verticalScroll(state = scrollState)
            .clickable(
                interactionSource = interactionSource,
                indication = null
            ) { focusManager.clearFocus() }
    ) {
        TextButton(
            onClick = { viewModel.signInAsGuest() },
            enabled = !isLoading,
            modifier = Modifier
                .width(150.dp)
                .height(40.dp),
            colors = // Custom colors for button background and text
                ButtonDefaults.textButtonColors(
                    containerColor = ISTBlue,  // Button background color
                    contentColor = Color.White
                ),
            shape = RoundedCornerShape(8.dp)
        ) {
            if (signInUserUiState is UiState.Loading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(24.dp)
                )
            } else {
                Text(
                    text = "Continue as guest",
                    fontSize = 14.sp,
                    modifier = Modifier.padding(4.dp)
                )
            }
        }
        Spacer(modifier = Modifier.height(16.dp))

        // Rest of your existing content
        Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Image(
                    painter = painterResource(id = R.drawable.ist_logo),
                    contentDescription = "Logo",
                    modifier = Modifier.size(100.dp)
                )
                Spacer(modifier = Modifier.width(16.dp))
                Text(
                    text = context.getString(R.string.app_name),
                    color = TextColor,
                    fontSize = 36.sp,
                    fontFamily = FontFamily.Cursive
                )
            }
        }
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.weight(weight = 9f)
        ) {
            CustomLottieAnimation(
                modifier = Modifier.size(150.dp),
                lottie = R.raw.login,
            )
            Spacer(modifier = Modifier.padding(10.dp))
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                LoginFormContent(
                    onButtonClicked = onButtonClicked,
                    signInUserUiState = signInUserUiState,
                    goToSignup = goToSignup
                )
            }
        }
        GoogleButton(
            loadingState = googleLoadingState,
            onClick = onGoogleSignClicked
        )
    }
}


@Composable
private fun LoginFormContent(
    onButtonClicked: (email: String, password: String) -> Unit,
    signInUserUiState: UiState,
    goToSignup: () -> Unit
) {

    var emailValue by remember { mutableStateOf("") }
    var passwordValue by remember { mutableStateOf("") }
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
                text = stringResource(id = R.string.signup_email_label),
            )
        },
        singleLine = true,
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
                    imageVector =
                        if (passwordVisibility) Icons.Default.Visibility
                        else Icons.Default.VisibilityOff,
                    contentDescription = stringResource(id = R.string.signup_password_label_content_description),
                )
            }
        },
        visualTransformation =
            if (passwordVisibility) VisualTransformation.None
            else PasswordVisualTransformation(),
        label = {
            Text(
                text = LocalContext.current.getString(R.string.signup_password_label),
            )
        },
        singleLine = true,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp)
    )
    Spacer(modifier = Modifier.padding(10.dp))
    Button(
        onClick = {
            onButtonClicked(emailValue, passwordValue)
        },
        enabled = !loading,
        modifier = Modifier
            .fillMaxWidth()
            .height(50.dp),
        colors = ButtonDefaults.buttonColors()
            .copy(containerColor = ISTBlue, contentColor = Color.White),
        shape = RoundedCornerShape(8.dp)
    ) {
        when (signInUserUiState) {
            is UiState.Loading -> {
                loading = true
                CircularProgressIndicator(color = Color.White)
            }

            else -> {
                loading = false
                Text(
                    text = stringResource(R.string.login_btn_text),
                    fontSize = 20.sp
                )
            }
        }
    }
    Spacer(modifier = Modifier.padding(15.dp))
    HorizontalDividerWithText(text = "Or")
    Spacer(modifier = Modifier.padding(15.dp))
    Row {
        Text(
            text = stringResource(R.string.login_no_account_label),
            color = TextColor,
            fontSize = MaterialTheme.typography.bodyMedium.fontSize
        )
        Spacer(modifier = Modifier.padding(end = 2.dp))
        Text(
            text = LocalContext.current.getString(R.string.login_register_label),
            color = TextColor,
            fontSize = MaterialTheme.typography.bodyMedium.fontSize,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.clickable {
                goToSignup()
            })
    }
}
