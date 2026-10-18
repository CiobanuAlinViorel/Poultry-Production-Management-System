{{/* Generate labels */}}
{{- define "ppms.labels" -}}
app.kubernetes.io/name: {{ .name }}
app.kubernetes.io/instance: {{ .Release.Name }}
app.kubernetes.io/managed-by: {{ .Release.Service }}
{{- end }}

{{/* Image name */}}
{{- define "ppms.image" -}}
{{- if .Values.global.imageRegistry -}}
{{ .Values.global.imageRegistry }}/{{ .Values.global.imagePrefix | default "ppms" }}/{{ .name }}:{{ .Values.global.imageTag }}
{{- else -}}
ppms/{{ .name }}:{{ .Values.global.imageTag }}
{{- end -}}
{{- end }}
