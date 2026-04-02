$ErrorActionPreference = "Stop"

Set-Location -Path (Join-Path $PSScriptRoot "..")

Write-Host "==> Terraform init"
terraform init

Write-Host "==> Terraform plan"
terraform plan

Write-Host "==> Terraform apply"
terraform apply -auto-approve

Write-Host ""
Write-Host "완료: 인프라가 적용되었습니다."
Write-Host "외부 PostgreSQL(Aiven 등) 연결 정보가 올바른지 애플리케이션 환경변수를 확인하세요."
