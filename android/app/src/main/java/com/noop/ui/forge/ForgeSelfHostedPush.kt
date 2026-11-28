package com.noop.ui.forge

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import com.noop.R
import com.noop.push.PushCapabilities
import com.noop.push.PushCapabilitiesResult
import com.noop.push.PushConnectionTester
import com.noop.push.PushEndpointPolicy
import com.noop.push.PushFailure
import com.noop.push.SelfHostedPushSettings
import com.noop.push.SelfHostedPushScheduler
import com.noop.push.isPushNetworkAvailable
import com.noop.push.pushEndpointProblemMessage
import com.noop.push.PushFailureCode
import com.noop.push.pushFailureMessage
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

private sealed interface CapabilityProbeUi {
    data object Idle : CapabilityProbeUi
    data object Testing : CapabilityProbeUi
    data class Success(val streams: List<String>, val checkedAt: Long) : CapabilityProbeUi
    data class Failure(val failure: PushFailure) : CapabilityProbeUi
}

@Composable
internal fun ForgeSelfHostedPushSection(onBack: () -> Unit) {
    val context = LocalContext.current
    val settings = remember { SelfHostedPushSettings.from(context) }
    var endpoint by remember { mutableStateOf(settings.endpointText()) }
    var token by remember { mutableStateOf("") }
    var snapshot by remember { mutableStateOf(settings.snapshot()) }
    var validationMessage by remember { mutableStateOf<String?>(null) }
    var capabilityProbe by remember { mutableStateOf<CapabilityProbeUi>(CapabilityProbeUi.Idle) }
    var capabilityProbeGeneration by remember { mutableStateOf(0) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(settings) {
        while (currentCoroutineContext().isActive) {
            snapshot = settings.snapshot()
            delay(750)
        }
    }

    val validatedEndpoint = PushEndpointPolicy.validate(endpoint) as? PushEndpointPolicy.Result.Valid
    val endpointValid = validatedEndpoint != null
    val tokenAvailable = token.isNotBlank() || snapshot.hasToken
    val active = snapshot.runState in setOf(
        SelfHostedPushSettings.RunState.QUEUED,
        SelfHostedPushSettings.RunState.RUNNING,
        SelfHostedPushSettings.RunState.CONTINUING,
        SelfHostedPushSettings.RunState.RETRYING,
    )

    SettingsColumn {
        PageTitle(stringResource(R.string.push_title), onBack)
        ForgeCard(Modifier.fillMaxWidth()) {
            Column(verticalArrangement = Arrangement.spacedBy(ForgeMetrics.space3)) {
                Text(stringResource(R.string.push_destination_title),
                     style = Forge.type.title2, color = Forge.colors.text)
                Text(stringResource(R.string.push_disclosure),
                     style = Forge.type.footnote, color = Forge.colors.textSecondary)
                Text(stringResource(R.string.push_one_way_warning),
                     style = Forge.type.footnote, color = androidx.compose.ui.graphics.Color(0xFFCC8A00))
                ForgePushField(
                    value = endpoint,
                    onValueChange = {
                        endpoint = it
                        validationMessage = null
                        capabilityProbe = CapabilityProbeUi.Idle
                        capabilityProbeGeneration++
                    },
                    label = stringResource(R.string.push_endpoint),
                    secret = false,
                )
                ForgePushField(
                    value = token,
                    onValueChange = {
                        token = it
                        capabilityProbe = CapabilityProbeUi.Idle
                        capabilityProbeGeneration++
                    },
                    label = if (snapshot.hasToken) stringResource(R.string.push_token_saved) else stringResource(R.string.push_token),
                    secret = true,
                )
                validationMessage?.let {
                    Text(it, style = Forge.type.footnote, color = androidx.compose.ui.graphics.Color(0xFFCC8A00))
                }
                ForgeButton(
                    text = stringResource(R.string.push_save),
                    enabled = endpointValid && tokenAvailable,
                    onClick = {
                        when (val result = settings.saveEndpoint(endpoint)) {
                            is PushEndpointPolicy.Result.Invalid ->
                                validationMessage = pushEndpointProblemMessage(context, result.problem)
                            is PushEndpointPolicy.Result.Valid -> {
                                val destinationChanged = snapshot.endpoint?.url != result.endpoint.url
                                endpoint = result.endpoint.url
                                if (token.isNotBlank()) settings.saveToken(token)
                                token = ""
                                snapshot = settings.snapshot()
                                if (destinationChanged && snapshot.ready) {
                                    SelfHostedPushScheduler.destinationChanged(context)
                                }
                                validationMessage = context.getString(R.string.push_saved)
                            }
                        }
                    },
                )
                ForgeButton(
                    text = if (capabilityProbe == CapabilityProbeUi.Testing) {
                        stringResource(R.string.push_testing_connection)
                    } else {
                        stringResource(R.string.push_test_connection)
                    },
                    enabled = endpointValid && tokenAvailable && capabilityProbe != CapabilityProbeUi.Testing,
                    onClick = {
                        val valid = PushEndpointPolicy.validate(endpoint) as? PushEndpointPolicy.Result.Valid
                        val testToken = token.trim().takeIf(String::isNotEmpty) ?: settings.token()
                        if (valid == null || testToken == null) {
                            validationMessage = context.getString(R.string.push_config_required)
                        } else if (!com.noop.push.canStartPushConnectionTest(
                                networkAvailable = isPushNetworkAvailable(context, wifiOnly = snapshot.wifiOnly),
                                endpointValid = true,
                                tokenAvailable = true,
                            )
                        ) {
                            validationMessage = context.getString(R.string.push_test_network_required)
                        } else {
                            val generation = capabilityProbeGeneration + 1
                            capabilityProbeGeneration = generation
                            val persistResult = token.isBlank() && valid.endpoint.url == snapshot.endpoint?.url
                            capabilityProbe = CapabilityProbeUi.Testing
                            scope.launch {
                                val result = PushConnectionTester().test(valid.endpoint, testToken)
                                if (capabilityProbeGeneration != generation) return@launch
                                capabilityProbe = when (result) {
                                    is PushCapabilitiesResult.Available -> {
                                        val checkedAt = System.currentTimeMillis()
                                        if (persistResult) runCatching {
                                            settings.recordCapabilities(valid.endpoint, result.capabilities, checkedAt)
                                        }
                                        snapshot = settings.snapshot()
                                        CapabilityProbeUi.Success(result.capabilities.wireNames, checkedAt)
                                    }
                                    is PushCapabilitiesResult.Rejected -> CapabilityProbeUi.Failure(
                                        result.failure ?: PushFailure(PushFailureCode.NETWORK_IO),
                                    )
                                }
                            }
                        }
                    },
                )
                Text(stringResource(R.string.push_test_connection_detail),
                     style = Forge.type.footnote, color = Forge.colors.textSecondary)
                val probeSuccess = capabilityProbe as? CapabilityProbeUi.Success
                val savedEndpointMatches = token.isBlank() &&
                    validatedEndpoint?.endpoint?.url == snapshot.endpoint?.url
                val shownStreams = probeSuccess?.streams ?: snapshot.supportedStreams.takeIf { savedEndpointMatches }
                val capabilitiesCheckedAt = probeSuccess?.checkedAt
                    ?: snapshot.capabilitiesCheckedAt.takeIf { savedEndpointMatches }
                when (val probe = capabilityProbe) {
                    is CapabilityProbeUi.Failure -> Text(
                        pushFailureMessage(context, probe.failure),
                        style = Forge.type.footnote, color = androidx.compose.ui.graphics.Color(0xFFCC8A00),
                    )
                    CapabilityProbeUi.Testing -> LinearProgressIndicator(
                        modifier = Modifier.fillMaxWidth(), color = Forge.colors.accent,
                    )
                    else -> Unit
                }
                shownStreams?.let { streams ->
                    Text(
                        stringResource(R.string.push_capabilities_summary, streams.size, PushCapabilities.ALL.wireNames.size),
                        style = Forge.type.body,
                        color = if (streams.isEmpty()) androidx.compose.ui.graphics.Color(0xFFCC8A00) else Forge.colors.text,
                    )
                    capabilitiesCheckedAt?.let { checkedAt ->
                        val checked = java.text.DateFormat.getDateTimeInstance(
                            java.text.DateFormat.MEDIUM, java.text.DateFormat.SHORT).format(java.util.Date(checkedAt))
                        Text(stringResource(R.string.push_capabilities_checked, checked),
                             style = Forge.type.footnote, color = Forge.colors.textSecondary)
                    }
                    Text(
                        if (streams.isEmpty()) stringResource(R.string.push_capabilities_none)
                        else stringResource(R.string.push_capabilities_streams, streams.joinToString(" · ")),
                        style = Forge.type.footnote,
                        color = if (streams.isEmpty()) androidx.compose.ui.graphics.Color(0xFFCC8A00) else Forge.colors.textSecondary,
                    )
                }
                if (snapshot.hasToken) {
                    ForgeButton(
                        text = stringResource(R.string.push_clear_token),
                        enabled = true,
                        onClick = {
                            settings.saveToken("")
                            settings.setEnabled(false)
                            SelfHostedPushScheduler.cancel(context)
                            snapshot = settings.snapshot()
                            validationMessage = context.getString(R.string.push_token_cleared)
                        },
                    )
                }
                ToggleRow(
                    title = stringResource(R.string.push_wifi_only),
                    detail = stringResource(R.string.push_wifi_only_detail),
                    checked = snapshot.wifiOnly,
                    onChange = { requested ->
                        capabilityProbe = CapabilityProbeUi.Idle
                        capabilityProbeGeneration++
                        settings.setWifiOnly(requested)
                        SelfHostedPushScheduler.networkPolicyChanged(context)
                        snapshot = settings.snapshot()
                    },
                )
                ToggleRow(
                    title = stringResource(R.string.push_enabled),
                    detail = stringResource(R.string.push_enabled_detail),
                    checked = snapshot.enabled,
                    onChange = { requested ->
                        if (!requested) {
                            settings.setEnabled(false)
                            SelfHostedPushScheduler.cancel(context)
                        } else if (!settings.setEnabled(true)) {
                            validationMessage = context.getString(R.string.push_config_required)
                        } else {
                            SelfHostedPushScheduler.enqueueLaunchCatchUp(context)
                        }
                        snapshot = settings.snapshot()
                    },
                )
                ForgeButton(
                    text = stringResource(R.string.push_export_now),
                    enabled = snapshot.ready,
                    onClick = {
                        SelfHostedPushScheduler.enqueueManualCatchUp(context)
                        snapshot = settings.snapshot()
                    },
                )
                Text(stringResource(R.string.push_export_now_detail),
                     style = Forge.type.footnote, color = Forge.colors.textSecondary)
            }
        }
        ForgeCard(Modifier.fillMaxWidth()) {
            Column(verticalArrangement = Arrangement.spacedBy(ForgeMetrics.space2)) {
                Text(stringResource(R.string.push_status_title),
                     style = Forge.type.title2, color = Forge.colors.text)
                Text(stringResource(R.string.push_status_detail),
                     style = Forge.type.footnote, color = Forge.colors.textSecondary)
                if (active) {
                    LinearProgressIndicator(modifier = Modifier.fillMaxWidth(), color = Forge.colors.accent)
                }
                val state = when (snapshot.runState) {
                    SelfHostedPushSettings.RunState.IDLE -> stringResource(R.string.push_state_idle)
                    SelfHostedPushSettings.RunState.QUEUED -> stringResource(R.string.push_state_queued)
                    SelfHostedPushSettings.RunState.RUNNING -> stringResource(R.string.push_state_running)
                    SelfHostedPushSettings.RunState.CONTINUING -> stringResource(R.string.push_state_continuing)
                    SelfHostedPushSettings.RunState.RETRYING -> stringResource(R.string.push_state_retrying)
                    SelfHostedPushSettings.RunState.COMPLETE -> stringResource(R.string.push_state_complete)
                    SelfHostedPushSettings.RunState.FAILED -> stringResource(R.string.push_state_failed)
                }
                Text(stringResource(R.string.push_current_state, state),
                     style = Forge.type.body, color = Forge.colors.text)
                Text(stringResource(R.string.push_progress, snapshot.acceptedBatches, snapshot.acceptedRecords),
                     style = Forge.type.footnote, color = Forge.colors.textSecondary)
                snapshot.currentStream?.let { stream ->
                    Text(stringResource(R.string.push_current_stream, stream),
                         style = Forge.type.footnote, color = Forge.colors.textSecondary)
                }
                val success = snapshot.lastSuccessAt?.let {
                    java.text.DateFormat.getDateTimeInstance(java.text.DateFormat.MEDIUM, java.text.DateFormat.SHORT)
                        .format(java.util.Date(it))
                } ?: stringResource(R.string.push_never)
                Text(stringResource(R.string.push_last_success, success),
                     style = Forge.type.body, color = Forge.colors.text)
                snapshot.lastError?.let {
                    Text(stringResource(R.string.push_last_error, it),
                         style = Forge.type.footnote, color = androidx.compose.ui.graphics.Color(0xFFCC8A00))
                }
            }
        }
    }
}

@Composable
private fun ForgePushField(value: String, onValueChange: (String) -> Unit, label: String, secret: Boolean) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        singleLine = true,
        visualTransformation = if (secret) PasswordVisualTransformation() else VisualTransformation.None,
        textStyle = Forge.type.body.copy(fontSize = 13.sp),
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = forgeFieldColors(),
    )
}

@Composable
private fun ForgeButton(text: String, enabled: Boolean, onClick: () -> Unit) {
    androidx.compose.material3.TextButton(
        onClick = onClick,
        enabled = enabled,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Text(text, style = Forge.type.body.copy(fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold),
             color = if (enabled) Forge.colors.accent else Forge.colors.textTertiary)
    }
}
