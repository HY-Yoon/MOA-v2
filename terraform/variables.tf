# ==============================================================================
# 외부 환경 설정 (PaaS 및 애플리케이션 Secrets)
# 이 파일에 선언된 변수들은 GitHub Actions의 Secrets 등을 통해 주입받습니다.
# ==============================================================================

# ------------------------------
# 0. 외부 PostgreSQL 연결 정보
# ------------------------------
variable "db_host" {
  description = "External PostgreSQL Host Endpoint"
  type        = string
  default     = ""
}

variable "db_port" {
  description = "External PostgreSQL Port"
  type        = string
  default     = "5432"
}

variable "db_sslmode" {
  description = "External PostgreSQL sslmode (e.g. require, disable)"
  type        = string
  default     = "require"
}

variable "db_name" {
  description = "External PostgreSQL database name"
  type        = string
  default     = "defaultdb"
}

variable "db_username" {
  description = "External PostgreSQL username"
  type        = string
  default     = "avnadmin"
}

variable "db_password" {
  description = "External PostgreSQL password"
  type        = string
  sensitive   = true
  default     = ""
}

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

# ------------------------------
# 5. NAT Nginx Reverse Proxy 설정
# ------------------------------
variable "nat_proxy_region" {
  description = "AWS region used by NAT instance AWS CLI lookup"
  type        = string
  default     = "ap-northeast-2"
}

variable "nat_proxy_app_asg_name" {
  description = "ASG name used by NAT to resolve backend private IP"
  type        = string
  default     = "moa-v2-app-asg"
}

variable "nat_proxy_app_name_tag" {
  description = "Fallback Name tag used by NAT to resolve backend private IP"
  type        = string
  default     = "moa-v2-app-instance"
}

variable "nat_proxy_public_domain" {
  description = "Public domain served by NAT nginx"
  type        = string
  default     = "moa.hee-factory.com"
}

variable "nat_proxy_backend_port" {
  description = "Backend app port proxied by NAT nginx"
  type        = number
  default     = 8080
}

variable "nat_proxy_private_zone_name" {
  description = "Private hosted zone name for backend service discovery"
  type        = string
  default     = "internal.moa"
}

variable "nat_proxy_backend_dns_name" {
  description = "Backend DNS name resolved by NAT nginx"
  type        = string
  default     = "app.internal.moa"
}

# ------------------------------
# 6. 비용 절감용 운영 시간 스케줄링
# ------------------------------
variable "enable_business_hours_schedule" {
  description = "Enable weekday business-hours auto start/stop scheduling"
  type        = bool
  default     = true
}

variable "business_hours_timezone" {
  description = "Timezone used by scheduler resources"
  type        = string
  default     = "Asia/Seoul"
}

variable "app_start_cron" {
  description = "Cron expression for starting app ASG on weekdays"
  type        = string
  default     = "0 9 * * 1-5"
}

variable "app_stop_cron" {
  description = "Cron expression for stopping app ASG on weekdays"
  type        = string
  default     = "0 21 * * 1-5"
}

variable "nat_start_cron" {
  description = "EventBridge cron expression for starting NAT instance on weekdays"
  type        = string
  default     = "cron(55 8 ? * MON-FRI *)"
}

variable "nat_stop_cron" {
  description = "EventBridge cron expression for stopping NAT instance on weekdays"
  type        = string
  default     = "cron(5 21 ? * MON-FRI *)"
}
