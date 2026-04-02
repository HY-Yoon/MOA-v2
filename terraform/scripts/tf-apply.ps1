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
Write-Host "RDS 접속 터널은 아래 명령으로 시작하세요:"
Write-Host "  powershell -ExecutionPolicy Bypass -File .\scripts\open-rds-tunnel.ps1"
