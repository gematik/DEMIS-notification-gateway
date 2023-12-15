{{/*
Expand the name of the chart.
*/}}
{{- define "notification-gateway.name" -}}
{{- default .Chart.Name .Values.nameOverride | trunc 63 | trimSuffix "-" }}
{{- end }}

{{/*
Create a default fully qualified app name.
We truncate at 63 chars because some Kubernetes name fields are limited to this (by the DNS naming spec).
If release name contains chart name it will be used as a full name.
*/}}
{{- define "notification-gateway.fullname" -}}
{{- if .Values.fullnameOverride }}
{{- .Values.fullnameOverride | trunc 63 | trimSuffix "-" }}
{{- else }}
{{- $name := default .Chart.Name .Values.nameOverride }}
{{- if contains $name .Release.Name }}
{{- .Release.Name | trunc 63 | trimSuffix "-" }}
{{- else }}
{{- printf "%s-%s" .Release.Name $name | trunc 63 | trimSuffix "-" }}
{{- end }}
{{- end }}
{{- end }}

{{- define "notification-gateway.fullversionname" -}}
{{- if .Values.istio.enable }}
{{- $name := include "notification-gateway.fullname" . }}
{{- $version := regexReplaceAll "\\.+" .Chart.Version "-" }}
{{- printf "%s-%s" $name $version | trunc 63 }}
{{- else }}
{{- include "notification-gateway.fullname" . }}
{{- end }}
{{- end }}

{{/*
Create chart name and version as used by the chart label.
*/}}
{{- define "notification-gateway.chart" -}}
{{- printf "%s-%s" .Chart.Name .Chart.Version | replace "+" "_" | trunc 63 | trimSuffix "-" }}
{{- end }}

{{/*
Common labels
*/}}
{{- define "notification-gateway.labels" -}}
helm.sh/chart: {{ include "notification-gateway.chart" . }}
{{ include "notification-gateway.selectorLabels" . }}
{{- if .Chart.AppVersion }}
app.kubernetes.io/version: {{ .Chart.AppVersion | quote }}
{{- end }}
app.kubernetes.io/managed-by: {{ .Release.Service }}
{{- with .Values.customLabels }}
{{ toYaml . }}
{{- end }}
{{- end }}

{{/*
Selector labels
*/}}
{{- define "notification-gateway.selectorLabels" -}}
{{ if .Values.istio.enable -}}
app: {{ include "notification-gateway.name" . }}
version: {{ .Chart.AppVersion | quote }}
{{ end -}}
app.kubernetes.io/name: {{ include "notification-gateway.name" . }}
app.kubernetes.io/instance: {{ .Release.Name }}
{{- end }}

{{/*
Create the name of the service account to use
*/}}
{{- define "notification-gateway.serviceAccountName" -}}
{{- if .Values.serviceAccount.create }}
{{- default (include "notification-gateway.fullversionname" .) .Values.serviceAccount.name }}
{{- else }}
{{- default "default" .Values.serviceAccount.name }}
{{- end }}
{{- end }}

{{/*
Get ConfigMap name from values
*/}}
{{- define "notification-gateway.configMapName" -}}
{{- if not .Values.config.configMap.useDefault }}
{{- print (.Values.config.configMap.name | required ".Values.config.configMap.name is required.") }}
{{- else }}
{{- print "config-" (include "notification-gateway.fullversionname" .) }}
{{- end }}
{{- end }}

{{/*
Get Persistence Volume Claim name from values
*/}}
{{- define "notification-gateway.pvcName" -}}
{{- if not .Values.config.volumeClaim.useDefault }}
{{- print (.Values.config.volumeClaim.name | required ".Values.config.volumeClaim.name is required.") }}
{{- else }}
{{- print "pvc-" (include "notification-gateway.fullversionname" .) }}
{{- end }}
{{- end }}

{{/*
Get Persistence Volume  name from values
*/}}
{{- define "notification-gateway.pvName" -}}
{{- print "pv-" (include "notification-gateway.fullversionname" .) }}
{{- end }}

{{/*
Get Default Secret name 
*/}}
{{- define "notification-gateway.defaultSecretName" -}}
{{- print "secret-" (include "notification-gateway.fullversionname" .) }}
{{- end }}

{{/*
Get Password Secret name from Values
*/}}
{{- define "notification-gateway.passwordSecretName" -}}
{{- if and (not .Values.config.secret.passwords.name) (not .Values.config.secret.useDefault) }}
{{- fail ".Values.config.secret.passwords.name is required" }}
{{- end }}
{{- print .Values.config.secret.passwords.name }}
{{- end }}

{{/*
Get Keystores Secret name from Values
*/}}
{{- define "notification-gateway.keystoresSecretName" -}}
{{- if and (not .Values.config.secret.keystores.name) (not .Values.config.secret.useDefault) }}
{{- fail ".Values.config.secret.keystores.name is required" }}
{{- end }}
{{- print .Values.config.secret.keystores.name }}
{{- end }}

{{/*
Get Default MountPath for Secrets
*/}}
{{- define "notification-gateway.secretMountPath" -}}
{{- print "/secrets" }}
{{- end }}
