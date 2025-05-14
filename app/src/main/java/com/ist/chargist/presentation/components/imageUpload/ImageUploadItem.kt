/*
package com.ist.chargist.presentation.components.imageUpload

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImagePainter
import coil.compose.SubcomposeAsyncImage
import coil.compose.SubcomposeAsyncImageContent
import coil.request.ImageRequest
import com.hocel.assetmanager.R
import com.hocel.assetmanager.presentation.components.dashedBorder
import com.hocel.assetmanager.ui.theme.AssetManagerTheme
import com.hocel.assetmanager.ui.theme.imageAdd
import com.hocel.assetmanager.ui.theme.imageAddCenterIcon
import com.hocel.assetmanager.ui.theme.imageAddCloudIcon
import com.hocel.assetmanager.ui.theme.imageAddFont
import com.hocel.assetmanager.utils.createImageUri
import com.ist.chargist.ui.theme.imageAdd

@Composable
fun DocumentUploadItem(
    imageUriSaved: Uri?,
    onClick: (() -> Unit, () -> Unit) -> Unit,
    onImageChosen: (Uri) -> Unit,

) {
    val context = LocalContext.current

    val cameraImageUri = remember { mutableStateOf<Uri?>(null) }
    var imageUri by remember { mutableStateOf<Uri?>(imageUriSaved) }

    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            imageUri = it
            onImageChosen(it)
        }
    }
    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) {
        if (it) {
            cameraImageUri.value?.let { path ->
                imageUri = path
                onImageChosen(path)
            }
        }
    }

    Column(
        verticalArrangement = Arrangement.spacedBy(5.dp)
    ) {
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(12.dp))
                .background(imageAdd)
                .height(150.dp)
                .fillMaxWidth()
                .dashedBorder(
                    1.5.dp,
                    imageAdd,
                    12.dp
                )
                .clickable {
                    onClick({ galleryLauncher.launch("image/*") }) {
                        cameraImageUri.value = createImageUri(context = context)
                        cameraImageUri.value?.let { cameraLauncher.launch(it) }
                    }
                },
            contentAlignment = Alignment.Center,
        ) {
            if (imageUri != null) {
                SubcomposeAsyncImage(
                    modifier = Modifier
                        .padding(5.dp)
                        .fillMaxSize(),
                    model = ImageRequest.Builder(LocalContext.current).data(imageUri)
                        .crossfade(true).build(),
                    contentDescription = "Image"
                ) {
                    when (painter.state) {
                        is AsyncImagePainter.State.Loading -> {
                            Box(
                                Modifier.fillMaxSize(), contentAlignment = Alignment.Center
                            ) {
                                CircularProgressIndicator()
                            }
                        }

                        else -> {
                            SubcomposeAsyncImageContent(
                                modifier = Modifier
                                    .clip(RectangleShape)
                                    .fillMaxSize(),
                                contentScale = ContentScale.FillBounds
                            )
                        }
                    }
                }
            } else {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Icon(
                        modifier = Modifier.size(50.dp),
                        painter = painterResource(R.drawable.ic_upload_image),
                        contentDescription = null,
                        tint = imageAddCenterIcon,
                    )
                    Row {
                        Icon(
                            modifier = Modifier
                                .size(15.dp)
                                .align(Alignment.CenterVertically),
                            painter = painterResource(R.drawable.ic_upload_cloud),
                            contentDescription = null,
                            tint = imageAddCloudIcon
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = stringResource(R.string.upload_image),
                            fontSize = 12.sp,
                            color = imageAddFont
                        )
                    }
                }
            }
        }
    }
}
*/
*/