package com.example.ui.screens

import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Badge
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.StudentProfile
import com.example.viewmodel.IdScanUiState

private val GRADE_LEVELS_TAMIL = listOf(
    "தொடக்கப் பள்ளி",
    "நடுநிலைப் பள்ளி",
    "உயர்நிலைப் பள்ளி",
    "கல்லூரி",
    "அனைத்து வயது"
)

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun IdVerificationScreen(
    existingProfile: StudentProfile?,
    scanState: IdScanUiState,
    onScanBitmap: (Bitmap) -> Unit,
    onConfirmProfile: (name: String, school: String, idNum: String, grade: String) -> Unit,
    onBack: (() -> Unit)? = null,
    onSignOut: (() -> Unit)? = null
) {
    if (onBack != null) {
        BackHandler(onBack = onBack)
    }

    val context = LocalContext.current
    var previewBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var fullName by remember(existingProfile) { mutableStateOf(existingProfile?.displayName.orEmpty()) }
    var schoolName by remember(existingProfile) { mutableStateOf(existingProfile?.schoolName.orEmpty()) }
    var studentIdNumber by remember(existingProfile) { mutableStateOf(existingProfile?.studentIdNumber.orEmpty()) }
    var gradeLevel by remember(existingProfile) { mutableStateOf(existingProfile?.gradeLevel ?: "உயர்நிலைப் பள்ளி") }
    var aiVerifiedCard by remember { mutableStateOf(existingProfile != null) }
    var aiConfidenceNote by remember { mutableStateOf<String?>(null) }
    var localDecodeError by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(scanState) {
        if (scanState is IdScanUiState.Extracted) {
            val res = scanState.result
            if (res.fullName.isNotBlank()) fullName = res.fullName
            if (res.schoolName.isNotBlank()) schoolName = res.schoolName
            if (res.studentIdNumber.isNotBlank()) studentIdNumber = res.studentIdNumber
            if (res.gradeLevel.isNotBlank()) gradeLevel = res.gradeLevel
            aiVerifiedCard = res.appearsToBeStudentId
            aiConfidenceNote = res.confidenceNote
        }
    }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null) {
            localDecodeError = null
            try {
                val bmp = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                    val source = ImageDecoder.createSource(context.contentResolver, uri)
                    ImageDecoder.decodeBitmap(source) { decoder, _, _ ->
                        decoder.isMutableRequired = true
                    }
                } else {
                    @Suppress("DEPRECATION")
                    MediaStore.Images.Media.getBitmap(context.contentResolver, uri)
                }
                previewBitmap = bmp
                onScanBitmap(bmp)
            } catch (e: Exception) {
                localDecodeError = "தேர்ந்தெடுத்த படத்தைத் திறக்க முடியவில்லை: ${e.localizedMessage ?: "ஆதரிக்கப்படாத கோப்பு"}"
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding()
            .navigationBarsPadding()
            .imePadding(),
        contentAlignment = Alignment.TopCenter
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 580.dp)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    if (onBack != null) {
                        IconButton(
                            onClick = onBack,
                            modifier = Modifier.testTag("id_verify_back_button")
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "பின்செல்க"
                            )
                        }
                    }
                    Column {
                        Text(
                            text = if (existingProfile == null) stringResource(R.string.id_verify_title) else "மாணவர் ID புதுப்பிப்பு",
                            style = MaterialTheme.typography.headlineSmall,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        Text(
                            text = "ஒரு கணக்கிற்கு ஒரு மாணவர் ID • +50 வரவேற்பு புள்ளிகள்",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                if (onSignOut != null && existingProfile == null) {
                    OutlinedButton(
                        onClick = onSignOut,
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                        modifier = Modifier.testTag("id_verify_sign_out_button")
                    ) {
                        Text("மாற்றுக", style = MaterialTheme.typography.labelMedium)
                    }
                }
            }

            Surface(
                color = MaterialTheme.colorScheme.surfaceContainerLow,
                shape = RoundedCornerShape(14.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Filled.Security,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = stringResource(R.string.id_verify_subtitle),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }

            Card(
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    val bmp = previewBitmap
                    if (bmp != null) {
                        Image(
                            bitmap = bmp.asImageBitmap(),
                            contentDescription = "தேர்ந்தெடுத்த மாணவர் அடையாள அட்டை",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(180.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .border(
                                    1.5.dp,
                                    MaterialTheme.colorScheme.primary,
                                    RoundedCornerShape(12.dp)
                                )
                        )
                    } else {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(145.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(MaterialTheme.colorScheme.surfaceContainerLow)
                                .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(12.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(6.dp),
                                modifier = Modifier.padding(12.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Badge,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(38.dp)
                                )
                                Text(
                                    text = "பள்ளி / கல்லூரி ID அட்டையைப் பதிவேற்றுக",
                                    style = MaterialTheme.typography.titleSmall,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "Gemini AI பெயர், பள்ளி மற்றும் ID எண்ணை தானாகப் படிக்கும்",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }

                    OutlinedButton(
                        onClick = {
                            photoPickerLauncher.launch(
                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                            )
                        },
                        enabled = scanState !is IdScanUiState.Scanning && scanState !is IdScanUiState.Saving,
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.2.dp, MaterialTheme.colorScheme.primary),
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 50.dp)
                            .testTag("pick_student_id_photo_button")
                    ) {
                        if (scanState is IdScanUiState.Scanning) {
                            CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(stringResource(R.string.id_verify_scanning))
                        } else {
                            Icon(
                                imageVector = Icons.Filled.PhotoLibrary,
                                contentDescription = null,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = if (bmp == null) stringResource(R.string.id_verify_pick_button) else "வேறு ID படத்தைத் தேர்ந்தெடுக்கவும்",
                                style = MaterialTheme.typography.labelLarge
                            )
                        }
                    }

                    AnimatedVisibility(visible = aiConfidenceNote != null) {
                        Surface(
                            color = MaterialTheme.colorScheme.surfaceContainer,
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = if (aiVerifiedCard) Icons.Filled.CheckCircle else Icons.Filled.Warning,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary
                                )
                                Text(
                                    text = aiConfidenceNote.orEmpty(),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }
                }
            }

            Text(
                text = "மாணவர் விவரங்களை உறுதிசெய்க",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onBackground
            )

            OutlinedTextField(
                value = fullName,
                onValueChange = { fullName = it.take(80) },
                label = { Text("மாணவர் முழுப் பெயர்") },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("student_name_input")
            )

            OutlinedTextField(
                value = schoolName,
                onValueChange = { schoolName = it.take(120) },
                label = { Text("பள்ளி / கல்லூரி பெயர்") },
                leadingIcon = { Icon(Icons.Filled.School, contentDescription = null) },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("student_school_input")
            )

            OutlinedTextField(
                value = studentIdNumber,
                onValueChange = { studentIdNumber = it.take(60) },
                label = { Text("மாணவர் ID / வரிசை எண்") },
                leadingIcon = { Icon(Icons.Filled.Badge, contentDescription = null) },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("student_id_number_input")
            )

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "கல்வி நிலை",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    GRADE_LEVELS_TAMIL.forEachIndexed { idx, level ->
                        FilterChip(
                            selected = gradeLevel == level,
                            onClick = { gradeLevel = level },
                            label = { Text(level) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primary,
                                selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                            ),
                            modifier = Modifier.testTag("grade_chip_$idx")
                        )
                    }
                }
            }

            val errorText = localDecodeError ?: (scanState as? IdScanUiState.Error)?.message
            AnimatedVisibility(visible = errorText != null) {
                Surface(
                    color = MaterialTheme.colorScheme.errorContainer,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = errorText.orEmpty(),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        modifier = Modifier.padding(12.dp)
                    )
                }
            }

            val canSubmit = fullName.trim().length >= 2 &&
                schoolName.trim().length >= 2 &&
                studentIdNumber.trim().length >= 2 &&
                scanState !is IdScanUiState.Scanning &&
                scanState !is IdScanUiState.Saving

            Button(
                onClick = {
                    onConfirmProfile(fullName, schoolName, studentIdNumber, gradeLevel)
                },
                enabled = canSubmit,
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 56.dp)
                    .testTag("confirm_student_id_button")
            ) {
                if (scanState is IdScanUiState.Saving) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        color = MaterialTheme.colorScheme.onPrimary,
                        strokeWidth = 2.dp
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text("மாணவர் அட்டை உருவாக்கப்படுகிறது…")
                } else {
                    Icon(Icons.Filled.CheckCircle, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (existingProfile == null) stringResource(R.string.id_verify_confirm_button) else "மாற்றங்களைச் சேமிக்கவும்",
                        style = MaterialTheme.typography.labelLarge
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
        }
    }
}
