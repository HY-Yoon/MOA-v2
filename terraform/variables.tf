# ==============================================================================
# 외부 환경 설정 (PaaS 및 애플리케이션 Secrets)
# 이 파일에 선언된 변수들은 GitHub Actions의 Secrets 등을 통해 주입받습니다.
# ==============================================================================

# ------------------------------
# 1. PaaS 연결 정보 (Aiven Redis, Kafka)
# ------------------------------
variable "redis_host" {
  description = "Aiven Redis Host Endpoint"
  type        = string
  default     = ""
}

variable "redis_port" {
  description = "Aiven Redis Port"
  type        = string
  default     = "6379"
}

variable "redis_password" {
  description = "Aiven Redis Password"
  type        = string
  sensitive   = true
  default     = ""
}

variable "kafka_bootstrap_servers" {
  description = "Aiven Kafka Bootstrap Servers (e.g. host:port)"
  type        = string
  default     = ""
}

# Aiven Kafka SSL 연동을 위한 인증서 부분 (필요시)
variable "kafka_ssl_truststore_cert" {
  description = "Kafka SSL Truststore Cert"
  type        = string
  sensitive   = true
  default     = ""
}

variable "kafka_ssl_keystore_cert" {
  description = "Kafka SSL Keystore Cert"
  type        = string
  sensitive   = true
  default     = ""
}

variable "kafka_ssl_keystore_key" {
  description = "Kafka SSL Keystore Key"
  type        = string
  sensitive   = true
  default     = ""
}


# ------------------------------
# 2. 애플리케이션 Security / Secret 정보
# ------------------------------
variable "jwt_access_secret" {
  description = "JWT Access Token Secret"
  type        = string
  sensitive   = true
  default     = ""
}

variable "jwt_refresh_secret" {
  description = "JWT Refresh Token Secret"
  type        = string
  sensitive   = true
  default     = ""
}

variable "encryption_key" {
  description = "Application Database Encryption Key"
  type        = string
  sensitive   = true
  default     = ""
}

# ------------------------------
# 3. OAuth2 Client Secrets
# ------------------------------
variable "google_client_id" {
  type      = string
  sensitive = true
  default   = ""
}

variable "google_client_secret" {
  type      = string
  sensitive = true
  default   = ""
}

variable "kakao_client_id" {
  type      = string
  sensitive = true
  default   = ""
}

variable "kakao_client_secret" {
  type      = string
  sensitive = true
  default   = ""
}

variable "naver_client_id" {
  type      = string
  sensitive = true
  default   = ""
}

variable "naver_client_secret" {
  type      = string
  sensitive = true
  default   = ""
}

# ------------------------------
# 4. 결제 및 기타 (Toss, Mail 등)
# ------------------------------
variable "mail_username" {
  type      = string
  sensitive = true
  default   = ""
}

variable "mail_password" {
  type      = string
  sensitive = true
  default   = ""
}

variable "cors_allowed_origins" {
  description = "CORS 허용 도메인 설정"
  type        = string
  default     = "http://localhost:3000,http://localhost:5173"
}
